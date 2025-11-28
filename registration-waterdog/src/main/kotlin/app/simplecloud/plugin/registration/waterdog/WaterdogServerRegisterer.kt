package app.simplecloud.plugin.registration.waterdog

import app.simplecloud.plugin.registration.shared.RegisteredServer
import app.simplecloud.plugin.registration.shared.ServerRegisterer
import dev.waterdog.waterdogpe.ProxyServer
import dev.waterdog.waterdogpe.network.serverinfo.BedrockServerInfo
import java.net.InetSocketAddress

class WaterdogServerRegisterer(
    private val plugin: WaterdogServerRegistrationPlugin,
    private val proxy: ProxyServer,
) : ServerRegisterer {

    private val servers = mutableMapOf<String, RegisteredServer>()

    override fun getRegistered(): Map<String, RegisteredServer> {
        return servers
    }

    override fun register(server: RegisteredServer) {
        val info = BedrockServerInfo(
            plugin.serverRegistration.parseServerId(server),
            InetSocketAddress.createUnresolved(server.ip, server.port),
            InetSocketAddress.createUnresolved(server.ip, server.port)
        )
        proxy.registerServerInfo(info)
        servers[server.serverId] = server
    }

    override fun unregister(server: RegisteredServer) {
        proxy.removeServerInfo(plugin.serverRegistration.parseServerId(server))
        servers.remove(server.serverId)
    }

}