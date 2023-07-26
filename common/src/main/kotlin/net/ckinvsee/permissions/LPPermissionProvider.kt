package net.ckinvsee.permissions

import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.node.types.PermissionNode
import net.minecraft.server.network.ServerPlayerEntity

class LPPermissionProvider : IPermissionProvider {
    companion object {
        fun isAvailable(): Boolean {
            // forge : ModList
            // fabric:
            return dev.architectury.platform.Platform.isModLoaded("luckperms")
        }
    }
    private val luckperms: LuckPerms = LuckPermsProvider.get()
    private val playerAdapter = luckperms.getPlayerAdapter(ServerPlayerEntity::class.java)
    init {
        // register permissions
        CKPermissions.values().forEach {
            perm ->
            PermissionNode.builder(perm.LPPermString)
                .value(perm.MCPermLevel == 0)
                .build()
        }
    }

    override fun hasPermission(player: ServerPlayerEntity, perm: CKPermissions): Boolean {
        val data = playerAdapter.getPermissionData(player)

        return data.checkPermission(perm.LPPermString).asBoolean()
    }
}