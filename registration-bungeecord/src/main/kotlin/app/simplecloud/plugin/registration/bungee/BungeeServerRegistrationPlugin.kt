package app.simplecloud.plugin.registration.bungee

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.plugin.registration.shared.ServerRegistrationPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.plugin.Plugin
import java.net.InetSocketAddress

class BungeeServerRegistrationPlugin : Plugin() {

    val serverRegistration by lazy {
        ServerRegistrationPlugin(
            ProxyServer.getInstance().logger,
            dataFolder.toPath(),
            BungeeServerRegisterer(this)
        )
    }

    private val api = ControllerApi.createCoroutineApi()

    override fun onEnable() {
        cleanupServers()
        CoroutineScope(Dispatchers.IO).launch {
            serverRegistration.start(api)
        }

        serverRegistration.getConfig().additionalServers.forEach {
            val serverInfo = ProxyServer.getInstance().constructServerInfo(
                it.name,
                InetSocketAddress.createUnresolved(it.address, it.port.toInt()),
                it.name,
                false
            )

            ProxyServer.getInstance().servers[it.name] = serverInfo
        }
    }

    private fun cleanupServers() {
        ProxyServer.getInstance().configurationAdapter.servers.clear()
        ProxyServer.getInstance().servers.clear()

        for (info in ProxyServer.getInstance().configurationAdapter.listeners) {
            info.serverPriority.clear()
        }
    }

}