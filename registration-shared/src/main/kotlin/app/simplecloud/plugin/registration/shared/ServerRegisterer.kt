package app.simplecloud.plugin.registration.shared

import app.simplecloud.controller.shared.server.Server


interface ServerRegisterer {
    fun getRegistered(): List<Server>
    fun register(server: Server)
    fun unregister(server: Server)
}