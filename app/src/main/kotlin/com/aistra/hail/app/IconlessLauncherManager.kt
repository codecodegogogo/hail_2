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
import java.text.Collator

object IconlessLauncherManager {
    data class Candidate(
        val packageName: String,
        val label: String
    )

    fun entries(): List<IconlessLauncherEntry> = synchronized(HailData.iconlessLauncherEntries) {
        HailData.iconlessLauncherEntries.toList()
    }

    fun availableApps(): List<Candidate> {
        val managedPackages = entries().mapTo(mutableSetOf()) { it.packageName }
        return queryLauncherActivities().asSequence()
            .filter { it.activityInfo.packageName != BuildConfig.APPLICATION_ID }
            .filter { it.activityInfo.packageName !in managedPackages }
            .map {
                Candidate(
                    packageName = it.activityInfo.packageName,
                    label = it.loadLabel(app.packageManager).toString()
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(Comparator { left, right -> Collator.getInstance().compare(left.label, right.label) })
            .toList()
    }

    fun applicationInfo(entry: IconlessLauncherEntry): ApplicationInfo? =
        HPackages.getApplicationInfoOrNull(entry.packageName)

    fun setFeatureEnabled(enabled: Boolean): Boolean {
        val changed = mutableListOf<IconlessLauncherEntry>()
        for (entry in entries()) {
            if (applicationInfo(entry) == null) continue
            if (!setEntryComponentsEnabled(entry, !enabled)) {
                changed.asReversed().forEach { setEntryComponentsEnabled(it, enabled) }
                return false
            }
            changed += entry
        }
        return true
    }

    fun add(packageName: String): Boolean {
        if (packageName == BuildConfig.APPLICATION_ID || entries().any { it.packageName == packageName }) return false
        val components = queryLauncherActivities(packageName)
            .map { ComponentName(it.activityInfo.packageName, it.activityInfo.name).flattenToString() }
            .distinct()
        if (components.isEmpty()) return false
        val entry = IconlessLauncherEntry(packageName, components)
        if (!setEntryComponentsEnabled(entry, false)) return false
        synchronized(HailData.iconlessLauncherEntries) {
            HailData.iconlessLauncherEntries += entry
            HailData.saveIconlessLauncherEntries()
        }
        return true
    }

    fun remove(entry: IconlessLauncherEntry): Boolean {
        if (applicationInfo(entry) != null && !setEntryComponentsEnabled(entry, true)) return false
        synchronized(HailData.iconlessLauncherEntries) {
            HailData.iconlessLauncherEntries.removeAll { it.packageName == entry.packageName }
            HailData.saveIconlessLauncherEntries()
        }
        return true
    }

    suspend fun launch(context: Context, entry: IconlessLauncherEntry): Boolean {
        if (applicationInfo(entry) == null || !setEntryComponentsEnabled(entry, true)) return false
        val component = entry.components.firstNotNullOfOrNull { ComponentName.unflattenFromString(it) }
        val launched = component != null && runCatching {
            context.startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(component)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.isSuccess
        delay(1200)
        val hiddenAgain = !HailData.iconlessLauncherEnabled || setEntryComponentsEnabled(entry, false)
        return launched && hiddenAgain
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

    private fun queryLauncherActivities(packageName: String? = null) = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .apply { if (packageName != null) setPackage(packageName) }
        .let {
            if (HTarget.T) app.packageManager.queryIntentActivities(
                it, PackageManager.ResolveInfoFlags.of(0)
            ) else app.packageManager.queryIntentActivities(it, 0)
        }
}
