package net.ckinvsee.invproviders

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap
import net.ckinvsee.CKInvSeeKotlin
import net.ckinvsee.util.IntRange
import net.minecraft.item.ItemStack
import net.minecraft.nbt.AbstractNbtList
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import org.apache.logging.log4j.Level
import java.io.File
import java.io.FileNotFoundException
import java.util.*

class OfflinePlayerInventory(val uuid: UUID) : IPlayerInventory {

    companion object {
        fun byUserCacheUUID(uuid: UUID): OfflinePlayerInventory {
            return OfflinePlayerInventory(uuid)
        }

        const val SLOT_ARMOR_HEAD = 103
        const val SLOT_ARMOR_CHEST = 102
        const val SLOT_ARMOR_LEGS = 101
        const val SLOT_ARMOR_FEET = 100
        const val SLOT_OFFHAND = -106

        val HotbarRow = IntRange(0, 8)
        val FirstRow = IntRange(9, 17)
        val SecondRow = IntRange(18, 26)
        val ThirdRow = IntRange(27, 35)

        // third row right side is 35
        // hotbar: 0-8
        // inventory first->third left->right 9-35
        val toCommonSlotsMap = Int2IntArrayMap(
            mapOf(
                SLOT_ARMOR_HEAD to IPlayerInventory.SLOT_ARMOR_HEAD,
                SLOT_ARMOR_CHEST to IPlayerInventory.SLOT_ARMOR_CHEST,
                SLOT_ARMOR_LEGS to IPlayerInventory.SLOT_ARMOR_LEGS,
                SLOT_ARMOR_FEET to IPlayerInventory.SLOT_ARMOR_FEET,
                SLOT_OFFHAND to IPlayerInventory.SLOT_OFFHAND
            ).plus(
                FirstRow.mapRanges(IPlayerInventory.FirstRow)
            ).plus(
                SecondRow.mapRanges(IPlayerInventory.SecondRow)
            ).plus(
                ThirdRow.mapRanges(IPlayerInventory.ThirdRow)
            ).plus(
                HotbarRow.mapRanges(IPlayerInventory.HotbarRow)
            )
        )
        val fromCommonSlotsMap = Int2IntArrayMap(toCommonSlotsMap.map { pair -> Pair(pair.value, pair.key) }.toMap())
    }

    private val playerFile: File = File("${CKInvSeeKotlin.getLevelName()}/playerdata/$uuid.dat")
    val data: NbtCompound
    val inventory: AbstractNbtList<NbtCompound>
    val echest: AbstractNbtList<NbtCompound>

    init {
        if (!playerFile.exists()) {
            throw FileNotFoundException("playerdata file not found!")
        }

        val nbtData = NbtIo.readCompressed(playerFile)
        if (nbtData != null) {
            data = nbtData


            @Suppress("UNCHECKED_CAST")
            inventory = data.get("Inventory") as? AbstractNbtList<NbtCompound>
                ?: throw Exception("unable to create AbstractNbtList from Inventory List!")
            @Suppress("UNCHECKED_CAST")
            echest = data.get("EnderItems") as? AbstractNbtList<NbtCompound>
                ?: throw Exception("Unable to create AbstractNBTList from EnderItems List!")

        } else {
            CKInvSeeKotlin.Log.log(Level.ERROR, "failed to read nbt of playerdata!")
            throw Exception("failed to read nbt of playerdata!")
        }
    }

    override fun isOffline(): Boolean {
        return true
    }

    override fun getPlayerUUID(): UUID {
        return uuid
    }


    /// saves any changes to disk
    fun save() {
        NbtIo.writeCompressed(data, playerFile)
    }


    override fun getInventory(): Array<ItemStack> {
        val items = toItemSlotList(inventory)

        // create inventory array
        return Array(IPlayerInventory.InventorySlots) {
            index ->
            items.firstOrNull { item -> (toCommonSlotsMap.getOrElse(item.first) {item.first}) == index }
                ?.second
            ?: ItemStack.EMPTY
        }
    }

    override fun setInventory(inv: Array<ItemStack>) {
        if (inv.size != IPlayerInventory.InventorySlots) {
            throw IllegalArgumentException("inv is not a Common Mapped Inventory!")
        }

        // wipe old inventory
        inventory.clear()

        // fill with new inventory
        inv.forEachIndexed() { slot, item ->
            if (!item.isEmpty) {
                inventory.add(toNBT(item, fromCommonSlotsMap.getOrElse(slot) { slot }))
            }
        }
    }

