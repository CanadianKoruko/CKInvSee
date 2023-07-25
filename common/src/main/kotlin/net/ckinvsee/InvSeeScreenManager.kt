package net.ckinvsee

import net.minecraft.server.network.ServerPlayerEntity
import java.util.*

object InvSeeScreenManager {
    fun openScreen(caller: ServerPlayerEntity, target: UUID) {
        registerController(InvSeeScreenController.openAsOffline(caller, target))
    }

    fun openScreen(caller: ServerPlayerEntity, target: ServerPlayerEntity) {
        registerController(InvSeeScreenController.openAsOnline(caller, target))
    }

    private fun registerController(controller: InvSeeScreenController) {
        controllerList.addLast(controller)
        controllerTargetLookup.getOrPut(controller.targetUUID) { LinkedList() }.add(controller)
        controllerTargetLookup.getOrPut(controller.callee.uuid) { LinkedList() }.add(controller)
    }

    private val controllerList = LinkedList<InvSeeScreenController>()
    private val controllerTargetLookup = HashMap<UUID, LinkedList<InvSeeScreenController>>()
    private val dirtyList = LinkedList<InvSeeScreenController>()

    internal fun markDirty(controller: InvSeeScreenController) {
        dirtyList.add(controller)
    }

    internal fun screenClosed(controller: InvSeeScreenController) {
        dirtyList.remove(controller)
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


    internal fun tick() {
        dirtyList.forEach() { controller ->
            controller.update()
        }
        dirtyList.clear()
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