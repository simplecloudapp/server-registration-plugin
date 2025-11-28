package app.simplecloud.plugin.registration.shared

interface ServerRegisterer {
    fun getRegistered(): Map<String, RegisteredServer>
    fun register(server: RegisteredServer)
    fun unregister(server: RegisteredServer)
}