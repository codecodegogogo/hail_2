package com.aistra.hail.ui.settings

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ManageSearch
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.DialogInputBinding
import com.aistra.hail.ui.main.MainActivity
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.compose.preference.*
import rikka.shizuku.Shizuku

class SettingsFragment : MainFragment(), MenuProvider {
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val menuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    ProvidePreferenceLocals {
                        SettingsScreen()
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsScreen() {
        val autoFreezeAfterLock = rememberPreferenceState(HailData.AUTO_FREEZE_AFTER_LOCK, false)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            listPreference(
                key = HailData.WORKING_MODE,
                defaultValue = HailData.MODE_DEFAULT,
                onValueChange = ::onWorkingModeChange,
                values = HailData.WORKING_MODE_VALUES,
                entriesId = R.array.working_mode_entries,
                titleId = R.string.working_mode,
                icon = Icons.Outlined.Adb,
                type = ListPreferenceType.ALERT_DIALOG
            )
            switchPreference(
                key = HailData.BIOMETRIC_LOGIN,
                defaultValue = false,
                titleId = R.string.action_biometric,
                icon = Icons.Outlined.Fingerprint
            )
            horizontalDivider()
            preferenceCategory(key = "customize", title = { Text(text = stringResource(R.string.title_customize)) })
            switchPreference(
                key = HailData.CALENDAR_DISGUISE,
                defaultValue = true,
                onValueChange = { _, value ->
                    setCalendarDisguiseEnabled(value)
                    true
                },
                titleId = R.string.calendar_disguise,
                icon = null
            )
            listPreference(
                key = HailData.APP_THEME,
                defaultValue = HailData.FOLLOW_SYSTEM,
                onValueChange = { _, value ->
                    app.setAppTheme(value)
                    true
                },
                values = HailData.APP_THEME_VALUES,
                entriesId = R.array.app_theme_entries,
                titleId = R.string.app_theme,
                icon = Icons.Outlined.DarkMode
            )
            sliderPreference(
                key = HailData.HOME_FONT_SIZE,
                defaultValue = 14f,
                title = { Text(text = stringResource(R.string.home_font_size)) },
                valueRange = 11f..16f,
                valueSteps = 4,
                icon = { Icon(imageVector = Icons.Outlined.TextFields, contentDescription = null) },
                valueText = { Text(text = "%.0f".format(it)) },
            )
            switchPreference(
                key = HailData.FUZZY_SEARCH,
                defaultValue = false,
                titleId = R.string.fuzzy_search,
                icon = Icons.AutoMirrored.Outlined.ManageSearch
            )
            horizontalDivider()
            preferenceCategory(key = "auto_freeze", title = { Text(text = stringResource(R.string.auto_freeze)) })
            switchPreference(
                rememberState = { autoFreezeAfterLock },
                onValueChange = { _, value ->
                    app.setAutoFreezeService(value)
                    true
                },
                titleId = R.string.auto_freeze_after_lock,
                icon = Icons.Outlined.ScreenLockPortrait
            )
            sliderPreference(
                key = HailData.AUTO_FREEZE_DELAY,
                defaultValue = 0f,
                title = { Text(text = stringResource(R.string.auto_freeze_delay)) },
                valueRange = 0f..30f,
                valueSteps = 29,
                enabled = { autoFreezeAfterLock.value },
                icon = { Icon(imageVector = Icons.Outlined.LockClock, contentDescription = null) },
                valueText = { Text(text = "%.0f".format(it)) },
            )
            switchPreference(
                key = HailData.SKIP_WHILE_CHARGING,
                defaultValue = false,
                titleId = R.string.skip_while_charging,
                enabled = autoFreezeAfterLock.value,
                icon = Icons.Outlined.BatteryChargingFull
            )
            switchPreference(
                key = HailData.SKIP_FOREGROUND_APP,
                defaultValue = false,
                onValueChange = { _, value ->
                    if (value && !HSystem.checkOpUsageStats(requireContext())) {
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        false
                    } else true
                },
                titleId = R.string.skip_foreground_app,
                enabled = autoFreezeAfterLock.value,
                icon = Icons.Outlined.Android
            )
            switchPreference(
                key = HailData.SKIP_NOTIFYING_APP,
                defaultValue = false,
                onValueChange = { _, value ->
                    val isGranted = NotificationManagerCompat.getEnabledListenerPackages(requireContext())
                        .contains(requireContext().packageName)
                    if (value && !isGranted) {
                        app.setAutoFreezeServiceEnabled(true)
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        false
                    } else true
                },
                titleId = R.string.skip_notifying_app,
                enabled = autoFreezeAfterLock.value,
                icon = Icons.Outlined.NotificationsActive
            )
        }
    }

    private fun LazyListScope.horizontalDivider() = item { HorizontalDivider() }

