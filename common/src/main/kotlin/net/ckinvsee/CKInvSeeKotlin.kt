package net.ckinvsee

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import dev.architectury.event.events.common.CommandRegistrationEvent
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.event.events.common.PlayerEvent
import dev.architectury.event.events.common.TickEvent
import net.ckinvsee.permissions.CKPermissions
import net.ckinvsee.permissions.IPermissionProvider
import net.ckinvsee.permissions.LPPermissionProvider
import net.ckinvsee.permissions.MCPermissionProvider
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
import java.util.*


object CKInvSeeKotlin {
    const val MOD_ID = "ckinvsee"
    internal val Log: Logger = LogManager.getLogger(CKInvSee.MOD_ID)

    // we don't expect anyone to run a command before the server started event fires!
    private lateinit var PermsProvider : IPermissionProvider

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
            _ ->

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
        TickEvent.SERVER_PRE.register { _ ->
            InvSeeScreenManager.tick()
        }
        PlayerEvent.PLAYER_JOIN.register { player ->
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
        val RedColor = Style.EMPTY.withColor(Formatting.RED)

        val invseeCMD = Command<ServerCommandSource> { ctx ->
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

                return@Command 1
            }


            if (isOffline) {
                InvSeeScreenManager.openScreen(caller, targetUUID)
            } else {
                return@Command server.playerManager.getPlayer(targetUUID)
                    ?.let {
                        InvSeeScreenManager.openScreen(caller, it)
                        0
                    } ?: run {
                    ctx.source.sendError(Text.of("Failed to get Player from playerManager!"))
                    1
                }
            }
            0
        }
        val registerInvsee = { base: LiteralArgumentBuilder<ServerCommandSource> ->
            base
                .requires { source -> PermsProvider.hasPermission(source.player, CKPermissions.CMDInvSee) }
                .then(
                    argument<ServerCommandSource, String?>("target_player", StringArgumentType.string())
                        .executes(invseeCMD)
                )

            base
        }

        dispatcher.register(registerInvsee(literal("invsee")))
        dispatcher.register(registerInvsee(literal("ck_invsee")))

        //TODO("ck_endersee/endersee")

    }
}