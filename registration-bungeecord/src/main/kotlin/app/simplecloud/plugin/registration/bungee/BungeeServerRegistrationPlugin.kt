package app.simplecloud.plugin.registration.bungee

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.event.bungeecord.mapping.CloudServerStopEvent
import app.simplecloud.event.bungeecord.mapping.CloudServerUpdateEvent
import app.simplecloud.plugin.registration.shared.ServerRegistrationPlugin
import build.buf.gen.simplecloud.controller.v1.ServerState
import build.buf.gen.simplecloud.controller.v1.ServerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler
import java.net.InetSocketAddress

class BungeeServerRegistrationPlugin : Plugin(), Listener {

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
        CoroutineScope(Dispatchers.Default).launch {
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
        ProxyServer.getInstance().pluginManager.registerListener(this, this)
    }

    private fun cleanupServers() {
        ProxyServer.getInstance().configurationAdapter.servers.clear()
        ProxyServer.getInstance().servers.clear()
        for (info in ProxyServer.getInstance().configurationAdapter.listeners) {
            info.serverPriority.clear()
        }
    }

    @EventHandler
    fun onServerStart(event: CloudServerUpdateEvent) {
        if (event.getTo().type != ServerType.SERVER) return
        if (event.getTo().state == ServerState.AVAILABLE && event.getFrom().state != ServerState.AVAILABLE) {
            serverRegistration.register(event.getTo())
            CoroutineScope(Dispatchers.Default).launch {
                api.getServers().updateServerProperty(event.getTo().uniqueId, "server-registered", "true")
            }
        }
    }

    @EventHandler
    fun onServerStop(event: CloudServerStopEvent) {
        serverRegistration.unregister(event.getServer())
    }

}