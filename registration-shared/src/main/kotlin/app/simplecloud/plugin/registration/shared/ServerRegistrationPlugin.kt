package app.simplecloud.plugin.registration.shared

import app.simplecloud.api.CloudApi
import app.simplecloud.api.group.GroupServerType
import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerQuery
import app.simplecloud.api.server.ServerState
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.kotlin.toNode
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Logger

class ServerRegistrationPlugin(
    private val logger: Logger,
    private val dataDirectory: Path,
    private val registerer: ServerRegisterer,
) {

    private var config: ServerRegistrationConfig = ServerRegistrationConfig(
        ignoreServerGroups = listOf(),
        serverNamePattern = "%GROUP%-%NUMERICAL_ID%",
        additionalServers = listOf()
    )

    suspend fun start(api: CloudApi) {
        logger.info("Initializing v3 server registration plugin...")

        registerPubSubListener(api)

        loadConfig(File(dataDirectory.toFile(), "config.yml"))
//        val serversByType = api.server().getServersByType(ServerType.SERVER)
//        logger.info("Found ${serversByType.size} servers")
//        serversByType.filter { it.state == ServerState.AVAILABLE }.forEach(::register)

        api.server().getAllServers(
            ServerQuery.create()
                .filterByState(ServerState.AVAILABLE)
                .filterByServerGroupType(GroupServerType.SERVER)
        ).thenAccept { servers ->
            logger.info("Found ${servers.size} servers")
            servers.forEach {
                register(convertToRegisteredServer(it))
            }
        }
    }

    private fun registerPubSubListener(api: CloudApi) {
//        api.getPubSubClient().subscribe("event", ServerUpdateEvent::class.java) { event ->
//            if (event.serverAfter.serverType != ServerType.SERVER) return@subscribe
//            if (event.serverAfter.serverState == ServerState.AVAILABLE && event.serverBefore.serverState != ServerState.AVAILABLE) {
//                register(Server.fromDefinition(event.serverAfter))
//                CoroutineScope(Dispatchers.IO).launch {
//                    api.getServers().updateServerProperty(event.serverAfter.uniqueId, "server-registered", "true")
//                }}
//        }
//
//        api.getPubSubClient().subscribe("event", ServerStopEvent::class.java) { event ->
//            unregister(Server.fromDefinition(event.server))
//        }

        api.event().server().onStateChanged { event ->
            val server = event.server ?: return@onStateChanged
            if (server.serverGroup?.type != GroupServerType.SERVER) return@onStateChanged
            if (event.newState == ServerState.AVAILABLE && event.oldState != ServerState.AVAILABLE) {
                register(convertToRegisteredServer(server))
            }
        }

        api.event().server().onStopped { event ->
            val server = event.server ?: return@onStopped
            unregister(convertToRegisteredServer(server))
        }

        // TODO: Persistent Servers
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

    fun parseServerId(server: RegisteredServer): String {
        var toReturn = config.serverNamePattern

        val placeholders = mutableMapOf(
            "%GROUP%" to server.serverGroupName,
            "%NUMERICAL_ID%" to server.numericalId.toString(),
            "%ID%" to server.serverId,
        )

        placeholders.putAll(server.properties.map {
            "%${it.key.uppercase().replace("-", "_")}%" to it.value.toString()
        })

        placeholders.forEach {
            toReturn = toReturn.replace(it.key, it.value)
        }

        return toReturn
    }

    private fun register(server: RegisteredServer) {
        if (server.blueprintConfigurator == "standalone") {
            return
        }

        logger.info("Registering server ${server.serverId} (${parseServerId(server)})...")
        registerer.register(server)
    }

    private fun unregister(server: RegisteredServer) {
        if (registerer.getRegistered().contains(server.serverId)) {
            logger.info("Unregistering server ${server.serverId} (${parseServerId(server)})...")
            registerer.unregister(server)
        }
    }

    private fun convertToRegisteredServer(server: Server): RegisteredServer {
        return RegisteredServer(
            serverId = server.serverId,
            numericalId = server.numericalId,
            ip = server.ip!!,
            port = server.port!!,
            serverGroupName = server.serverGroup!!.name!!,
            properties = server.properties ?: emptyMap(),
            blueprintConfigurator = server.blueprint?.configurator
        )
    }

}

