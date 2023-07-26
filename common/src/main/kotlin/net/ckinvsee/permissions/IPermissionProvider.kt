package net.ckinvsee.permissions

import net.minecraft.server.network.ServerPlayerEntity

interface IPermissionProvider {
    fun hasPermission(player: ServerPlayerEntity, perm: CKPermissions): Boolean
}