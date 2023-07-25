package net.ckinvsee.invproviders

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap
import net.ckinvsee.util.IntRange
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity

class OnlinePlayerInventory(private val player: ServerPlayerEntity) : IPlayerInventory {
    companion object {
        val HotbarRow = IntRange(0, 8)
        val FirstRow = IntRange(9, 17)
        val SecondRow = IntRange(18, 26)
        val ThirdRow = IntRange(27, 35)

        val toCommonSlotsMap = Int2IntArrayMap(
            FirstRow.mapRanges(IPlayerInventory.FirstRow)
                .plus(
                    SecondRow.mapRanges(IPlayerInventory.SecondRow)
                ).plus(
                    ThirdRow.mapRanges(IPlayerInventory.ThirdRow)
                ).plus(
                    HotbarRow.mapRanges(IPlayerInventory.HotbarRow)
                )
        )
        val fromCommonSlotsMap = Int2IntArrayMap(toCommonSlotsMap.map { pair -> Pair(pair.value, pair.key) }.toMap())
    }

    override fun getInventory(): Array<ItemStack> {
        return Array(IPlayerInventory.InventorySlots) { index ->
            getInvSlot(index)
        }
    }

    override fun setInventory(inv: Array<ItemStack>) {
        inv.forEachIndexed { index, item ->
            setInvSlot(index, item)
        }
    }

    override fun getInvSlot(slotId: Int): ItemStack {
        return when (slotId) {
            IPlayerInventory.SLOT_ARMOR_HEAD -> {
                player.inventory.armor[3]
            }

            IPlayerInventory.SLOT_ARMOR_CHEST -> {
                player.inventory.armor[2]
            }

            IPlayerInventory.SLOT_ARMOR_LEGS -> {
                player.inventory.armor[1]
            }

            IPlayerInventory.SLOT_ARMOR_FEET -> {
                player.inventory.armor[0]
            }

            IPlayerInventory.SLOT_OFFHAND -> {
                player.inventory.offHand[0]
            }

            else -> {
                player.inventory.main[
                    fromCommonSlotsMap.getOrElse(slotId) {
                        throw IndexOutOfBoundsException("unknown common slot index")
                    }
                ]
            }
        }
    }

    override fun setInvSlot(slotId: Int, item: ItemStack) {
        when (slotId) {
            IPlayerInventory.SLOT_ARMOR_HEAD -> {
                player.inventory.armor[3] = item
            }

            IPlayerInventory.SLOT_ARMOR_CHEST -> {
                player.inventory.armor[2] = item
            }

            IPlayerInventory.SLOT_ARMOR_LEGS -> {
                player.inventory.armor[1] = item
            }

            IPlayerInventory.SLOT_ARMOR_FEET -> {
                player.inventory.armor[0] = item
            }

            IPlayerInventory.SLOT_OFFHAND -> {
                player.inventory.offHand[0] = item
            }

            else -> {
                player.inventory.main[
                    fromCommonSlotsMap.getOrElse(slotId) {
                        throw IndexOutOfBoundsException("unknown common slot index")
                    }
                ] = item
            }
        }
    }
}