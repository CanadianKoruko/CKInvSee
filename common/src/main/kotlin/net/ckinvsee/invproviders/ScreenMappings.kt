package net.ckinvsee.invproviders

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap
import net.ckinvsee.CKInvSeeKotlin
import net.ckinvsee.permissions.CKPermissions
import net.ckinvsee.util.IntRange
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.ArmorItem
import net.minecraft.item.Items


object ScreenMappings {
    /// Protected Slot for the Menu Selection Button
    const val ToSelectMenu = 8

    @Suppress("UNUSED")
    val FirstRow = IntRange(0, 8)
    @Suppress("UNUSED")
    val SecondRow = IntRange(9, 17)
    val ThirdRow = IntRange(18, 26)
    val FourthRow = IntRange(27, 35)
    val FifthRow = IntRange(36, 44)
    val SixthRow = IntRange(45, 53)


    internal val SupportedInventories: ArrayList<InventorySelect> = arrayListOf()

    // I know not why, I care not why, but you can't join the declaration of these two!
    @Suppress("JoinDeclarationAndAssignment")
    val PlayerInv: InventorySelect
    val EnderInv: InventorySelect

    init {

        PlayerInv = InventorySelect(
            "Player Inventory",
            Items.PLAYER_HEAD,
            Int2IntArrayMap(
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
            ),
            IPlayerInventory::getInventory,
            IPlayerInventory::setInventory,
            IPlayerInventory::getInvSlot,
            IPlayerInventory::setInvSlot,
            { slot, stack ->
                when(slot) {
                    IPlayerInventory.SLOT_ARMOR_HEAD -> {
                        val item = stack.item
                        item is ArmorItem && item.slotType == EquipmentSlot.HEAD
                    }
                    IPlayerInventory.SLOT_ARMOR_CHEST -> {
                        val item = stack.item
                        item is ArmorItem && item.slotType == EquipmentSlot.CHEST
                    }
                    IPlayerInventory.SLOT_ARMOR_LEGS -> {
                        val item = stack.item
                        item is ArmorItem && item.slotType == EquipmentSlot.LEGS
                    }
                    IPlayerInventory.SLOT_ARMOR_FEET -> {
                        val item = stack.item
                        item is ArmorItem && item.slotType == EquipmentSlot.FEET
                    }
                    else -> true
                }
            },
            { player -> CKInvSeeKotlin.PermsProvider.hasPermission(player, CKPermissions.CMDInvSee) }
        )

        EnderInv = InventorySelect(
            "EChest",
            Items.ENDER_CHEST,
            Int2IntArrayMap(
                    FourthRow.mapRanges(FirstRow)
                .plus(
                    FifthRow.mapRanges(SecondRow)
                ).plus(
                    SixthRow.mapRanges(ThirdRow)
                )
            ),
            IPlayerInventory::getEnderInventory,
            IPlayerInventory::setEnderInventory,
            IPlayerInventory::getEnderSlot,
            IPlayerInventory::setEnderSlot,
            { _,_ -> true },
            { player -> CKInvSeeKotlin.PermsProvider.hasPermission(player, CKPermissions.CMDEnderSee) }
        )

        registerInventory(PlayerInv)
        registerInventory(EnderInv)
    }

    // TODO("Implement Trinkets/Curios")

    fun registerInventory(selector: InventorySelect) {
        // ensure ToSelectMenu is never overcast
        assert(!selector.slotMap.contains(ToSelectMenu))
        SupportedInventories.add(selector)
    }
}