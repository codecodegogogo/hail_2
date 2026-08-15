package com.aistra.hail.ui.home

import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.ArrayAdapter
import androidx.appcompat.widget.SearchView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.DialogInputBinding
import com.aistra.hail.databinding.FragmentPagerBinding
import com.aistra.hail.extensions.*
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.*
import com.aistra.hail.work.HWork
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class PagerFragment : MainFragment(), PagerAdapter.OnItemClickListener, PagerAdapter.OnItemLongClickListener,
    MenuProvider {
    private var query: String = String()
    private var _binding: FragmentPagerBinding? = null
    private val binding get() = _binding!!
    private lateinit var pagerAdapter: PagerAdapter
    private var multiselect: Boolean
        set(value) {
            (parentFragment as HomeFragment).multiselect = value
        }
        get() = (parentFragment as HomeFragment).multiselect
    private val selectedList get() = (parentFragment as HomeFragment).selectedList
    private val tabs: TabLayout get() = (parentFragment as HomeFragment).binding.tabs
    private val adapter get() = (parentFragment as HomeFragment).binding.pager.adapter as HomeAdapter
    private val tag: Pair<String, Int> get() = HailData.tags[tabs.selectedTabPosition]
    private val tagWorkingMode: String get() = HailData.tagWorkingMode(tag.second)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val menuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        _binding = FragmentPagerBinding.inflate(inflater, container, false)
        pagerAdapter = PagerAdapter(selectedList).apply {
            onItemClickListener = this@PagerFragment
            onItemLongClickListener = this@PagerFragment
        }
        binding.recyclerView.run {
            layoutManager = GridLayoutManager(
                activity, resources.getInteger(R.integer.home_span)
            )
            adapter = pagerAdapter
            applyDefaultInsetter { paddingRelative(isRtl, bottom = isLandscape) }

        }

        binding.refresh.apply {
            setOnRefreshListener {
                updateCurrentList()
                binding.refresh.isRefreshing = false
            }
            applyDefaultInsetter { marginRelative(isRtl, start = !isLandscape, end = true) }
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        updateCurrentList()
        updateBarTitle()
        activity.appbar.setLiftOnScrollTargetView(binding.recyclerView)
        tabs.getTabAt(tabs.selectedTabPosition)?.view?.setOnLongClickListener {
            if (isResumed) showTagDialog()
            true
        }
        activity.fab.setOnClickListener { activity.toggleFabMenu() }
        activity.fabMenu.findViewById<View>(R.id.fab_action_freeze_current).setOnClickListener {
            activity.closeFabMenu()
            setListFrozen(
                true, pagerAdapter.currentList.filterNot { it.whitelisted }, workingMode = tagWorkingMode
            )
        }
        activity.fabMenu.findViewById<View>(R.id.fab_action_unfreeze_current).setOnClickListener {
            activity.closeFabMenu()
            setListFrozen(false, pagerAdapter.currentList, workingMode = tagWorkingMode)
        }
    }

    private fun updateCurrentList() = HailData.checkedList.filter {
        if (query.isEmpty()) tag.second in it.tagIdList
        else (FuzzySearch.search(it.packageName, query) || FuzzySearch.search(
            it.name.toString(), query
        ) || PinyinSearch.searchPinyinAll(it.name.toString(), query))
    }.sortedWith(NameComparator).let {
        binding.empty.isVisible = it.isEmpty()
        pagerAdapter.workingMode = tagWorkingMode
        pagerAdapter.submitList(it)
        app.setAutoFreezeService()
    }

    private fun updateBarTitle() {
        activity.supportActionBar?.title =
            if (multiselect) getString(R.string.msg_selected, selectedList.size.toString())
            else getString(R.string.app_name)
    }

    override fun onItemClick(info: AppInfo) {
        if (multiselect) {
            if (info in selectedList) selectedList.remove(info)
            else selectedList.add(info)
            updateCurrentList()
            updateBarTitle()
            return
        }
        if (info.applicationInfo == null) {
            Snackbar.make(binding.root, R.string.app_not_installed, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_remove_home) { removeCheckedApp(info.packageName) }.show()
            return
        }
        launchApp(info.packageName)
    }

    override fun onItemLongClick(info: AppInfo): Boolean {
        if (info.applicationInfo == null && (!multiselect || info !in selectedList)) {
            exportToClipboard(listOf(info))
            return true
        }
        if (info in selectedList) {
            onMultiSelect()
            return true
        }
        val pkg = info.packageName
        val frozen = AppManager.isAppFrozen(pkg, tagWorkingMode)
        val action = getString(if (frozen) R.string.action_unfreeze else R.string.action_freeze)
        MaterialAlertDialogBuilder(activity).setTitle(info.name).setItems(
            resources.getStringArray(R.array.home_action_entries).filter {
                (it != getString(R.string.action_freeze) || !frozen) && (it != getString(R.string.action_unfreeze) || frozen) && (it != getString(
                    R.string.action_pin
                ) || !info.pinned) && (it != getString(R.string.action_unpin) || info.pinned) && (it != getString(
                    R.string.action_whitelist
                ) || !info.whitelisted) && (it != getString(R.string.action_remove_whitelist) || info.whitelisted) && (it != getString(
                    R.string.action_unfreeze_remove_home
                ) || frozen)
            }.toTypedArray()
        ) { _, which ->
            when (which) {
                0 -> launchApp(pkg)
                1 -> setListFrozen(!frozen, listOf(info), workingMode = tagWorkingMode)
                2 -> {
                    val values = resources.getIntArray(R.array.deferred_task_values)
                    val entries = arrayOfNulls<String>(values.size)
                    values.forEachIndexed { i, it ->
                        entries[i] = resources.getQuantityString(R.plurals.deferred_task_entry, it, it)
                    }
                    MaterialAlertDialogBuilder(activity).setTitle(R.string.action_deferred_task)
                        .setItems(entries) { _, i ->
                            HWork.setDeferredFrozen(
                                pkg, !frozen, values[i].toLong(), tagWorkingMode
                            )
                            Snackbar.make(
                                binding.root, resources.getQuantityString(
                                    R.plurals.msg_deferred_task, values[i], values[i], action, info.name
                                ), Snackbar.LENGTH_INDEFINITE
                            ).setAction(R.string.action_undo) { HWork.cancelWork(pkg) }.show()
                        }.setNegativeButton(android.R.string.cancel, null).show()
                }

                3 -> {
                    info.pinned = !info.pinned
                    HailData.saveApps()
                    updateCurrentList()
                }

                4 -> {
                    info.whitelisted = !info.whitelisted
                    HailData.saveApps()
                    updateCurrentList()
                }

                5 -> tagDialog(info)

                6 -> exportToClipboard(listOf(info))
                7 -> removeCheckedApp(pkg)
                8 -> {
                    setListFrozen(false, listOf(info), false, tagWorkingMode)
                    if (!AppManager.isAppFrozen(pkg, tagWorkingMode)) removeCheckedApp(pkg)
                }
            }
        }.setNeutralButton(R.string.action_details) { _, _ ->
            HUI.startActivity(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS, HPackages.packageUri(pkg)
            )
        }.setNegativeButton(android.R.string.cancel, null).show()
        return true
    }

    private fun tagDialog(info: AppInfo) {
        val checkedItems = BooleanArray(HailData.tags.size) { index ->
            HailData.tags[index].second in info.tagIdList
        }
        MaterialAlertDialogBuilder(activity).setTitle(R.string.action_tag_set).setMultiChoiceItems(
            HailData.tags.map { it.first }.toTypedArray(), checkedItems
        ) { _, index, isChecked ->
            checkedItems[index] = isChecked
        }.setPositiveButton(android.R.string.ok) { _, _ ->
            info.tagIdList.clear()
            checkedItems.forEachIndexed { index, checked ->
                if (checked) info.tagIdList.add(HailData.tags[index].second)
            }
            if (info.tagIdList.isEmpty()) {
                removeCheckedApp(info.packageName, false)
            }
            HailData.saveApps()
            updateCurrentList()
        }.setNeutralButton(R.string.action_tag_add) { _, _ ->
            showTagDialog(listOf(info))
        }.setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun deselect(update: Boolean = true) {
        selectedList.clear()
        if (!update) return
        updateCurrentList()
        updateBarTitle()
    }

    private fun onMultiSelect() {
        MaterialAlertDialogBuilder(activity).setTitle(
            getString(
                R.string.msg_selected, selectedList.size.toString()
            )
        ).setItems(
            intArrayOf(
                R.string.action_freeze,
                R.string.action_unfreeze,
                R.string.action_tag_set,
                R.string.action_export_clipboard,
                R.string.action_remove_home,
                R.string.action_unfreeze_remove_home
            ).map { getString(it) }.toTypedArray()
        ) { _, which ->
            when (which) {
                0 -> {
                    setListFrozen(true, selectedList, false, tagWorkingMode)
                    deselect()
                }

                1 -> {
                    setListFrozen(false, selectedList, false, tagWorkingMode)
                    deselect()
                }

                2 -> triStateTagDialog()

                3 -> {
                    exportToClipboard(selectedList)
                    deselect()
                }

                4 -> {
                    selectedList.forEach { removeCheckedApp(it.packageName, false) }
                    HailData.saveApps()
                    deselect()
                }

                5 -> {
                    setListFrozen(false, selectedList, false, tagWorkingMode)
                    selectedList.forEach {
                        if (!AppManager.isAppFrozen(it.packageName, tagWorkingMode)) {
                            removeCheckedApp(it.packageName, false)
                        }
                    }
                    HailData.saveApps()
                    deselect()
                }
            }
        }.setNegativeButton(R.string.action_deselect) { _, _ ->
            deselect()
        }.setNeutralButton(R.string.action_select_all) { _, _ ->
            selectedList.addAll(pagerAdapter.currentList.filterNot { it in selectedList })
            updateCurrentList()
            updateBarTitle()
            onMultiSelect()
        }.show()
    }

    private fun triStateTagDialog() {
        val initialStates = Array(HailData.tags.size) { index ->
            val tagId = HailData.tags[index].second
            when (selectedList.count { tagId in it.tagIdList }) {
                selectedList.size -> ToggleableState.On
                0 -> ToggleableState.Off
                else -> ToggleableState.Indeterminate
            }
        }
        val states = mutableStateListOf(*initialStates)
        MaterialAlertDialogBuilder(activity).setTitle(R.string.action_tag_set).setView(ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { AppTheme { TriStateTagList(initialStates, states) } }
        }).setPositiveButton(android.R.string.ok) { _, _ ->
            selectedList.forEach {
                states.forEachIndexed { index, state ->
                    val tagId = HailData.tags[index].second
                    when (state) {
                        ToggleableState.On -> {
                            if (tagId !in it.tagIdList) it.tagIdList.add(tagId)
                        }

                        ToggleableState.Off -> it.tagIdList.remove(tagId)
                        ToggleableState.Indeterminate -> {}
                    }
                }
                if (it.tagIdList.isEmpty()) removeCheckedApp(it.packageName, false)
            }
            HailData.saveApps()
            deselect()
        }.setNeutralButton(R.string.action_tag_add) { _, _ ->
            showTagDialog(selectedList)
        }.setNegativeButton(android.R.string.cancel, null).show()
    }

    @Composable
    private fun TriStateTagList(initialStates: Array<ToggleableState>, states: MutableList<ToggleableState>) = Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        HailData.tags.forEachIndexed { index, tag ->
            Row(modifier = Modifier.fillMaxWidth().clickable {
                states[index] = if (initialStates[index] == ToggleableState.Indeterminate) when (states[index]) {
                    ToggleableState.On -> ToggleableState.Off
                    ToggleableState.Off -> ToggleableState.Indeterminate
                    ToggleableState.Indeterminate -> ToggleableState.On
                }
                else if (states[index] == ToggleableState.On) ToggleableState.Off
                else ToggleableState.On
            }.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                TriStateCheckbox(
                    state = states[index],
                    onClick = null,
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                )
                Spacer(modifier = Modifier.width(24.dp))
                Text(
                    text = tag.first,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    private fun launchApp(packageName: String) {
        if (AppManager.isAppFrozen(packageName, tagWorkingMode) &&
            AppManager.setAppFrozen(packageName, false, tagWorkingMode)
        ) {
            updateCurrentList()
        }
        if (tagWorkingMode == HailData.MODE_ISLAND_HIDE) {
            HIsland.ensureLaunchIntentExists(packageName)
        }
        app.packageManager.getLaunchIntentForPackage(packageName)?.let {
            startActivity(it)
        } ?: HUI.showToast(R.string.activity_not_found)
    }

    private fun setListFrozen(
        frozen: Boolean,
        list: List<AppInfo> = HailData.checkedList,
        updateList: Boolean = true,
        workingMode: String = HailData.workingMode
    ) {
        val result = executeListFrozen(frozen, list, workingMode) ?: return
        if (updateList) updateCurrentList()
        HUI.showToast(workingMode.operationMessageId(frozen), result)
    }

    private fun setAllAppsFrozen(frozen: Boolean) {
        val messages = HailData.checkedList.groupBy(HailData::appWorkingMode).mapNotNull { (workingMode, apps) ->
            executeListFrozen(frozen, apps, workingMode)?.let { result ->
                getString(workingMode.operationMessageId(frozen), result)
            }
        }
        updateCurrentList()
        if (messages.isNotEmpty()) HUI.showToast(
            messages.joinToString(separator = "\n"), isLengthLong = messages.size > 1
        )
    }

    private fun executeListFrozen(
        frozen: Boolean, list: List<AppInfo>, workingMode: String
    ): String? {
        if (workingMode == HailData.MODE_DEFAULT) {
            MaterialAlertDialogBuilder(activity).setMessage(R.string.msg_guide)
                .setPositiveButton(android.R.string.ok, null).show()
            return null
        } else if (workingMode == HailData.MODE_SHIZUKU_HIDE) {
            runCatching { HShizuku.isRoot }.onSuccess {
                if (!it) {
                    MaterialAlertDialogBuilder(activity).setMessage(R.string.shizuku_hide_adb)
                        .setPositiveButton(android.R.string.ok, null).show()
                    return null
                }
            }
        }
        val filtered = list.filter { AppManager.isAppFrozen(it.packageName, workingMode) != frozen }
        return AppManager.setListFrozen(frozen, workingMode, *filtered.toTypedArray()).also {
            if (it == null) HUI.showToast(R.string.permission_denied)
        }
    }

    private fun showTagDialog(list: List<AppInfo>? = null) {
        val binding = DialogInputBinding.inflate(layoutInflater)
        val position = tabs.selectedTabPosition
        val editedTagId = if (list == null) HailData.tags[position].second else null
        val permission = HailData.workingPermission(HailData.workingMode)
        val supportedActions = HailData.supportedWorkingActions(permission)
        val actionValues = listOf<String?>(null) + supportedActions
        val actionEntries = listOf(getString(R.string.tag_working_action_default)) +
                supportedActions.map { getString(it.workingActionTitleId()) }
        var selectedAction = editedTagId?.let(HailData::tagWorkingAction)
            ?.takeIf { it in supportedActions }

        binding.inputLayout.setHint(R.string.tag)
        list ?: binding.editText.setText(tag.first)
        binding.workingModeDropdown.setAdapter(
            ArrayAdapter(
                requireContext(), android.R.layout.simple_dropdown_item_1line, actionEntries
            )
        )
        binding.workingModeDropdown.setText(
            actionEntries[actionValues.indexOf(selectedAction).coerceAtLeast(0)], false
        )
        binding.workingModeDropdown.setOnItemClickListener { _, _, selectedPosition, _ ->
            selectedAction = actionValues[selectedPosition]
        }
        binding.workingModeLayout.isEnabled = supportedActions.isNotEmpty()

        MaterialAlertDialogBuilder(activity).setTitle(if (list != null) R.string.action_tag_add else R.string.action_tag_set)
            .setView(binding.root).setPositiveButton(android.R.string.ok) { _, _ ->
                val tagName = binding.editText.text.toString()
                val tagId = tagName.hashCode()
                val duplicated = HailData.tags.withIndex().any { (index, existingTag) ->
                    (list != null || index != position) &&
                            (existingTag.first == tagName || existingTag.second == tagId)
                }
                if (tagName.isBlank() || duplicated) return@setPositiveButton
                if (list != null) { // Add tag
                    HailData.tags.add(tagName to tagId)
                    HailData.setTagWorkingAction(tagId, selectedAction)
                    adapter.notifyItemInserted(adapter.itemCount - 1)
                    if (query.isEmpty() && tabs.tabCount == 2) tabs.isVisible = true
                    if (list == selectedList) triStateTagDialog() else tagDialog(list.first())
                } else { // Rename tag
                    val defaultTab = position == 0
                    val oldTagId = HailData.tags[position].second
                    val newTagId = if (defaultTab) 0 else tagId
                    HailData.tags[position] = tagName to newTagId
                    if (!defaultTab) {
                        HailData.checkedList.forEach {
                            val index = it.tagIdList.indexOf(oldTagId)
                            if (index != -1) it.tagIdList[index] = tagId
                        }
                        HailData.saveApps()
                    }
                    HailData.replaceTagWorkingAction(oldTagId, newTagId, selectedAction)
                    adapter.notifyItemChanged(position)
                    updateCurrentList()
                }
                HailData.saveTags()
            }.apply {
                if (list != null || position == 0) return@apply
                setNeutralButton(R.string.action_tag_remove) { _, _ ->
                    val tagIdToRemove = HailData.tags[position].second
                    HailData.checkedList.toList().forEach {
                        if (it.tagIdList.remove(tagIdToRemove) && it.tagIdList.isEmpty()) {
                            removeCheckedApp(it.packageName, false)
                        }
                    }
                    HailData.tags.removeAt(position)
                    HailData.setTagWorkingAction(tagIdToRemove, null)
                    adapter.notifyItemRemoved(position)
                    if (tabs.tabCount == 1) tabs.isVisible = false
                    HailData.saveApps()
                    HailData.saveTags()
                }
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun exportToClipboard(list: List<AppInfo>) {
        if (list.isEmpty()) return
        HUI.copyText(if (list.size > 1) JSONArray().run {
            list.forEach { put(it.packageName) }
            toString()
        } else list[0].packageName)
        HUI.showToast(
            R.string.msg_exported, if (list.size > 1) list.size.toString() else list[0].name
        )
    }

    private fun importFromClipboard() = runCatching {
        val str = HUI.pasteText() ?: throw IllegalArgumentException()
        val json = if (str.contains('[')) JSONArray(
            str.substring(
                str.indexOf('[')..str.indexOf(']', str.indexOf('['))
            )
        )
        else JSONArray().put(str)
        var i = 0
        for (index in 0 until json.length()) {
            val pkg = json.getString(index)
            if (HPackages.getApplicationInfoOrNull(pkg) != null && !HailData.isChecked(pkg)) {
                HailData.addCheckedApp(pkg, tag.second, false)
                i++
            }
        }
        if (i > 0) {
            HailData.saveApps()
            updateCurrentList()
        }
        HUI.showToast(getString(R.string.msg_imported, i.toString()))
    }

    private suspend fun importFrozenApp() = withContext(Dispatchers.IO) {
        HPackages.getInstalledApplications().map { it.packageName }
            .filter { AppManager.isAppFrozen(it, tagWorkingMode) && !HailData.isChecked(it) }
            .onEach { HailData.addCheckedApp(it, tag.second, false) }.size
    }

    private fun removeCheckedApp(packageName: String, saveApps: Boolean = true) {
        HailData.removeCheckedApp(packageName, saveApps)
        if (saveApps) updateCurrentList()
    }

    private fun MenuItem.updateIcon() = icon?.setTint(
        MaterialColors.getColor(
            activity.findViewById(R.id.toolbar),
            if (multiselect) androidx.appcompat.R.attr.colorPrimary else com.google.android.material.R.attr.colorOnSurface
        )
    )

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_multiselect -> {
                multiselect = !multiselect
                item.updateIcon()
                if (multiselect) {
                    updateBarTitle()
                    HUI.showToast(R.string.tap_to_select)
                } else deselect()
            }

            R.id.action_freeze_all -> setAllAppsFrozen(true)
            R.id.action_unfreeze_all -> setAllAppsFrozen(false)
            R.id.action_freeze_non_whitelisted -> setListFrozen(true, HailData.checkedList.filterNot { it.whitelisted })

            R.id.action_import_clipboard -> importFromClipboard()
            R.id.action_import_frozen -> lifecycleScope.launch {
                val size = importFrozenApp()
                if (size > 0) {
                    HailData.saveApps()
                    updateCurrentList()
                }
                HUI.showToast(getString(R.string.msg_imported, size.toString()))
            }

            R.id.action_export_current -> exportToClipboard(pagerAdapter.currentList)
            R.id.action_export_all -> exportToClipboard(HailData.checkedList)
        }
        return false
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            private var inited = false
            override fun onQueryTextChange(newText: String): Boolean {
                if (inited) {
                    query = newText
                    tabs.isVisible = query.isEmpty() && tabs.tabCount > 1
                    updateCurrentList()
                } else inited = true
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean = true
        })
        menu.findItem(R.id.action_multiselect).updateIcon()
    }

    override fun onDestroyView() {
        activity.closeFabMenu()
        pagerAdapter.onDestroy()
        super.onDestroyView()
        _binding = null
    }

    private fun String.workingActionTitleId(): Int = when (this) {
        HailData.STOP -> R.string.working_action_stop
        HailData.DISABLE -> R.string.working_action_disable
        HailData.HIDE -> R.string.working_action_hide
        HailData.SUSPEND -> R.string.working_action_suspend
        else -> R.string.working_action_unavailable
    }

    private fun String.operationMessageId(frozen: Boolean): Int = when (HailData.workingAction(this)) {
        HailData.STOP -> if (frozen) R.string.msg_force_stopped else R.string.msg_restored
        HailData.DISABLE -> if (frozen) R.string.msg_disabled else R.string.msg_enabled
        HailData.HIDE -> if (frozen) R.string.msg_hidden else R.string.msg_unhidden
        HailData.SUSPEND -> if (frozen) R.string.msg_suspended else R.string.msg_unsuspended
        else -> if (frozen) R.string.msg_freeze else R.string.msg_unfreeze
    }
}
