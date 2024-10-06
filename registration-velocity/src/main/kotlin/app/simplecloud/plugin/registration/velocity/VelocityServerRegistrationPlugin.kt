package app.simplecloud.plugin.registration.velocity

import BuildConstants
import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.event.velocity.mapping.CloudServerStopEvent
import app.simplecloud.event.velocity.mapping.CloudServerUpdateEvent
import app.simplecloud.plugin.registration.shared.ServerRegistrationPlugin
import build.buf.gen.simplecloud.controller.v1.ServerState
import build.buf.gen.simplecloud.controller.v1.ServerType
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import java.net.InetSocketAddress
import java.nio.file.Path
import java.util.logging.Logger


@Plugin(
    id = BuildConstants.MODULE_NAME,
    name = BuildConstants.MODULE_NAME,
    version = BuildConstants.VERSION,
    authors = ["daviidooo"],
    description = "Server Registration plugin for SimpleCloud v3",
    url = "https://github.com/theSimpleCloud/server-registration-plugin"
) class VelocityServerRegistrationPlugin @Inject constructor(
    @DataDirectory val dataDirectory: Path,
    private val server: ProxyServer,
    private val logger: Logger
) {

    val serverRegistration = ServerRegistrationPlugin(
        logger,
        dataDirectory,
        VelocityServerRegisterer(this, server)
    )

    private val api = ControllerApi.create()

    @Subscribe
    fun handleInitialize(ignored: ProxyInitializeEvent) {
        cleanupServers()
        serverRegistration.start(api)
        serverRegistration.getConfig().additionalServers.forEach {
            val serverInfo = ServerInfo(it.name, InetSocketAddress.createUnresolved(it.address, it.port.toInt()))
            server.registerServer(serverInfo)
        }
    }

    @Subscribe
    fun onServerStart(event: CloudServerUpdateEvent) {
        if(event.getTo().type != ServerType.SERVER) return
        if(event.getTo().state == ServerState.AVAILABLE && event.getFrom().state != ServerState.AVAILABLE) {
            serverRegistration.register(event.getTo())
            api.getServers().updateServerProperty(event.getTo().uniqueId, "server-registered", "true")
        }
    }

    @Subscribe
    fun onServerStop(event: CloudServerStopEvent) {
        serverRegistration.unregister(event.getServer())
    }

    private fun cleanupServers() {
        server.allServers.forEach {
            server.unregisterServer(it.serverInfo)
        }
    }

}