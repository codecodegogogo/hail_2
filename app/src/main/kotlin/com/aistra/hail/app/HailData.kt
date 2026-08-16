package com.aistra.hail.app

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.aistra.hail.BuildConfig
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.utils.HFiles
import org.json.JSONArray
import org.json.JSONObject

object HailData {
    const val URL_WHY_FREE_SOFTWARE = "https://www.gnu.org/philosophy/free-software-even-more-important.html"
    const val URL_GITHUB_HAIL_2 = "https://github.com/codecodegogogo/hail_2"
    const val URL_GITHUB = "https://github.com/aistra0528/Hail"
    const val URL_README = "$URL_GITHUB#readme"
    const val URL_RELEASES = "$URL_GITHUB/releases"
    const val URL_TELEGRAM = "https://t.me/+yvRXYTounDIxODFl"
    const val URL_QQ = "http://qm.qq.com/cgi-bin/qm/qr?k=I2g_Ymanc6bQMo4cVKTG0knARE0twtSG"
    const val URL_FDROID = "https://f-droid.org/packages/${BuildConfig.APPLICATION_ID}"
    const val URL_ALIPAY = "https://qr.alipay.com/tsx02922ajwj6xekqyd1rbf"
    const val URL_ALIPAY_API = "alipays://platformapi/startapp?saId=10000007&qrcode=$URL_ALIPAY"
    const val URL_BILIBILI = "https://space.bilibili.com/9261272"
    const val URL_LIBERAPAY = "https://liberapay.com/aistra0528"
    const val URL_PAYPAL = "https://www.paypal.me/aistra0528"
    const val URL_TRANSLATE = "https://hosted.weblate.org/engage/hail/"
    const val VERSION = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    private const val KEY_ID = "id"
    const val KEY_TAG = "tag"
    private const val KEY_TAGS = "tags"
    private const val KEY_TAG_WORKING_ACTIONS = "tag_working_actions"
    private const val KEY_PINNED = "pinned"
    private const val KEY_WHITELISTED = "whitelisted"
    const val KEY_PACKAGE = "package"
    const val KEY_FROZEN = "frozen"
    private const val SORT_BY = "sort_by"
    const val SORT_NAME = "name"
    const val SORT_INSTALL = "install"
    const val SORT_UPDATE = "update"
    const val FILTER_USER_APPS = "filter_user_apps"
    const val FILTER_SYSTEM_APPS = "filter_system_apps"
    const val FILTER_FROZEN_APPS = "filter_frozen_apps"
    const val FILTER_UNFROZEN_APPS = "filter_unfrozen_apps"
    private const val ICONLESS_SORT_BY = "iconless_sort_by"
    const val ICONLESS_FILTER_USER_APPS = "iconless_filter_user_apps"
    const val ICONLESS_FILTER_SYSTEM_APPS = "iconless_filter_system_apps"
    const val ICONLESS_FILTER_HIDDEN_APPS = "iconless_filter_hidden_apps"
    const val ICONLESS_FILTER_VISIBLE_APPS = "iconless_filter_visible_apps"
    const val OWNER = "owner_"
    const val DHIZUKU = "dhizuku_"
    const val SU = "su_"
    const val SHIZUKU = "shizuku_"
    const val ISLAND = "island_"
    const val PRIVAPP = "privapp_"
    const val STOP = "stop"
    const val DISABLE = "disable"
    const val HIDE = "hide"
    const val SUSPEND = "suspend"
    const val WORKING_MODE = "working_mode"
    const val MODE_DEFAULT = "default"
    const val MODE_SHIZUKU_STOP = SHIZUKU + STOP
    const val MODE_SHIZUKU_DISABLE = SHIZUKU + DISABLE
    const val MODE_SHIZUKU_HIDE = SHIZUKU + HIDE
    const val MODE_SHIZUKU_SUSPEND = SHIZUKU + SUSPEND
    const val MODE_SU_STOP = SU + STOP
    const val MODE_SU_DISABLE = SU + DISABLE
    const val MODE_SU_HIDE = SU + HIDE
    const val MODE_SU_SUSPEND = SU + SUSPEND
    const val MODE_DHIZUKU_HIDE = DHIZUKU + HIDE
    const val MODE_DHIZUKU_SUSPEND = DHIZUKU + SUSPEND
    const val MODE_OWNER_HIDE = OWNER + HIDE
    const val MODE_OWNER_SUSPEND = OWNER + SUSPEND
    const val MODE_ISLAND_HIDE = ISLAND + HIDE
    const val MODE_ISLAND_SUSPEND = ISLAND + SUSPEND
    const val MODE_PRIVAPP_STOP = PRIVAPP + STOP
    const val MODE_PRIVAPP_DISABLE = PRIVAPP + DISABLE
    // The stored value remains permission + action so existing installs and execution dispatch stay compatible.
    val WORKING_PERMISSION_VALUES = listOf(MODE_DEFAULT, SHIZUKU, SU, DHIZUKU, OWNER, ISLAND, PRIVAPP)
    private val WORKING_ACTION_VALUES = listOf(STOP, DISABLE, HIDE, SUSPEND)
    const val BIOMETRIC_LOGIN = "biometric_login"
    const val APP_THEME = "app_theme"
    const val FOLLOW_SYSTEM = "follow_system"
    const val THEME_LIGHT = "theme_light"
    const val THEME_DARK = "theme_dark"
    val APP_THEME_VALUES = listOf(FOLLOW_SYSTEM, THEME_LIGHT, THEME_DARK)
    const val CALENDAR_DISGUISE = "calendar_disguise"
    const val ICONLESS_LAUNCHER_ENABLED = "iconless_launcher_enabled"
    const val HOME_FONT_SIZE = "home_font_size_f"
    const val FUZZY_SEARCH = "fuzzy_search"
    const val ACTION_LOCK = "lock"
    const val AUTO_FREEZE_AFTER_LOCK = "auto_freeze_after_lock"
    const val AUTO_FREEZE_DELAY = "auto_freeze_delay_f"
    const val SKIP_WHILE_CHARGING = "skip_while_charging"
    const val SKIP_FOREGROUND_APP = "skip_foreground_app"
    const val SKIP_NOTIFYING_APP = "skip_notifying_app"

