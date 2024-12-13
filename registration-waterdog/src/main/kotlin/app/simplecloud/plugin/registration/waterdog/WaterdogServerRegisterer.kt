package app.simplecloud.plugin.registration.waterdog

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.registration.shared.ServerRegisterer
import dev.waterdog.waterdogpe.ProxyServer
import dev.waterdog.waterdogpe.network.serverinfo.BedrockServerInfo
import java.net.InetSocketAddress

class VelocityServerRegisterer(
    private val plugin: WaterdogServerRegistrationPlugin,
    private val proxy: ProxyServer
): ServerRegisterer {

    private val servers = mutableListOf<Server>()
    override fun getRegistered(): List<Server> {
        return servers
    }

    override fun register(server: Server) {
        val info = BedrockServerInfo(plugin.serverRegistration.parseServerId(server), InetSocketAddress.createUnresolved(server.ip, server.port.toInt()), InetSocketAddress.createUnresolved(server.ip, server.port.toInt()))
        proxy.registerServerInfo(info)
        servers.add(server)
    }

    override fun unregister(server: Server) {
        proxy.removeServerInfo(plugin.serverRegistration.parseServerId(server))
        servers.remove(server)
    }
}