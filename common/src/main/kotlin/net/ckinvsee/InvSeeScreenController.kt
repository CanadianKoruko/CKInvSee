package net.ckinvsee

import net.ckinvsee.invproviders.*
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.LiteralText
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Util
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

    internal fun playerJoined(player: ServerPlayerEntity) {
        val uuid = player.uuid

        if (callee.uuid == uuid) {
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


    /// Switches the screen to the Ender Chest Inventory
    fun switchToEChestInventory() { proxyInv.switchTo(ScreenMappings.EnderInv) }
    /// Switches the screen to the Player Inventory
    fun switchToPlayerInventory() { proxyInv.switchTo(ScreenMappings.PlayerInv) }

    private class ProxyInventory(var playerInv: IPlayerInventory, private val controller: InvSeeScreenController) : Inventory {
        companion object {
            val borderItemStack = ItemStack(Items.GRAY_STAINED_GLASS_PANE, 1)
            val selectionButtonItemStack = ItemStack(Items.BARRIER, 1)
        }

        // inventory selection mapper (lets us switch between multiple inventory screens)
        private var invSelect: InventorySelect = ScreenMappings.PlayerInv

        private var inInvSelection = true

        override fun onClose(player: PlayerEntity?) {
            if (player?.uuid == controller.callee.uuid) {
                controller.close()
            } else {
                CKInvSeeKotlin.Log.log(Level.WARN, "Unknown Entity closed InvSee Screen!")
            }
        }

        /// Switch to Player Inventory
        fun changeInventoryTo(newInventory: IPlayerInventory) {
            ScreenMappings.SupportedInventories.forEach {
                mapping -> mapping.setInv(newInventory, mapping.getInv(playerInv))
            }

            playerInv = newInventory
        }

        /// Switch to Specified Mappings, (due note, custom mappings are not retained cross player join/leaves)
        fun switchTo(select: InventorySelect) {
            if(select.hasPerms(controller.callee)) {
                invSelect = select
                inInvSelection = false
            }
            else {
                controller.callee.sendSystemMessage(
                    LiteralText("You do not have the required permissions!").fillStyle(Style.EMPTY.withColor(Formatting.RED)),
                    Util.NIL_UUID
                    )
            }
        }

        /// Switches to the inventory selection screen
        fun openInvSelection() {
            inInvSelection = true
        }

        override fun clear() {
            if (inInvSelection) {
                return
            }
            invSelect.setInv(playerInv, Array(IPlayerInventory.InventorySlots) { ItemStack.EMPTY })
        }

        override fun size(): Int {
            return 54 // static 9x6 chest size
        }

        override fun isEmpty(): Boolean {
            if(inInvSelection) {
                return false
            }
            return invSelect.getInv(playerInv).firstOrNull { item -> !item.isEmpty } != null
        }

        override fun getStack(slot: Int): ItemStack {
            if (inInvSelection) {
                val selection = ScreenMappings.SupportedInventories.getOrNull(slot) ?: return ItemStack.EMPTY

                val item = ItemStack(selection.item, 1)
                item.setCustomName(Text.of(selection.name))

                return item
            } else if (slot == ScreenMappings.ToSelectMenu) {
                return selectionButtonItemStack
            }

            return invSelect.getSlot(playerInv, invSelect.slotMap.getOrElse(slot) { return@getStack borderItemStack })
        }

        override fun removeStack(slot: Int, amount: Int): ItemStack {
            if(inInvSelection) {
                selectionStackClick(slot)
                return ItemStack.EMPTY
            } else if (slot == ScreenMappings.ToSelectMenu) {
                openInvSelection()
                return ItemStack.EMPTY
            }

            val index = invSelect.slotMap.getOrElse(slot) { return ItemStack.EMPTY }
            val stack = invSelect.getSlot(playerInv, index)

            if (amount >= stack.count) {
                invSelect.setSlot(playerInv, index, ItemStack.EMPTY)
                return stack
            }

            val newStack = stack.copy()
            newStack.count = amount
            stack.count = stack.count - amount

            invSelect.setSlot(playerInv, index, stack)
            return newStack
        }

        override fun removeStack(slot: Int): ItemStack {
            if (inInvSelection) {
                selectionStackClick(slot)
                return ItemStack.EMPTY
            } else if (slot == ScreenMappings.ToSelectMenu) {
                openInvSelection()
                return ItemStack.EMPTY
            }

            val index = invSelect.slotMap.getOrElse(slot) { return ItemStack.EMPTY }
            val stack = invSelect.getSlot(playerInv, index)
            invSelect.setSlot(playerInv, index, ItemStack.EMPTY)
            return stack
        }

        fun selectionStackClick(slot: Int) {
            val selection = ScreenMappings.SupportedInventories.getOrNull(slot) ?: return
            switchTo(selection)
        }

        override fun setStack(slot: Int, stack: ItemStack?) {
            if(!inInvSelection && isValidItemForSlot(slot, stack)) {
                val index = invSelect.slotMap.getOrElse(slot) { return }
                invSelect.setSlot(playerInv, index, stack ?: ItemStack.EMPTY)
            }
        }

        override fun markDirty() { /*do nothing*/ }

        fun isValidItemForSlot(slot: Int, stack: ItemStack?): Boolean {
            if (inInvSelection) {
                return stack?.isEmpty ?: true
            }

            if (stack == null)
                return true
            if (stack.isEmpty)
                return true

            val index = invSelect.slotMap.getOrElse(slot) {return false}
            return  invSelect.isValid(index, stack)
        }

        override fun canPlayerUse(player: PlayerEntity?): Boolean {
            return player?.uuid == controller.callee.uuid
        }
    }

    private class ProxySlot(val slotid: Int, val proxyInv: ProxyInventory, x: Int, y: Int) : Slot(
        proxyInv,
        slotid,
        x,
        y
    ) {
        override fun canInsert(stack: ItemStack?): Boolean {
            return proxyInv.isValidItemForSlot(slotid, stack)
        }
    }

    class ScreenFactory(private val controller: InvSeeScreenController) : NamedScreenHandlerFactory {
        override fun createMenu(syncId: Int, inv: PlayerInventory?, player: PlayerEntity?): ScreenHandler {
            if (player?.uuid == controller.callee.uuid) {

                controller.handler = GenericContainerScreenHandler.createGeneric9x6(syncId, inv, controller.proxyInv)
                controller.handler.sendContentUpdates()

                // we need to override the container slots, to inject our restrictions for those slots
                controller.handler.slots.replaceAll { slot ->
                    if(slot.inventory == controller.proxyInv) {
                        ProxySlot(slot.index, controller.proxyInv, slot.x, slot.y)
                    }
                    else
                    {
                        slot
                    }
                }

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