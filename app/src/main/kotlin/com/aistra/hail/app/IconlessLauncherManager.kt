package com.aistra.hail.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.aistra.hail.BuildConfig
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HShell
import com.aistra.hail.utils.HShizuku
import com.aistra.hail.utils.HTarget
import kotlinx.coroutines.delay

data class IconlessLauncherApp(
    val packageName: String,
    val label: String,
    val applicationInfo: ApplicationInfo,
    val components: List<String>,
    val hidden: Boolean,
    val operated: Boolean,
    val lastOperatedAt: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long
)

object IconlessLauncherManager {
    fun apps(): List<IconlessLauncherApp> {
        val queriedComponents = queryLauncherActivities()
            .groupBy { it.activityInfo.packageName }
            .mapValues { (_, activities) ->
                activities.map {
                    ComponentName(it.activityInfo.packageName, it.activityInfo.name).flattenToString()
                }.distinct()
            }
        val records = entries().associateBy { it.packageName }
        val packageNames = (queriedComponents.keys + records.keys).asSequence()
            .filter { it != BuildConfig.APPLICATION_ID }
            .distinct()

        return packageNames.mapNotNull { packageName ->
            val info = HPackages.getApplicationInfoOrNull(packageName)
                ?.takeIf { it.flags and ApplicationInfo.FLAG_INSTALLED != 0 }
                ?: return@mapNotNull null
            val record = records[packageName]
            val components = (record?.components.orEmpty() + queriedComponents[packageName].orEmpty()).distinct()
            if (components.isEmpty()) return@mapNotNull null
            val packageInfo = HPackages.getUnhiddenPackageInfoOrNull(packageName)
            IconlessLauncherApp(
                packageName = packageName,
                label = info.loadLabel(app.packageManager).toString(),
                applicationInfo = info,
                components = components,
                hidden = HailData.iconlessLauncherEnabled && record?.hidden == true,
                operated = record?.hidden == true,
                lastOperatedAt = record?.lastOperatedAt ?: 0L,
                firstInstallTime = packageInfo?.firstInstallTime ?: 0L,
                lastUpdateTime = packageInfo?.lastUpdateTime ?: 0L
            )
        }.toList()
    }

    fun setFeatureEnabled(enabled: Boolean): Boolean {
        val changed = mutableListOf<IconlessLauncherEntry>()
        for (entry in entries().filter { it.hidden }) {
            if (HPackages.getApplicationInfoOrNull(entry.packageName) == null) continue
            if (!setEntryComponentsEnabled(entry, !enabled)) {
                changed.asReversed().forEach { setEntryComponentsEnabled(it, enabled) }
                return false
            }
            changed += entry
        }
        return true
    }

    fun setIconHidden(appInfo: IconlessLauncherApp, hidden: Boolean): Boolean {
        if (!HailData.iconlessLauncherEnabled) return false
        val existing = entry(appInfo.packageName)
        val components = appInfo.components.ifEmpty { existing?.components.orEmpty() }
        if (components.isEmpty()) return false
        val updated = IconlessLauncherEntry(
            packageName = appInfo.packageName,
            components = components,
            hidden = hidden,
            lastOperatedAt = System.currentTimeMillis()
        )
        if (!setEntryComponentsEnabled(updated, !hidden)) return false
        synchronized(HailData.iconlessLauncherEntries) {
            HailData.iconlessLauncherEntries.removeAll { it.packageName == updated.packageName }
            HailData.iconlessLauncherEntries += updated
            HailData.saveIconlessLauncherEntries()
        }
        return true
    }

    suspend fun launch(context: Context, appInfo: IconlessLauncherApp): Boolean {
        val record = entry(appInfo.packageName)
        val temporarilyVisible = HailData.iconlessLauncherEnabled && record?.hidden == true
        if (temporarilyVisible && !setEntryComponentsEnabled(record, true)) return false
        if (temporarilyVisible) delay(150L)

        val components = record?.components.orEmpty().ifEmpty { appInfo.components }
        val fallbackComponent = components.firstNotNullOfOrNull { ComponentName.unflattenFromString(it) }
        val launchIntent = app.packageManager.getLaunchIntentForPackage(appInfo.packageName)
            ?: fallbackComponent?.let { Intent.makeMainActivity(it) }
        val launched = launchIntent != null && runCatching {
            context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess

        if (temporarilyVisible) {
            delay(1200L)
            if (HailData.iconlessLauncherEnabled && entry(appInfo.packageName)?.hidden == true) {
                setEntryComponentsEnabled(record, false)
            }
        }
        return launched
    }

    private fun entries(): List<IconlessLauncherEntry> = synchronized(HailData.iconlessLauncherEntries) {
        HailData.iconlessLauncherEntries.toList()
    }

    private fun entry(packageName: String): IconlessLauncherEntry? =
        synchronized(HailData.iconlessLauncherEntries) {
            HailData.iconlessLauncherEntries.firstOrNull { it.packageName == packageName }
        }

    private fun setEntryComponentsEnabled(entry: IconlessLauncherEntry, enabled: Boolean): Boolean {
        val changed = mutableListOf<ComponentName>()
        for (flattenedComponent in entry.components) {
            val component = ComponentName.unflattenFromString(flattenedComponent) ?: continue
            if (!setComponentEnabled(component, enabled)) {
                changed.asReversed().forEach { setComponentEnabled(it, !enabled) }
                return false
            }
            changed += component
        }
        return changed.isNotEmpty()
    }

    private fun setComponentEnabled(componentName: ComponentName, enabled: Boolean): Boolean = when {
        HailData.workingMode.startsWith(HailData.SU) -> HShell.setComponentEnabled(componentName, enabled)
        HailData.workingMode.startsWith(HailData.SHIZUKU) -> HShizuku.setComponentEnabled(componentName, enabled)
        HailData.workingMode.startsWith(HailData.PRIVAPP) -> HPackages.setComponentEnabled(componentName, enabled)
        else -> false
    }

    private fun queryLauncherActivities() = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .let {
            val flags = PackageManager.MATCH_DISABLED_COMPONENTS
            if (HTarget.T) app.packageManager.queryIntentActivities(
                it, PackageManager.ResolveInfoFlags.of(flags.toLong())
            ) else app.packageManager.queryIntentActivities(it, flags)
        }
}
