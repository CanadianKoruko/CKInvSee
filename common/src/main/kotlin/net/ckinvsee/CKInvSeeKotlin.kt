package net.ckinvsee

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import dev.architectury.event.events.common.CommandRegistrationEvent
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.event.events.common.PlayerEvent
import net.ckinvsee.permissions.CKPermissions
import net.ckinvsee.permissions.IPermissionProvider
import net.ckinvsee.permissions.LPPermissionProvider
import net.ckinvsee.permissions.MCPermissionProvider
import net.ckinvsee.util.PartialStringLookupTree
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.dedicated.ServerPropertiesLoader
import net.minecraft.text.LiteralText
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.nio.file.Path
import java.time.Instant
import java.util.*
import kotlin.collections.HashSet


object CKInvSeeKotlin {
    const val MOD_ID = "ckinvsee"
    internal val Log: Logger = LogManager.getLogger(CKInvSee.MOD_ID)
    private val RedColor = Style.EMPTY.withColor(Formatting.RED)

    //                                      min|hour|day|days     (>1Month = full retention)
    private const val retentionTimeSeconds = 60L*60L*24L*30L
    private val UsernameLookup = PartialStringLookupTree(HashSet())

    // we don't expect anyone to run a command before the server started event fires!
    internal lateinit var PermsProvider : IPermissionProvider


    private var level_name: String? = null
    fun getLevelName(): String {
        if (level_name == null) {
            // read server.properties file for level-name
            level_name = ServerPropertiesLoader(Path.of("server.properties")).propertiesHandler.levelName
        }
        return level_name!!
    }


    @JvmStatic
    fun init() {
        Log.log(Level.DEBUG, "Hello from CK Inv See!")

        LifecycleEvent.SERVER_STARTED.register {
            server ->
            // load usernames to cache from UserCache with retention time
            val bestBeforeTime: Long = ((60L*60L*24L*30L)-retentionTimeSeconds)+Instant.now().epochSecond
            server.userCache.load().forEach { profile ->
                if (profile.expirationDate.toInstant().epochSecond > bestBeforeTime) {
                    UsernameLookup.insert(profile.profile.name)
                }
            }
            Log.log(Level.INFO, "Loaded ${UsernameLookup.size} usernames from cache.")

            // check if luckperms is available
            PermsProvider = if(LPPermissionProvider.isAvailable()) {
                Log.log(Level.INFO, "CKInvSee is using the LuckPerms permission system!")
                LPPermissionProvider()
            } else {
                Log.log(Level.INFO, "CKInvSee is using the default Minecraft permission system!")
                MCPermissionProvider()
            }
        }
        CommandRegistrationEvent.EVENT.register { dispatcher, selection ->
            registerCommands(dispatcher, selection)
        }
        PlayerEvent.PLAYER_JOIN.register { player ->
            UsernameLookup.insert(player.name.string)
            InvSeeScreenManager.onPlayerJoin(player)
        }
        PlayerEvent.PLAYER_QUIT.register { player ->
            InvSeeScreenManager.onPlayerLeft(player)
        }
    }

    private fun registerCommands(
        dispatcher: CommandDispatcher<ServerCommandSource>,
        selection: CommandManager.RegistrationEnvironment
    ) {
        if (selection.ordinal != CommandManager.RegistrationEnvironment.DEDICATED.ordinal) {
            return
        }

        Log.log(Level.DEBUG, "Registering Commands")

        val tryOpenScreen = {
            ctx: CommandContext<ServerCommandSource> ->
            val server = ctx.source.server
            val caller = ctx.source.player
            val target = ctx.getArgument("target_player", String::class.java)
            var isOffline = false

            // try resolve to online player
            var targetUUID = server.playerManager.playerList.firstOrNull { onlinePlayer ->
                onlinePlayer.uuidAsString == target
                        || onlinePlayer.name.string == target
                        || (
                        onlinePlayer.hasCustomName()
                                && onlinePlayer.customName!!.string == target
                        )
            }?.uuid

            if (targetUUID == null) {
                isOffline = true
                try {
                    // check if the target is A uuid
                    targetUUID = UUID.fromString(target)
                } catch (e: IllegalArgumentException) {
                    // try resolve to offline player
                    val profileCache = server.userCache.findByName(target)

                    if (profileCache.isPresent) {
                        targetUUID = profileCache.get().id
                    }
                }
            }

            //verify that we found the target
            if (targetUUID == null) {
                ctx.source.sendError(
                    LiteralText(
                        "Unable to find player/UUID "
                    ).fillStyle(RedColor).append(
                        LiteralText(
                            target
                        ).fillStyle(Style.EMPTY.withColor(Formatting.YELLOW))
                    ).append(
                        LiteralText(
                            "!"
                        ).fillStyle(RedColor)
                    )
                )

                null
            }
            // open offline screen
            else if (isOffline) {
                InvSeeScreenManager.openScreen(caller, targetUUID)
            }
            // open online screen
            else {
                server.playerManager.getPlayer(targetUUID)
                    ?.let {
                        return@let InvSeeScreenManager.openScreen(caller, it)
                    } ?: run {
                    ctx.source.sendError(Text.of("Failed to get Player from playerManager!"))
                    null
                }
            }
        }

        val invseeCMD = Command { ctx ->
            val screen = tryOpenScreen(ctx)
            if (screen == null) {
                1
            }
            else {
                // open player inventory
                screen.switchToPlayerInventory()
                0
            }
        }
        val withArgsAndPerm = { base: LiteralArgumentBuilder<ServerCommandSource>, perm: CKPermissions, cmd: Command<ServerCommandSource> ->
            base
                .requires { source -> PermsProvider.hasPermission(source.player, perm) }
                .then(
                    argument<ServerCommandSource, String?>("target_player", StringArgumentType.string())
                        .suggests { _, suggestionsBuilder ->
                            UsernameLookup.partialLookup(suggestionsBuilder.remaining)
                                .forEach { username -> suggestionsBuilder.suggest(username) }
                            suggestionsBuilder.buildFuture()
                        }
                        .executes(cmd)
                )

            base
        }

        val enderseeCMD = Command { ctx ->
            val screen = tryOpenScreen(ctx)
            if (screen == null) {
                1
            }
            else {
                // open player inventory
                screen.switchToEChestInventory()
                0
            }
        }

        dispatcher.register(withArgsAndPerm(literal("invsee"), CKPermissions.CMDInvSee, invseeCMD))
        dispatcher.register(withArgsAndPerm(literal("ck_invsee"), CKPermissions.CMDInvSee, invseeCMD))

        dispatcher.register(withArgsAndPerm(literal("endersee"), CKPermissions.CMDEnderSee, enderseeCMD))
        dispatcher.register(withArgsAndPerm(literal("ck_endersee"), CKPermissions.CMDEnderSee, enderseeCMD))

    }
}