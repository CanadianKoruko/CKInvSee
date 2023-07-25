package net.ckinvsee

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap
import net.ckinvsee.invproviders.IPlayerInventory
import net.ckinvsee.invproviders.OfflinePlayerInventory
import net.ckinvsee.invproviders.OnlinePlayerInventory
import net.ckinvsee.util.IntRange
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerListener
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.apache.logging.log4j.Level
import java.util.*

internal class InvSeeScreenController(
    val callee: ServerPlayerEntity,
    targetInv: IPlayerInventory,
    val targetUUID: UUID
) {

    companion object {
        fun openAsOnline(callee: ServerPlayerEntity, target: ServerPlayerEntity): InvSeeScreenController {
            return InvSeeScreenController(callee, OnlinePlayerInventory(target), target.uuid)
        }

        fun openAsOffline(callee: ServerPlayerEntity, target: UUID): InvSeeScreenController {
            return InvSeeScreenController(callee, OfflinePlayerInventory.byUserCacheUUID(target), target)
        }
    }

    private val proxyInv: ProxyInventory = ProxyInventory(targetInv, this)
    private lateinit var handler: GenericContainerScreenHandler

    init {
        callee.openHandledScreen(ScreenFactory(this))
    }

    private var isClosing = false
    private var isDirty = false
    private fun markDirty() {
        if (!isDirty) {
            isDirty = true
            InvSeeScreenManager.markDirty(this)
        }
    }

    // events
    internal fun update() {
        handler.updateToClient()
        isDirty = false
    }

    internal fun playerJoined(player: ServerPlayerEntity) {
        val uuid = player.uuid

        if (callee.uuid == uuid) {
            @Suppress("UsePropertyAccessSyntax")
            CKInvSeeKotlin.Log.log(
                Level.WARN,
                "Received PlayerJoined on InvSeeController for player: ${player.name.getString()} ($uuid)"
            )
        } else if (targetUUID == uuid) {
            // switch to online screen
            proxyInv.changeInventoryTo(OnlinePlayerInventory(player))
        }
    }

    internal fun playerLeft(player: ServerPlayerEntity) {
        val uuid = player.uuid

        if (callee.uuid == uuid) {
            close()
        } else if (targetUUID == uuid) {
            // switch to offline screen
            proxyInv.changeInventoryTo(OfflinePlayerInventory.byUserCacheUUID(targetUUID))
        }
    }

    internal fun close() {
        if (!isClosing) {
            isClosing = true
            handler.close(callee)
            // save offline player inventory
            (proxyInv.playerInv as? OfflinePlayerInventory)?.save()
            InvSeeScreenManager.screenClosed(this)
        }
    }

    private class ProxyInventory(var playerInv: IPlayerInventory, private val controller: InvSeeScreenController) :
        Inventory {
        companion object {
            //TODO("Implement Curios")

            @Suppress("UNUSED")
            val FirstRow = IntRange(0, 8)
            @Suppress("UNUSED")
            val SecondRow = IntRange(9, 17)
            val ThirdRow = IntRange(18, 26)
            val FourthRow = IntRange(27, 35)
            val FifthRow = IntRange(36, 44)
            val SixthRow = IntRange(45, 53)

            val borderItemStack = ItemStack(Items.BARRIER, 1)
            val toCommonSlotsMap = Int2IntArrayMap(
                mapOf(
                    0 to IPlayerInventory.SLOT_ARMOR_HEAD,
                    1 to IPlayerInventory.SLOT_ARMOR_CHEST,
                    2 to IPlayerInventory.SLOT_ARMOR_LEGS,
                    3 to IPlayerInventory.SLOT_ARMOR_FEET,
                    5 to IPlayerInventory.SLOT_OFFHAND
                ).plus(
                    ThirdRow.mapRanges(IPlayerInventory.FirstRow)
                ).plus(
                    FourthRow.mapRanges(IPlayerInventory.SecondRow)
                ).plus(
                    FifthRow.mapRanges(IPlayerInventory.ThirdRow)
                ).plus(
                    SixthRow.mapRanges(IPlayerInventory.HotbarRow)
                )
            )
            @Suppress("UNUSED")
            val fromCommonSlotsMap =
                Int2IntArrayMap(OfflinePlayerInventory.toCommonSlotsMap.map { pair -> Pair(pair.value, pair.key) }
                    .toMap())
        }

        override fun onClose(player: PlayerEntity?) {
            if (player?.uuid == controller.callee.uuid) {
                controller.close()
            } else {
                CKInvSeeKotlin.Log.log(Level.WARN, "Unknown Entity closed InvSee Screen!")
            }
        }

        fun changeInventoryTo(newInventory: IPlayerInventory) {
            val cacheInv = playerInv.getInventory()
            playerInv = newInventory
            newInventory.setInventory(cacheInv)
            markDirty()
        }

        override fun clear() {
            playerInv.setInventory(Array(IPlayerInventory.InventorySlots) { ItemStack.EMPTY })
            markDirty()
        }

        override fun size(): Int {
            return 54
        }

        override fun isEmpty(): Boolean {
            return playerInv.getInventory().firstOrNull { item -> !item.isEmpty } != null
        }

        override fun getStack(slot: Int): ItemStack {
            return playerInv.getInvSlot(toCommonSlotsMap.getOrElse(slot) { return borderItemStack })
        }

        override fun removeStack(slot: Int, amount: Int): ItemStack {
            val index = toCommonSlotsMap.getOrElse(slot) { markDirty(); return ItemStack.EMPTY }
            val stack = playerInv.getInvSlot(index)

            if (amount >= stack.count) {
                playerInv.setInvSlot(index, ItemStack.EMPTY)
                return stack
            }

            val newStack = stack.copy()
            newStack.count = amount
            stack.count = stack.count - amount

            playerInv.setInvSlot(index, stack)
            return newStack
        }

        override fun removeStack(slot: Int): ItemStack {
            val index = toCommonSlotsMap.getOrElse(slot) { markDirty(); return ItemStack.EMPTY }
            val stack = playerInv.getInvSlot(index)
            playerInv.setInvSlot(index, ItemStack.EMPTY)
            return stack
        }

        override fun setStack(slot: Int, stack: ItemStack?) {
            val index = toCommonSlotsMap.getOrElse(slot) { markDirty(); return }
            playerInv.setInvSlot(index, stack ?: ItemStack.EMPTY)
        }

        override fun markDirty() {
            controller.markDirty()
        }

        override fun canPlayerUse(player: PlayerEntity?): Boolean {
            return player?.uuid == controller.callee.uuid
        }
    }

    class HandlerListener(private val controller: InvSeeScreenController) : ScreenHandlerListener {
        override fun onSlotUpdate(handler: ScreenHandler?, slotId: Int, stack: ItemStack?) {
            if (handler == null) {
                controller.close()
            } else {
                controller.markDirty()
            }
        }

        override fun onPropertyUpdate(handler: ScreenHandler?, property: Int, value: Int) {
            if (handler == null) {
                controller.close()
            }
        }

    }

    class ScreenFactory(private val controller: InvSeeScreenController) : NamedScreenHandlerFactory {
        override fun createMenu(syncId: Int, inv: PlayerInventory?, player: PlayerEntity?): ScreenHandler {
            if (player?.uuid == controller.callee.uuid) {

                controller.handler = GenericContainerScreenHandler.createGeneric9x6(syncId, inv, controller.proxyInv)
                controller.handler.enableSyncing()
                controller.handler.sendContentUpdates()
                controller.handler.addListener(HandlerListener(controller))

                return controller.handler
            } else {
                CKInvSeeKotlin.Log.log(Level.WARN, "Unknown non-Player accessed CKInvSee Screen factory!")
                throw IllegalAccessException("Call site must originate from expected callee player")
            }
        }

        override fun getDisplayName(): Text {
            return Text.of("CKInvSee Proxy Inventory")
        }

    }
}