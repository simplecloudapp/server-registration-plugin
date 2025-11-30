package app.simplecloud.plugin.registration.shared

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ServerRegistrationConfig(
    val serverNamePattern: String = "%NAME%-%NUMERICAL_ID%",
    val persistentServerNamePattern: String = "%NAME%",
    val ignoreServerGroups: List<String> = listOf(),
    val additionalServers: List<RegistrationServer> = listOf()
)

@ConfigSerializable
data class RegistrationServer(
    val name: String = "",
    val address: String = "",
    val port: Long = 0L
)