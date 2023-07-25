package net.ckinvsee.invproviders

import net.ckinvsee.util.IntRange
import net.minecraft.item.ItemStack

interface IPlayerInventory {

    companion object {
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

        @Suppress("UNUSED")
        const val EnderSlots = 27
    }

    fun getInventory(): Array<ItemStack>
    fun setInventory(inv: Array<ItemStack>)

    fun getInvSlot(slotId: Int): ItemStack
    fun setInvSlot(slotId: Int, item: ItemStack)

    //TODO("getEnderInventory/setEnderInventory  /  getEnderSlot/setEnderSlot")
}