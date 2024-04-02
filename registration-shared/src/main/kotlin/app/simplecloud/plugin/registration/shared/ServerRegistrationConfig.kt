package app.simplecloud.plugin.registration.shared

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ServerRegistrationConfig(
    val serverNamePattern: String,
    val ignoreServerGroups: List<String>,
    val additionalServers: List<RegistrationServer>
)

@ConfigSerializable
data class RegistrationServer(
    val name: String,
    val address: String,
    val port: Long
)