    private val sp = PreferenceManager.getDefaultSharedPreferences(app)
    val sortBy get() = sp.getString(SORT_BY, SORT_NAME)
    val filterUserApps get() = sp.getBoolean(FILTER_USER_APPS, true)
    val filterSystemApps get() = sp.getBoolean(FILTER_SYSTEM_APPS, false)
    val filterFrozenApps get() = sp.getBoolean(FILTER_FROZEN_APPS, true)
    val filterUnfrozenApps get() = sp.getBoolean(FILTER_UNFROZEN_APPS, true)
    val iconlessSortBy get() = sp.getString(ICONLESS_SORT_BY, SORT_NAME)!!
    val iconlessFilterUserApps get() = sp.getBoolean(ICONLESS_FILTER_USER_APPS, true)
    val iconlessFilterSystemApps get() = sp.getBoolean(ICONLESS_FILTER_SYSTEM_APPS, false)
    val iconlessFilterHiddenApps get() = sp.getBoolean(ICONLESS_FILTER_HIDDEN_APPS, true)
    val iconlessFilterVisibleApps get() = sp.getBoolean(ICONLESS_FILTER_VISIBLE_APPS, true)
    val workingMode get() = sp.getString(WORKING_MODE, MODE_DEFAULT)!!
    val biometricLogin get() = sp.getBoolean(BIOMETRIC_LOGIN, false)
    val calendarDisguise get() = sp.getBoolean(CALENDAR_DISGUISE, true)
    val iconlessLauncherEnabled get() = sp.getBoolean(ICONLESS_LAUNCHER_ENABLED, false)
    val appTheme get() = sp.getString(APP_THEME, FOLLOW_SYSTEM)!!
    val homeFontSize get() = sp.getFloat(HOME_FONT_SIZE, 14f)
    val fuzzySearch get() = sp.getBoolean(FUZZY_SEARCH, false)
    var autoFreezeAfterLock
        get() = sp.getBoolean(AUTO_FREEZE_AFTER_LOCK, false)
        set(value) = sp.edit { putBoolean(AUTO_FREEZE_AFTER_LOCK, value) }
    val autoFreezeDelay get() = sp.getFloat(AUTO_FREEZE_DELAY, 0f).toLong()
    val skipWhileCharging get() = sp.getBoolean(SKIP_WHILE_CHARGING, false)
    val skipForegroundApp get() = sp.getBoolean(SKIP_FOREGROUND_APP, false)
    val skipNotifyingApp get() = sp.getBoolean(SKIP_NOTIFYING_APP, false)

