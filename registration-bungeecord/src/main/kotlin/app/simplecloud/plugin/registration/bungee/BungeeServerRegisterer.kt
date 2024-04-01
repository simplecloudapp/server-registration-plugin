package app.simplecloud.plugin.registration.bungee

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.registration.shared.ServerRegisterer
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.config.ServerInfo
import java.net.InetSocketAddress

class BungeeServerRegisterer: ServerRegisterer {

    private val registered = mutableListOf<Server>()

    private val registeredFallbacks = mutableListOf<ServerInfo>()

    override fun getRegistered(): List<Server> {
       return registered
    }

    fun getFallbacks(): List<ServerInfo> {
        return registeredFallbacks
    }

    init {
        ProxyServer.getInstance().reconnectHandler = BungeeReconnectHandler(this)
    }

    override fun register(server: Server) {
        val id = "${server.group}-${server.numericalId}"
        val info = ProxyServer.getInstance().constructServerInfo(id, InetSocketAddress.createUnresolved(server.ip, server.port.toInt()), server.uniqueId, server.properties.getOrDefault("proxy-restricted", "false").toBoolean())
        ProxyServer.getInstance().servers[id] = info
        if(server.properties.getOrDefault("fallback-server", "false").toBoolean())
            registeredFallbacks.add(info)
        registered.add(server)
        println("Registered $id!")
    }

    override fun unregister(server: Server) {
        val proxy = ProxyServer.getInstance()
        val id = "${server.group}-${server.numericalId}"
        val info = proxy.servers[id] ?: return
        proxy.servers.remove(id)
        registeredFallbacks.remove(info)
        registered.remove(server)
    }
}