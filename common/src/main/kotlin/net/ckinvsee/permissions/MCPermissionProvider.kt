package net.ckinvsee.permissions

import net.minecraft.server.network.ServerPlayerEntity

class MCPermissionProvider : IPermissionProvider {
    override fun hasPermission(player: ServerPlayerEntity, perm: CKPermissions): Boolean {
        return player.hasPermissionLevel(perm.MCPermLevel)
    }

}