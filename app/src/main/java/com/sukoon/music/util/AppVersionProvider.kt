package com.sukoon.music.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppVersionProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getInstalledVersionCode(): Int {
        return runCatching {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            PackageInfoCompat.getLongVersionCode(packageInfo)
                .coerceIn(1L, Int.MAX_VALUE.toLong())
                .toInt()
        }.getOrElse { error ->
            DevLogger.e("AppVersionProvider", "Failed to read installed version code", error)
            1
        }
    }
}
