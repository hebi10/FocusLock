package com.haebi.focuslock.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build

/** Only the requested permissions exposed for packages visible under Android package visibility. */
data class AppPermission(val name: String, val status: String)

sealed interface PermissionLookup {
    data class Found(val permissions: List<AppPermission>) : PermissionLookup
    data object NotVisible : PermissionLookup
}

class AppPermissionRepository(private val context: Context) {
    fun inspect(packageName: String?): PermissionLookup {
        if (packageName.isNullOrEmpty()) return PermissionLookup.NotVisible
        val pm = context.packageManager
        return try {
            val info = if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }
            val permissions = info.requestedPermissions ?: emptyArray()
            val flags = info.requestedPermissionsFlags ?: IntArray(permissions.size)
            val visible = permissions.mapIndexedNotNull { index, permission ->
                val permissionInfo = try { pm.getPermissionInfo(permission, 0) }
                    catch (_: PackageManager.NameNotFoundException) { null }
                val label = permissionLabel(permission)
                    ?: permissionInfo?.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() }
                    ?: permission.substringAfterLast('.')
                val protection = permissionInfo?.protectionLevel?.and(PermissionInfo.PROTECTION_MASK_BASE) ?: -1
                val status = if (protection == PermissionInfo.PROTECTION_DANGEROUS && index < flags.size) {
                    if (flags[index] and PackageInfoFlag.GRANTED != 0) "허용" else "미허용"
                } else "Android에서 확인"
                AppPermission(label, status)
            }.distinctBy { it.name }.sortedBy { if (it.status == "Android에서 확인") 1 else 0 }
            PermissionLookup.Found(visible)
        } catch (_: PackageManager.NameNotFoundException) {
            PermissionLookup.NotVisible
        } catch (_: SecurityException) {
            PermissionLookup.NotVisible
        }
    }

    private object PackageInfoFlag {
        const val GRANTED = android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED
    }

    private fun permissionLabel(name: String): String? = when (name) {
        Manifest.permission.CAMERA -> "카메라"
        Manifest.permission.RECORD_AUDIO -> "마이크"
        Manifest.permission.ACCESS_FINE_LOCATION -> "정확한 위치"
        Manifest.permission.ACCESS_COARSE_LOCATION -> "대략적인 위치"
        Manifest.permission.READ_CONTACTS -> "연락처 읽기"
        Manifest.permission.WRITE_CONTACTS -> "연락처 수정"
        Manifest.permission.READ_CALENDAR -> "캘린더 읽기"
        Manifest.permission.WRITE_CALENDAR -> "캘린더 수정"
        Manifest.permission.POST_NOTIFICATIONS -> "알림"
        else -> null
    }
}