    private fun LazyListScope.switchPreference(
        rememberState: @Composable () -> MutableState<Boolean>,
        onValueChange: (MutableState<Boolean>, Boolean) -> Boolean = { _, _ -> true },
        @StringRes titleId: Int,
        enabled: Boolean = true,
        icon: ImageVector?,
    ) = item(key = titleId, contentType = "SwitchPreference") {
        val state = rememberState()
        SwitchPreference(
            value = state.value,
            onValueChange = { if (onValueChange(state, it)) state.value = it },
            title = { Text(text = stringResource(titleId)) },
            enabled = enabled,
            icon = {
                if (icon != null) Icon(imageVector = icon, contentDescription = null)
                else Spacer(modifier = Modifier.size(24.dp))
            })
    }

    private fun LazyListScope.switchPreference(
        key: String,
        defaultValue: Boolean,
        onValueChange: (MutableState<Boolean>, Boolean) -> Boolean = { _, _ -> true },
        @StringRes titleId: Int,
        enabled: Boolean = true,
        icon: ImageVector?,
    ) = switchPreference(
        rememberState = { rememberPreferenceState(key, defaultValue) },
        onValueChange = onValueChange,
        titleId = titleId,
        enabled = enabled,
        icon = icon
    )

    private fun LazyListScope.listPreference(
        key: String,
        defaultValue: String,
        onValueChange: (MutableState<String>, String) -> Boolean = { _, _ -> true },
        values: List<String>,
        @StringRes titleId: Int,
        icon: ImageVector,
        summary: @Composable (String) -> String,
        type: ListPreferenceType = ListPreferenceType.DROPDOWN_MENU,
        valueToText: (String) -> String
    ) = item(key = key, contentType = "ListPreference") {
        val state = rememberPreferenceState(key, defaultValue)
        ListPreference(
            value = state.value,
            onValueChange = { if (onValueChange(state, it)) state.value = it },
            values = values,
            title = { Text(text = stringResource(titleId)) },
            icon = { Icon(imageVector = icon, contentDescription = null) },
            summary = { Text(text = summary(state.value)) },
            type = type,
            valueToText = { AnnotatedString(valueToText(it)) })
    }

    private fun LazyListScope.listPreference(
        key: String,
        defaultValue: String,
        onValueChange: (MutableState<String>, String) -> Boolean = { _, _ -> true },
        values: List<String>,
        @ArrayRes entriesId: Int,
        @StringRes titleId: Int,
        icon: ImageVector,
        type: ListPreferenceType = ListPreferenceType.DROPDOWN_MENU
    ) = listPreference(
        key = key,
        defaultValue = defaultValue,
        onValueChange = onValueChange,
        values = values,
        titleId = titleId,
        icon = icon,
        summary = { it.toEntry(values, entriesId) },
        type = type,
        valueToText = { it.toEntry(values, entriesId) })

    private fun String.toEntry(values: List<String>, @ArrayRes entriesId: Int): String =
        resources.getStringArray(entriesId)[values.indexOf(this)]

