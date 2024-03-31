package app.simplecloud.plugin.registration.bungee

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.registration.shared.ServerRegisterer
import net.md_5.bungee.api.ProxyServer
import java.net.InetSocketAddress

class BungeeServerRegisterer: ServerRegisterer {

    private val registered = mutableListOf<Server>()

    override fun getRegistered(): List<Server> {
       return registered
    }

    override fun register(server: Server) {
        val proxy = ProxyServer.getInstance()
        val id = "${server.group}-${server.numericalId}"
        val info = proxy.constructServerInfo(id, InetSocketAddress.createUnresolved(server.ip, server.port.toInt()), server.properties.getOrDefault("motd", "motd"), server.properties.getOrDefault("proxy-restricted", "false").toBoolean())
        proxy.servers[id] = info
        registered.add(server)
    }

    override fun unregister(server: Server) {
        val proxy = ProxyServer.getInstance()
        val id = "${server.group}-${server.numericalId}"
        proxy.servers.remove(id)
        registered.remove(server)
    }
}