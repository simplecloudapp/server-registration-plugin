package app.simplecloud.plugin.registration.shared

import app.simplecloud.controller.api.Controller
import app.simplecloud.controller.shared.proto.ServerType
import app.simplecloud.controller.shared.server.Server
import kotlinx.coroutines.*
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger

class ServerRegistrationPlugin(
    private val registerer: ServerRegisterer
) {
    private lateinit var logger: Logger
    fun start(logger: Logger) {
        this.logger = logger
        logger.info("Initializing v3 server registration plugin...")
        Controller.connect()
        startRegistrationLoop()
    }

    private fun getAllChildren(): CompletableFuture<List<Server>> {
        return Controller.serverApi.getServersByType(ServerType.SERVER)
    }

    @OptIn(InternalCoroutinesApi::class)
    private fun startRegistrationLoop(): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            while(NonCancellable.isActive) {
                getAllChildren().thenApply { servers ->
                    //register all servers that are not registered yet
                    servers.filter { server -> !registerer.getRegistered().contains(server.uniqueId) }.forEach {
                        logger.info("Registering server ${it.uniqueId}...")
                        registerer.register(it)
                    }
                    //unregister all servers that are not online anymore
                    registerer.getRegistered().filter { server -> !servers.contains(server.uniqueId) }.forEach {
                        logger.info("Unregistering server ${it.uniqueId}...")
                        registerer.unregister(it)
                    }
                }
                delay(5000L)
            }
        }
    }

    private fun List<Server>.contains(uniqueId: String): Boolean {
        return any { it.uniqueId == uniqueId }
    }



}