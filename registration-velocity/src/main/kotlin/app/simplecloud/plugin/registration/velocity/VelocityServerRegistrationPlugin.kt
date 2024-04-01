package app.simplecloud.plugin.registration.velocity

import app.simplecloud.plugin.registration.shared.ServerRegistrationPlugin
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import java.util.logging.Logger


@Plugin(
    id = "registration-velocity",
    name = "registration-velocity",
    version = "1.0-SNAPSHOT",
    authors = ["daviidooo"],
    description = "Server Registration plugin for SimpleCloud v3",
    url = "https://github.com/theSimpleCloud/server-registration-plugin"
) class VelocityServerRegistrationPlugin @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger
) {

    private lateinit var plugin: ServerRegistrationPlugin
    @Subscribe
    fun handleInitialize(ignored: ProxyInitializeEvent) {
        plugin = ServerRegistrationPlugin(VelocityServerRegisterer(server))
        plugin.start()
    }
}