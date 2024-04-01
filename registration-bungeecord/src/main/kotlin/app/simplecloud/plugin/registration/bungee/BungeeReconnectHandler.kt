package app.simplecloud.plugin.registration.bungee

import net.md_5.bungee.api.ReconnectHandler
import net.md_5.bungee.api.config.ServerInfo
import net.md_5.bungee.api.connection.ProxiedPlayer

//TEMPORARY SOLUTION, WILL BE REPLACED BY DEDICATED FALLBACK PLUGIN
class BungeeReconnectHandler(private val registerer: BungeeServerRegisterer): ReconnectHandler {
    override fun getServer(player: ProxiedPlayer?): ServerInfo? {
        return registerer.getFallbacks().firstOrNull()
    }

    override fun setServer(player: ProxiedPlayer) {
    }

    override fun save() {

    }

    override fun close() {

    }
}