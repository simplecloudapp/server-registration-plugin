package app.simplecloud.plugin.registration.bungee

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.registration.shared.ServerRegisterer
import app.simplecloud.plugin.registration.shared.ServerRegistrationPlugin
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.config.ServerInfo
import java.net.InetSocketAddress

class BungeeServerRegisterer(private val plugin: BungeeServerRegistrationPlugin): ServerRegisterer {

    private val registered = mutableListOf<Server>()

    override fun getRegistered(): List<Server> {
       return registered
    }

    override fun register(server: Server) {
        val id = plugin.getInstance().parseServerId(server)
        val info = ProxyServer.getInstance().constructServerInfo(id, InetSocketAddress.createUnresolved(server.ip, server.port.toInt()), server.uniqueId, server.properties.getOrDefault("proxy-restricted", "false").toBoolean())
        ProxyServer.getInstance().servers[id] = info
        registered.add(server)
    }

    override fun unregister(server: Server) {
        val proxy = ProxyServer.getInstance()
        proxy.servers.removeServer(server.uniqueId)
        registered.remove(server)
    }

    private fun MutableMap<String, ServerInfo>.removeServer(uniqueId: String): ServerInfo? {
        val toRemove = this.filter { it.value.motd == uniqueId }
        val value = toRemove.values.firstOrNull() ?: return null
        val key = toRemove.keys.firstOrNull() ?: return null
        remove(key, value)
        return value
    }
}