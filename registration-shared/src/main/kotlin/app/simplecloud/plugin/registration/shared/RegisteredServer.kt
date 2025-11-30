package app.simplecloud.plugin.registration.shared

data class RegisteredServer(
    val serverId: String,
    val numericalId: Int,
    val ip: String,
    val port: Int,
    val serverBaseName: String,
    val properties: Map<String, Any>,
    val blueprintConfigurator: String?
)