    private fun setCalendarDisguiseEnabled(enabled: Boolean) {
        val packageManager = requireContext().packageManager
        val calendarLauncher = ComponentName(
            requireContext(), "com.aistra.hail.ui.calendar.CalendarLauncher"
        )
        val hailLauncher = ComponentName(
            requireContext(), "com.aistra.hail.ui.main.HailLauncher"
        )
        val launcherToEnable = if (enabled) calendarLauncher else hailLauncher
        val launcherToDisable = if (enabled) hailLauncher else calendarLauncher
        packageManager.setComponentEnabledSetting(
            launcherToEnable,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        packageManager.setComponentEnabledSetting(
            launcherToDisable,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    fun onWorkingModeChange(rememberState: MutableState<String>, mode: String): Boolean {
        // Show/hide terminal menu.
        activity.invalidateOptionsMenu()
        when {
            mode.startsWith(HailData.OWNER) -> if (!HPolicy.isDeviceOwnerActive) {
                MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.title_set_owner)
                    .setMessage(getString(R.string.msg_set_owner, HPolicy.ADB_COMMAND))
                    .setPositiveButton(android.R.string.ok, null)
                    .setNeutralButton(android.R.string.copy) { _, _ -> HUI.copyText(HPolicy.ADB_COMMAND) }.show()
                    .findViewById<MaterialTextView>(android.R.id.message)?.setTextIsSelectable(true)
                return false
            }

            mode.startsWith(HailData.DHIZUKU) -> return runCatching {
                Dhizuku.init(app)
                when {
                    Dhizuku.isPermissionGranted() -> true
                    else -> {
                        lifecycleScope.launch {
                            val result = callbackFlow {
                                Dhizuku.requestPermission(object : DhizukuRequestPermissionListener() {
                                    override fun onRequestPermission(grantResult: Int) {
                                        trySendBlocking(grantResult == PackageManager.PERMISSION_GRANTED)
                                    }
                                })
                                awaitClose()
                            }.first()
                            if (result) {
                                rememberState.value = mode
                                if (HTarget.O) HDhizuku.setDelegatedScopes()
                            }
                        }
                        false
                    }
                }
            }.getOrElse {
                HLog.e(it)
                HUI.showToast(R.string.permission_denied)
                false
            }

            mode.startsWith(HailData.SU) -> if (!HShell.checkSU) {
                HUI.showToast(R.string.permission_denied)
                return false
            }

            mode.startsWith(HailData.SHIZUKU) -> return runCatching {
                when {
                    Shizuku.isPreV11() -> throw IllegalStateException("unsupported shizuku version")
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> true
                    Shizuku.shouldShowRequestPermissionRationale() -> {
                        HUI.showToast(R.string.permission_denied)
                        false
                    }

                    else -> {
                        lifecycleScope.launch {
                            val result = callbackFlow {
                                val shizukuRequestCode = 0
                                val listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
                                    if (requestCode != shizukuRequestCode) return@OnRequestPermissionResultListener
                                    trySendBlocking(grantResult == PackageManager.PERMISSION_GRANTED)
                                }
                                Shizuku.addRequestPermissionResultListener(listener)
                                Shizuku.requestPermission(shizukuRequestCode)
                                awaitClose {
                                    Shizuku.removeRequestPermissionResultListener(listener)
                                }
                            }.first()
                            if (result) rememberState.value = mode
                        }
                        false
                    }
                }
            }.getOrElse {
                HLog.e(it)
                HUI.showToast(R.string.shizuku_missing)
                false
            }

            mode.startsWith(HailData.ISLAND) -> return runCatching {
                when {
                    mode == HailData.MODE_ISLAND_HIDE && HIsland.freezePermissionGranted() -> true
                    mode == HailData.MODE_ISLAND_SUSPEND && HIsland.suspendPermissionGranted() -> true
                    else -> {
                        lifecycleScope.launch {
                            requestPermissionLauncher.launch(
                                if (mode == HailData.MODE_ISLAND_HIDE) HIsland.PERMISSION_FREEZE_PACKAGE
                                else HIsland.PERMISSION_SUSPEND_PACKAGE
                            )
                        }
                        false
                    }
                }
            }.getOrElse {
                HLog.e(it)
                HUI.showToast(R.string.permission_denied)
                false
            }.also {
                if (it) {
                    HIsland.checkOwnerApp()
                }
            }

            mode.startsWith(HailData.PRIVAPP) -> if (!HPackages.isPrivilegedApp(app.packageName)) {
                HUI.showToast(R.string.permission_denied)
                return false
            }
        }

        return true
    }

    private suspend fun onTerminalResult(exitValue: Int, msg: String?) = withContext(Dispatchers.Main) {
        if (exitValue == 0 && msg.isNullOrBlank()) return@withContext
        MaterialAlertDialogBuilder(requireActivity()).apply {
            if (!msg.isNullOrBlank()) {
                if (exitValue != 0) {
                    setTitle(getString(R.string.operation_failed, exitValue.toString()))
                }
                setMessage(msg)
                setNeutralButton(android.R.string.copy) { _, _ -> HUI.copyText(msg) }
            } else if (exitValue != 0) {
                setMessage(getString(R.string.operation_failed, exitValue.toString()))
            }
        }.setPositiveButton(android.R.string.ok, null).show().findViewById<MaterialTextView>(android.R.id.message)
            ?.setTextIsSelectable(true)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_terminal -> showTerminalDialog()
            R.id.action_remove_owner -> (requireActivity() as MainActivity).ownerRemoveDialog()
            R.id.action_help -> HUI.openLink(HailData.URL_README)
        }
        return false
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_settings, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        if (HailData.workingMode.startsWith(HailData.SU) || HailData.workingMode.startsWith(
                HailData.SHIZUKU
            )
        ) menu.findItem(R.id.action_terminal).isVisible = true
        else if (HPolicy.isDeviceOwnerActive) menu.findItem(R.id.action_remove_owner).isVisible = true
    }

    private fun showTerminalDialog() {
        val binding = DialogInputBinding.inflate(layoutInflater)
        binding.inputLayout.setHint(R.string.command)
        binding.editText.run {
            setSingleLine()
            filters = arrayOf()
        }
        MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.action_terminal).setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                lifecycleScope.launch {
                    val result = AppManager.execute(binding.editText.text.toString())
                    onTerminalResult(result.first, result.second)
                }
            }.setNegativeButton(android.R.string.cancel, null).show()
    }
}