    override fun getInvSlot(slotId: Int): ItemStack {
        val index = fromCommonSlotsMap.getOrElse(slotId) { throw IllegalArgumentException("Invalid Common Slot Id") }

        return tryFindExistingNBTSlot(inventory, index)?.let { keyIndex ->
            toItemStack(inventory[keyIndex]).first
        } ?: ItemStack.EMPTY
    }

    override fun setInvSlot(slotId: Int, item: ItemStack) {
        val index = fromCommonSlotsMap.getOrElse(slotId) { throw IllegalArgumentException("Invalid Common Slot Id") }
        tryFindExistingNBTSlot(inventory, index)?.let {
            keyIndex ->
            if (item.isEmpty) {
                inventory.removeAt(keyIndex)
            } else {
                inventory[keyIndex] = toNBT(item, index)
            }
        } ?: run {
            if (!item.isEmpty) {
                insertAtEnd(inventory, item, index)
            }
        }
    }


    // Ender Chest -------------------------------------------------------------
    override fun getEnderInventory(): Array<ItemStack> {
        val items = toItemSlotList(echest)

        return Array(IPlayerInventory.EnderSlots) {
            index ->
            items.firstOrNull { item -> item.first == index }
                ?.second
            ?: ItemStack.EMPTY
        }
    }

    override fun setEnderInventory(inv: Array<ItemStack>) {
        if (inv.size != IPlayerInventory.EnderSlots) {
            throw IllegalArgumentException("inv is not a Common Mapped EnderChest Inventory!")
        }

        // wipe old inventory
        echest.clear()

        // fill with new inventory
        inv.forEachIndexed {
            slot, item ->
            if(!item.isEmpty) {
                echest.add(toNBT(item, slot))
            }
        }
    }

    override fun getEnderSlot(slotId: Int): ItemStack {
        tryFindExistingNBTSlot(echest, slotId) ?.let {
            key ->
            return toItemStack(echest[key]).first
        } ?: run {
            return ItemStack.EMPTY
        }
    }

    override fun setEnderSlot(slotId: Int, item: ItemStack) {
        tryFindExistingNBTSlot(echest, slotId) ?.let {
            keyIndex ->
            if (item.isEmpty) {
                echest.removeAt(keyIndex)
            } else {
                echest[keyIndex] = toNBT(item, slotId)
            }
        } ?: run {
            if(!item.isEmpty) {
                insertAtEnd(echest, item, slotId)
            }
        }
    }


    // Helpers -----------------------------------------------------------------
    private fun toItemSlotList(inv: AbstractNbtList<NbtCompound>) : LinkedList<Pair<Int, ItemStack>> {
        val items = LinkedList<Pair<Int, ItemStack>>()

        // load saved inventory
        inv.forEach { item ->
            val itemSlot = toItemStack(item as NbtCompound)

            //remap slot to Common Slots and insert
            items.addLast(Pair(itemSlot.second, itemSlot.first))
        }

        return items
    }

    private fun toItemStack(nbt: NbtCompound): Pair<ItemStack, Int> {
        val itemStack = ItemStack(Registry.ITEM.get(Identifier(nbt.getString("id"))), nbt.getInt("Count"))

        if (nbt.contains("tag")) {
            val itemNBT = nbt.getCompound("tag")
            itemStack.nbt = itemNBT
        }
        return Pair(itemStack, nbt.getInt("Slot"))
    }

    private fun toNBT(item: ItemStack, slot: Int): NbtCompound {
        val itm = NbtCompound()

        itm.putInt("Slot", slot)
        itm.putInt("Count", item.count)
        itm.putString("id", Registry.ITEM.getId(item.item).toString())

        if (item.hasNbt()) {
            itm.put("tag", item.nbt)
        }

        return itm
    }

    private fun tryFindExistingNBTSlot(inv: AbstractNbtList<NbtCompound>, slotId: Int): Int? {

        for (index in 0 until inv.size) {
            val itm = inv[index]
            if (itm.getInt("Slot") == slotId) {
                return index
            }
        }

        return null
    }

    private fun insertAtEnd(inv: AbstractNbtList<NbtCompound>, item: ItemStack, slot: Int) {
        val itm = toNBT(item, slot)
        if (!inv.add(itm)) {
            CKInvSeeKotlin.Log.log(Level.ERROR, "failed to place item into offline inventory!")
        }
    }
}