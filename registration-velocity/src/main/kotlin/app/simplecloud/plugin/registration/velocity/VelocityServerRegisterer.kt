package app.simplecloud.plugin.registration.velocity

import app.simplecloud.plugin.registration.shared.RegisteredServer
import app.simplecloud.plugin.registration.shared.ServerRegisterer
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import java.net.InetSocketAddress
import kotlin.jvm.optionals.getOrNull

class VelocityServerRegisterer(
    private val plugin: VelocityServerRegistrationPlugin,
    private val proxy: ProxyServer
): ServerRegisterer {

    private val servers = mutableMapOf<String, RegisteredServer>()

    override fun getRegistered(): Map<String, RegisteredServer> {
        return servers
    }

    override fun register(server: RegisteredServer) {
        val info = ServerInfo(plugin.serverRegistration.parseServerId(server), InetSocketAddress.createUnresolved(server.ip, server.port))
        proxy.registerServer(info)
        servers[server.serverId] = server
    }

    override fun unregister(server: RegisteredServer) {
        val registeredSerer = proxy.getServer(plugin.serverRegistration.parseServerId(server)).getOrNull() ?: return
        proxy.unregisterServer(registeredSerer.serverInfo)
        servers.remove(server.serverId)
    }

}