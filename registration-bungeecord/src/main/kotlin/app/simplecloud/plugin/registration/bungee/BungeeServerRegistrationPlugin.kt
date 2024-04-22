package app.simplecloud.plugin.registration.bungee

import app.simplecloud.plugin.registration.shared.ServerRegistrationPlugin
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

    override fun onEnable() {
        cleanupServers()
        serverRegistration.start()
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