package net.ckinvsee.invproviders

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity

class InventorySelect(
    /// selection page inventory name
    val name: String,
    /// selection page inventory display item
    val item: Item,
    /// 9x6 mappings for this inventory
    val slotMap: Int2IntArrayMap,
    /// get the entire inventory
    val getInv: (IPlayerInventory) -> Array<ItemStack>,
    /// set the entire inventory
    val setInv: (IPlayerInventory, Array<ItemStack>) -> Unit,
    /// get the specified slot
    val getSlot: (IPlayerInventory, Int) -> ItemStack,
    /// set the specified slot
    val setSlot: (IPlayerInventory, Int, ItemStack) -> Unit,
    /// Specify whether the selected itemStack is able to be inserted into that specific slot!
    val isValid: (Int, ItemStack) -> Boolean,
    /// Permissions
    val hasPerms: (callee: ServerPlayerEntity) -> Boolean
)