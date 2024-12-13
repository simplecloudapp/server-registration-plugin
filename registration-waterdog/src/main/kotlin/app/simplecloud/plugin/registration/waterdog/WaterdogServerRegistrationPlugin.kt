package app.simplecloud.plugin.registration.waterdog

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.plugin.registration.shared.ServerRegistrationPlugin
import dev.waterdog.waterdogpe.network.serverinfo.BedrockServerInfo
import dev.waterdog.waterdogpe.plugin.Plugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.util.logging.Logger

class WaterdogServerRegistrationPlugin: Plugin() {

    private val logger = Logger.getLogger(getLogger().name)

    val serverRegistration = ServerRegistrationPlugin(
        logger,
        dataFolder.toPath(),
        VelocityServerRegisterer(this, proxy)
    )

    private val api = ControllerApi.createCoroutineApi()

    override fun onEnable() {
        cleanupServers()
        CoroutineScope(Dispatchers.Default).launch {
            serverRegistration.start(api)
        }
        serverRegistration.getConfig().additionalServers.forEach {
            proxy.registerServerInfo(BedrockServerInfo(it.name,
                InetSocketAddress.createUnresolved(it.address, it.port.toInt()),
                InetSocketAddress.createUnresolved(it.address, it.port.toInt())
                )
            )
        }
    }


    private fun cleanupServers() {
        proxy.servers.forEach {
            proxy.removeServerInfo(it.serverName)
        }
    }

}