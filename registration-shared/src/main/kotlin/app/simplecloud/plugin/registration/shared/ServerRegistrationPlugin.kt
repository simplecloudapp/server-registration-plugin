package app.simplecloud.plugin.registration.shared

import app.simplecloud.controller.api.Controller
import app.simplecloud.controller.shared.server.Server
import build.buf.gen.simplecloud.controller.v1.ServerState
import build.buf.gen.simplecloud.controller.v1.ServerType
import kotlinx.coroutines.*
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.kotlin.toNode
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger

class ServerRegistrationPlugin(
    private val logger: Logger,
    private val dataDirectory: Path,
    private val registerer: ServerRegisterer
) {

    private var config: ServerRegistrationConfig = ServerRegistrationConfig(
        ignoreServerGroups = listOf(),
        serverNamePattern = "%GROUP%-%NUMERICAL_ID%",
        additionalServers = listOf()
    )

    fun start() {
        logger.info("Initializing v3 server registration plugin...")
        Controller.connect()
        startRegistrationLoop()
        loadConfig(File(dataDirectory.toFile(), "config.yml"))
    }

    private fun getAllChildren(): CompletableFuture<List<Server>> {
        return Controller.serverApi.getServersByType(ServerType.SERVER).thenApply {
            it.filter { server ->
                !config.ignoreServerGroups.contains(server.group)
                        && (server.state == ServerState.AVAILABLE || server.state == ServerState.INGAME)
            }
        }
    }

    private fun loadConfig(file: File) {
        val loader = YamlConfigurationLoader.builder().file(file).defaultOptions {
            options ->
                options.serializers {
                    it.registerAnnotatedObjects(objectMapperFactory()).build()
                }
        }.build()
        var replace = false
        if(!file.exists()) {
            replace = true
            Files.createDirectories(file.parentFile.toPath())
            Files.createFile(file.toPath())
        }
        val node = loader.load()
        if(replace) {
            config.toNode(node)
            loader.save(node)
        }
        config = node.get<ServerRegistrationConfig>() ?: return
    }

    fun getConfig(): ServerRegistrationConfig {
        return config
    }

    fun parseServerId(server: Server): String {
        var toReturn = config.serverNamePattern
        val placeholders = mutableMapOf(
            "%GROUP%" to server.group,
            "%NUMERICAL_ID%" to server.numericalId.toString(),
            "%ID%" to server.uniqueId,
        )
        placeholders.putAll(server.properties.map {
            "%${it.key.uppercase().replace("-", "_")}%" to it.value
        })
        placeholders.forEach {
            toReturn = toReturn.replace(it.key, it.value)
        }
        return toReturn
    }

    private fun startRegistrationLoop(): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            while(isActive) {
                getAllChildren().thenApply { servers ->
                    //register all servers that are not registered yet
                    servers.filter { server -> !registerer.getRegistered().contains(server.uniqueId) }.forEach {
                        logger.info("Registering server ${it.uniqueId} (${parseServerId(it)})...")
                        registerer.register(it)
                    }
                    //unregister all servers that are not online anymore
                    registerer.getRegistered().filter { server -> !servers.contains(server.uniqueId) }.forEach {
                        logger.info("Unregistering server ${it.uniqueId} (${parseServerId(it)})...")
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