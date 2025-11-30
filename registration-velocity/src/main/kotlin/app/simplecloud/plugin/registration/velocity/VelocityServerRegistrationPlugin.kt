package app.simplecloud.plugin.registration.velocity

import BuildConstants
import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.registration.shared.ServerRegistrationPlugin
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.nio.file.Path
import java.util.logging.Logger

@Plugin(
    id = BuildConstants.MODULE_NAME,
    name = BuildConstants.MODULE_NAME,
    version = BuildConstants.VERSION,
    authors = ["daviidooo", "Fllip"],
    description = "Server Registration plugin for SimpleCloud v3",
    url = "https://github.com/theSimpleCloud/server-registration-plugin"
)
class VelocityServerRegistrationPlugin @Inject constructor(
    @DataDirectory val dataDirectory: Path,
    private val server: ProxyServer,
    private val logger: Logger
) {

    val serverRegistration = ServerRegistrationPlugin(
        logger,
        dataDirectory,
        VelocityServerRegisterer(this, server)
    )

    private val api = CloudApi.create()

    @Subscribe
    fun handleInitialize(ignored: ProxyInitializeEvent) {
        cleanupServers()
        CoroutineScope(Dispatchers.IO).launch {
            serverRegistration.start(api)
        }

        serverRegistration.getConfig().additionalServers.forEach {
            val serverInfo = ServerInfo(it.name, InetSocketAddress.createUnresolved(it.address, it.port.toInt()))
            server.registerServer(serverInfo)
            logger.info("Additional server ${serverInfo.name} has been registered!")
        }
    }

    private fun cleanupServers() {
        server.allServers.forEach {
            server.unregisterServer(it.serverInfo)
        }
    }
}