    fun workingPermission(mode: String): String =
        WORKING_PERMISSION_VALUES.drop(1).firstOrNull { mode.startsWith(it) } ?: MODE_DEFAULT

    fun supportedWorkingActions(permission: String): List<String> = when (permission) {
        SHIZUKU, SU -> WORKING_ACTION_VALUES
        DHIZUKU, OWNER, ISLAND -> listOf(HIDE, SUSPEND)
        PRIVAPP -> listOf(STOP, DISABLE)
        else -> emptyList()
    }

    fun workingAction(mode: String): String {
        val actions = supportedWorkingActions(workingPermission(mode))
        return actions.firstOrNull { mode.endsWith(it) } ?: MODE_DEFAULT
    }

    fun combineWorkingMode(permission: String, preferredAction: String): String {
        val actions = supportedWorkingActions(permission)
        if (actions.isEmpty()) return MODE_DEFAULT
        val action = preferredAction.takeIf { it in actions } ?: actions.first()
        return permission + action
    }

    private val dir = "${app.filesDir.path}/v1"
    private val appsPath = "$dir/apps.json"
    private val tagsPath = "$dir/tags.json"
    private val tagWorkingActionsPath = "$dir/$KEY_TAG_WORKING_ACTIONS.json"
    private val iconlessLauncherPath = "$dir/iconless_launcher.json"

    val checkedList: MutableList<AppInfo> by lazy {
        mutableListOf<AppInfo>().apply {
            runCatching {
                val json = JSONArray(HFiles.read(appsPath))
                for (i in 0 until json.length()) {
                    add(with(json.getJSONObject(i)) {
                        AppInfo(
                            packageName = getString(KEY_PACKAGE),
                            pinned = optBoolean(KEY_PINNED),
                            whitelisted = optBoolean(KEY_WHITELISTED),
                            tagIdList = optJSONArray(KEY_TAGS)?.let {
                                MutableList(it.length()) { index -> it.getInt(index) }
                            } ?: mutableListOf(optInt(KEY_TAG))
                        )
                    })
                }
            }
        }
    }

    fun isChecked(packageName: String): Boolean = checkedList.any { it.packageName == packageName }

    fun addCheckedApp(packageName: String, tagId: Int = 0, saveApps: Boolean = true) {
        checkedList.add(AppInfo(packageName, tagIdList = mutableListOf(tagId)))
        if (saveApps) saveApps()
    }

    fun removeCheckedApp(packageName: String, saveApps: Boolean = true) {
        checkedList.removeAll { it.packageName == packageName }
        if (saveApps) saveApps()
    }

    fun saveApps() {
        if (!HFiles.exists(dir)) HFiles.createDirectories(dir)
        HFiles.write(appsPath, JSONArray().run {
            checkedList.forEach {
                put(
                    JSONObject()
                        .put(KEY_PACKAGE, it.packageName)
                        .put(KEY_PINNED, it.pinned)
                        .put(KEY_WHITELISTED, it.whitelisted)
                        .put(KEY_TAGS, JSONArray(it.tagIdList))
                )
            }
            toString()
        })
    }

    val tags: MutableList<Pair<String, Int>> by lazy {
        mutableListOf<Pair<String, Int>>().apply {
            runCatching {
                val json = JSONArray(HFiles.read(tagsPath))
                for (i in 0 until json.length()) {
                    add(with(json.getJSONObject(i)) { getString(KEY_TAG) to getInt(KEY_ID) })
                }
            }.onFailure {
                add(app.getString(R.string.label_default) to 0)
            }
        }
    }

    fun saveTags() {
        if (!HFiles.exists(dir)) HFiles.createDirectories(dir)
        HFiles.write(tagsPath, JSONArray().run {
            tags.forEach {
                put(JSONObject().put(KEY_TAG, it.first).put(KEY_ID, it.second))
            }
            toString()
        })
    }

