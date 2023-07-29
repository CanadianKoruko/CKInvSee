package net.ckinvsee.invproviders

import net.ckinvsee.util.IntRange
import net.minecraft.item.ItemStack
import java.util.UUID

interface IPlayerInventory {

    companion object CommonMappings {
        // common Mappings

        const val InventorySlots = 41

        val FirstRow = IntRange(0, 8)
        val SecondRow = IntRange(9, 17)
        val ThirdRow = IntRange(18, 26)
        val HotbarRow = IntRange(27, 35)

        const val SLOT_ARMOR_HEAD = 36
        const val SLOT_ARMOR_CHEST = 37
        const val SLOT_ARMOR_LEGS = 38
        const val SLOT_ARMOR_FEET = 39
        const val SLOT_OFFHAND = 40


        const val EnderSlots = 27

    }


    /// is OfflinePlayerInventory else is OnlinePlayerInventory
    fun isOffline() : Boolean
    /// Player UUID
    fun getPlayerUUID(): UUID



    /// get Player Inventory with CommonMappings
    fun getInventory(): Array<ItemStack>
    /// set Player Inventory with CommonMappings
    fun setInventory(inv: Array<ItemStack>)
    /// get Player Inventory Slot with CommonMappings
    fun getInvSlot(slotId: Int): ItemStack
    /// set Player Inventory Slot with CommonMappings
    fun setInvSlot(slotId: Int, item: ItemStack)



    /// get Player Ender Chest Inventory
    fun getEnderInventory(): Array<ItemStack>
    /// set Player Ender Chest Inventory
    fun setEnderInventory(inv: Array<ItemStack>)
    /// get Player Ender Chest Inventory Slot
    fun getEnderSlot(slotId: Int): ItemStack
    /// set Player Ender Chest Inventory Slot
    fun setEnderSlot(slotId: Int, item: ItemStack)
}