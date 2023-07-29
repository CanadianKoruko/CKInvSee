package net.ckinvsee

import net.minecraft.server.network.ServerPlayerEntity
import java.util.*

internal object InvSeeScreenManager {
    fun openScreen(caller: ServerPlayerEntity, target: UUID) : InvSeeScreenController {
        val controller = InvSeeScreenController.openAsOffline(caller, target)
        registerController(controller)
        return controller
    }

    fun openScreen(caller: ServerPlayerEntity, target: ServerPlayerEntity) : InvSeeScreenController {
        val controller = InvSeeScreenController.openAsOnline(caller, target)
        registerController(controller)
        return controller
    }

    private fun registerController(controller: InvSeeScreenController) {
        controllerList.addLast(controller)
        controllerTargetLookup.getOrPut(controller.targetUUID) { LinkedList() }.add(controller)
        controllerTargetLookup.getOrPut(controller.callee.uuid) { LinkedList() }.add(controller)
    }

    private val controllerList = LinkedList<InvSeeScreenController>()
    private val controllerTargetLookup = HashMap<UUID, LinkedList<InvSeeScreenController>>()

    internal fun screenClosed(controller: InvSeeScreenController) {
        controllerTargetLookup[controller.callee.uuid]?.let { controllers ->
            controllers.remove(controller)
            if (controllers.isEmpty()) {
                controllerTargetLookup.remove(controller.callee.uuid)
            }
        }
        controllerTargetLookup[controller.targetUUID]?.let { controllers ->
            controllers.remove(controller)
            if (controllers.isEmpty()) {
                controllerTargetLookup.remove(controller.targetUUID)
            }
        }
    }


    internal fun onPlayerJoin(player: ServerPlayerEntity) {
        val uuid = player.uuid
        val controllers = controllerTargetLookup.getOrElse(uuid) { return }
        controllers.forEach { controller ->
            controller.playerJoined(player)
        }
    }

    internal fun onPlayerLeft(player: ServerPlayerEntity) {
        val uuid = player.uuid
        val controllers = controllerTargetLookup.getOrElse(uuid) { return }
        controllers.forEach { controller ->
            controller.playerLeft(player)
        }
    }
}