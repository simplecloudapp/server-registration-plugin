package app.simplecloud.plugin.registration.shared

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.shared.server.Server
import build.buf.gen.simplecloud.controller.v1.ServerState
import build.buf.gen.simplecloud.controller.v1.ServerType
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.kotlin.toNode
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.LocalDateTime
import java.util.logging.Logger

class ServerRegistrationPlugin(
    private val logger: Logger,
    private val dataDirectory: Path,
    private val registerer: ServerRegisterer
) {

    private var config: ServerRegistrationConfig = ServerRegistrationConfig(
        ignoreServerGroups = listOf(),
        serverNamePattern = "%GROUP%-%NUMERICAL_ID%",
        additionalServers = listOf()
    )

    suspend fun start(api: ControllerApi.Coroutine) {
        logger.info("Initializing v3 server registration plugin...")
        loadConfig(File(dataDirectory.toFile(), "config.yml"))
        api.getServers().getServersByType(ServerType.SERVER).filter {
            it.state == ServerState.AVAILABLE && Duration.between(
                it.updatedAt,
                LocalDateTime.now()
            ).seconds < 10
        }.forEach(::register)
    }

    private fun loadConfig(file: File) {
        val loader = YamlConfigurationLoader.builder()
            .file(file)
            .nodeStyle(NodeStyle.FLOW)
            .defaultOptions { options ->
                options.serializers {
                    it.registerAnnotatedObjects(objectMapperFactory()).build()
                }
            }
            .build()

        var replace = false
        if (!file.exists()) {
            replace = true
            Files.createDirectories(file.parentFile.toPath())
            Files.createFile(file.toPath())
        }

        val node = loader.load()
        if (replace) {
            config.toNode(node)
            loader.save(node)
        }

        config = node.get<ServerRegistrationConfig>() ?: return
    }

    fun getConfig(): ServerRegistrationConfig {
        return config
    }

    fun parseServerId(server: Server): String {
        var toReturn = config.serverNamePattern
        val placeholders = mutableMapOf(
            "%GROUP%" to server.group,
            "%NUMERICAL_ID%" to server.numericalId.toString(),
            "%ID%" to server.uniqueId,
        )

        placeholders.putAll(server.properties.map {
            "%${it.key.uppercase().replace("-", "_")}%" to it.value
        })

        placeholders.forEach {
            toReturn = toReturn.replace(it.key, it.value)
        }

        return toReturn
    }

    fun register(server: Server) {
        if (server.properties["configurator"]?.contains("standalone") == true) {
            logger.info("Registering server ${server.uniqueId} (${parseServerId(server)})...")
            registerer.register(server)
        }
    }

    fun unregister(server: Server) {
        if (registerer.getRegistered().any() { it.uniqueId == server.uniqueId }) {
            logger.info("Unregistering server ${server.uniqueId} (${parseServerId(server)})...")
            registerer.unregister(server)
        }
    }
}