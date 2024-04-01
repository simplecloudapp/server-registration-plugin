package app.simplecloud.plugin.registration.velocity

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.registration.shared.ServerRegisterer
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import java.net.InetSocketAddress
import kotlin.jvm.optionals.getOrNull

class VelocityServerRegisterer(
    private val proxy: ProxyServer
): ServerRegisterer {

    private val servers = mutableListOf<Server>()
    override fun getRegistered(): List<Server> {
        return servers
    }

    private fun getServerName(server: Server): String {
        return "${server.group}-${server.numericalId}"
    }

    override fun register(server: Server) {
        val info = ServerInfo(getServerName(server), InetSocketAddress.createUnresolved(server.ip, server.port.toInt()))
        proxy.registerServer(info)
        servers.add(server)
    }

    override fun unregister(server: Server) {
        val registeredSerer = proxy.getServer(getServerName(server)).getOrNull() ?: return
        proxy.unregisterServer(registeredSerer.serverInfo)
        servers.remove(server)
    }
}