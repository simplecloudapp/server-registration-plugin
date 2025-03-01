package app.simplecloud.plugin.registration.shared

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.pubsub.PubSubClient
import build.buf.gen.simplecloud.controller.v1.ServerState
import build.buf.gen.simplecloud.controller.v1.ServerStopEvent
import build.buf.gen.simplecloud.controller.v1.ServerType
import build.buf.gen.simplecloud.controller.v1.ServerUpdateEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

        registerPubSubListener(api)

        loadConfig(File(dataDirectory.toFile(), "config.yml"))
        val serversByType = api.getServers().getServersByType(ServerType.SERVER)
        logger.info("Found ${serversByType.size} servers")
        serversByType.filter {
            it.state == ServerState.AVAILABLE && Duration.between(
                it.updatedAt,
                LocalDateTime.now()
            ).seconds < 10
        }.forEach(::register)
    }

    private fun registerPubSubListener(api: ControllerApi.Coroutine) {
        api.getPubSubClient().subscribe("event", ServerUpdateEvent::class.java) { event ->
            if (event.serverAfter.serverType != ServerType.SERVER) return@subscribe
            if (event.serverAfter.serverState == ServerState.AVAILABLE && event.serverBefore.serverState != ServerState.AVAILABLE) {
                register(Server.fromDefinition(event.serverAfter))
                CoroutineScope(Dispatchers.IO).launch {
                    api.getServers().updateServerProperty(event.serverAfter.uniqueId, "server-registered", "true")
                }}
        }

        api.getPubSubClient().subscribe("event", ServerStopEvent::class.java) { event ->
            unregister(Server.fromDefinition(event.server))
        }
    }

    private fun loadConfig(file: File) {
        val loader = YamlConfigurationLoader.builder()
            .file(file)
            .nodeStyle(NodeStyle.BLOCK)
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

    private fun register(server: Server) {
        if (server.properties["configurator"]?.contains("standalone") == false) {
            logger.info("Registering server ${server.uniqueId} (${parseServerId(server)})...")
            registerer.register(server)
        }
    }

    private fun unregister(server: Server) {
        if (registerer.getRegistered().any() { it.uniqueId == server.uniqueId }) {
            logger.info("Unregistering server ${server.uniqueId} (${parseServerId(server)})...")
            registerer.unregister(server)
        }
    }

}