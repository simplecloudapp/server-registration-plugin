package app.simplecloud.plugin.registration.bungee

import app.simplecloud.plugin.registration.shared.RegisteredServer
import app.simplecloud.plugin.registration.shared.ServerRegisterer
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.config.ServerInfo
import java.net.InetSocketAddress

class BungeeServerRegisterer(private val plugin: BungeeServerRegistrationPlugin) : ServerRegisterer {

    private val servers = mutableMapOf<String, RegisteredServer>()

    override fun getRegistered(): Map<String, RegisteredServer> {
        return servers
    }

    override fun register(server: RegisteredServer) {
        val id = plugin.serverRegistration.parseServerId(server)

        val info = ProxyServer.getInstance().constructServerInfo(
            id,
            InetSocketAddress.createUnresolved(server.ip, server.port),
            server.serverId,
            server.properties.getOrDefault("proxy-restricted", "false").toString().toBoolean()
        )

        ProxyServer.getInstance().servers[id] = info
        servers[server.serverId] = server
    }

    override fun unregister(server: RegisteredServer) {
        ProxyServer.getInstance().servers.removeServer(server.serverId)
        servers.remove(server.serverId)
    }

    private fun MutableMap<String, ServerInfo>.removeServer(uniqueId: String): ServerInfo? {
        val toRemove = this.filter { it.value.motd == uniqueId }
        val value = toRemove.values.firstOrNull() ?: return null
        val key = toRemove.keys.firstOrNull() ?: return null

        remove(key, value)
        return value
    }

}