    private val tagWorkingActions: MutableMap<Int, String> by lazy {
        mutableMapOf<Int, String>().apply {
            runCatching {
                val json = JSONObject(HFiles.read(tagWorkingActionsPath))
                json.keys().forEach { tagId ->
                    val action = json.getString(tagId)
                    if (action in WORKING_ACTION_VALUES) put(tagId.toInt(), action)
                }
            }
        }
    }

    fun tagWorkingAction(tagId: Int): String? = tagWorkingActions[tagId]

    fun tagWorkingMode(tagId: Int): String {
        val permission = workingPermission(workingMode)
        val action = tagWorkingAction(tagId)
        return if (action != null && action in supportedWorkingActions(permission)) {
            permission + action
        } else workingMode
    }

    fun appWorkingModeTag(appInfo: AppInfo): Pair<Int?, String> {
        val permission = workingPermission(workingMode)
        val supportedActions = supportedWorkingActions(permission)
        val tagAndAction = tags.asSequence().mapNotNull { tag ->
            val action = tagWorkingAction(tag.second)
            if (tag.second in appInfo.tagIdList && action != null && action in supportedActions) {
                tag to action
            } else null
        }.firstOrNull()
        return tagAndAction?.let { (tag, action) ->
            tag.second to permission + action
        } ?: (null to workingMode)
    }

    fun setTagWorkingAction(tagId: Int, action: String?) {
        if (action != null && action in WORKING_ACTION_VALUES) tagWorkingActions[tagId] = action
        else tagWorkingActions.remove(tagId)
        saveTagWorkingActions()
    }

    fun replaceTagWorkingAction(oldTagId: Int, newTagId: Int, action: String?) {
        tagWorkingActions.remove(oldTagId)
        if (action != null && action in WORKING_ACTION_VALUES) tagWorkingActions[newTagId] = action
        saveTagWorkingActions()
    }

    private fun saveTagWorkingActions() {
        if (!HFiles.exists(dir)) HFiles.createDirectories(dir)
        HFiles.write(tagWorkingActionsPath, JSONObject().run {
            tagWorkingActions.forEach { (tagId, action) -> put(tagId.toString(), action) }
            toString()
        })
    }

    fun changeAppsSort(sort: String) = sp.edit { putString(SORT_BY, sort) }

    fun changeAppsFilter(filter: String, enabled: Boolean) = sp.edit { putBoolean(filter, enabled) }

    fun changeIconlessAppsSort(sort: String) = sp.edit { putString(ICONLESS_SORT_BY, sort) }

    fun changeIconlessAppsFilter(filter: String, enabled: Boolean) = sp.edit { putBoolean(filter, enabled) }

    val iconlessLauncherEntries: MutableList<IconlessLauncherEntry> by lazy {
        mutableListOf<IconlessLauncherEntry>().apply {
            runCatching {
                val json = JSONArray(HFiles.read(iconlessLauncherPath))
                for (i in 0 until json.length()) {
                    val item = json.getJSONObject(i)
                    val components = item.optJSONArray("components") ?: JSONArray()
                    add(
                        IconlessLauncherEntry(
                            packageName = item.getString(KEY_PACKAGE),
                            components = List(components.length()) { index -> components.getString(index) },
                            hidden = item.optBoolean("hidden", true),
                            lastOperatedAt = item.optLong("last_operated_at", 0L)
                        )
                    )
                }
            }
        }
    }

    fun saveIconlessLauncherEntries() {
        if (!HFiles.exists(dir)) HFiles.createDirectories(dir)
        HFiles.write(iconlessLauncherPath, JSONArray().run {
            iconlessLauncherEntries.forEach {
                put(
                    JSONObject()
                        .put(KEY_PACKAGE, it.packageName)
                        .put("components", JSONArray(it.components))
                        .put("hidden", it.hidden)
                        .put("last_operated_at", it.lastOperatedAt)
                )
            }
            toString()
        })
    }
}

data class IconlessLauncherEntry(
    val packageName: String,
    val components: List<String>,
    val hidden: Boolean = true,
    val lastOperatedAt: Long = 0L
)
