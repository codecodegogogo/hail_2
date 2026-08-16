package com.aistra.hail.ui.launcher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.app.IconlessLauncherApp
import com.aistra.hail.app.IconlessLauncherManager
import com.aistra.hail.databinding.FragmentIconlessLauncherBinding
import com.aistra.hail.extensions.applyDefaultInsetter
import com.aistra.hail.extensions.isLandscape
import com.aistra.hail.extensions.isRtl
import com.aistra.hail.extensions.marginRelative
import com.aistra.hail.extensions.paddingRelative
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.utils.HUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherFragment : MainFragment(), MenuProvider {
    private val model: LauncherViewModel by viewModels()
    private var _binding: FragmentIconlessLauncherBinding? = null
    private val binding get() = _binding!!
    private lateinit var launcherAdapter: LauncherAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        (requireActivity() as MenuHost).addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        _binding = FragmentIconlessLauncherBinding.inflate(inflater, container, false)
        launcherAdapter = LauncherAdapter(::changeIconVisibility, ::launchApp)

        binding.refresh.apply {
            setOnRefreshListener(model::updateAppList)
            applyDefaultInsetter { marginRelative(isRtl, start = !isLandscape, end = true) }
        }
        binding.recyclerView.apply {
            activity.appbar.setLiftOnScrollTargetView(this)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = launcherAdapter
            applyDefaultInsetter { paddingRelative(isRtl, bottom = isLandscape) }
        }
        model.isRefreshing.observe(viewLifecycleOwner) { binding.refresh.isRefreshing = it }
        model.displayApps.observe(viewLifecycleOwner) {
            launcherAdapter.submitList(it)
            binding.empty.isVisible = it.isEmpty()
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        model.updateAppList()
    }

    private fun changeIconVisibility(appInfo: IconlessLauncherApp) {
        if (!HailData.iconlessLauncherEnabled) {
            HUI.showToast(R.string.iconless_launcher_enable_first)
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val hidden = !appInfo.hidden
            val success = withContext(Dispatchers.IO) {
                IconlessLauncherManager.setIconHidden(appInfo, hidden)
            }
            if (success) {
                HUI.showToast(
                    if (hidden) R.string.iconless_launcher_icon_hidden
                    else R.string.iconless_launcher_icon_shown,
                    appInfo.label
                )
                model.updateAppList()
            } else HUI.showToast(R.string.iconless_launcher_operation_failed)
        }
    }

    private fun launchApp(appInfo: IconlessLauncherApp) {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                IconlessLauncherManager.launch(appContext, appInfo)
            }
            if (!success) HUI.showToast(R.string.iconless_launcher_launch_failed)
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_iconless_launcher, menu)
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                model.postQuery(newText, if (newText.isEmpty()) 0L else 300L)
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                model.postQuery(query, 0L)
                return true
            }
        })
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(
            when (HailData.iconlessSortBy) {
                HailData.SORT_INSTALL -> R.id.sort_by_install
                HailData.SORT_UPDATE -> R.id.sort_by_update
                else -> R.id.sort_by_name
            }
        ).isChecked = true
        menu.findItem(
            if (HailData.iconlessFilterSystemApps) R.id.filter_system_apps else R.id.filter_user_apps
        ).isChecked = true
        menu.findItem(R.id.filter_hidden_launcher_apps).isChecked = HailData.iconlessFilterHiddenApps
        menu.findItem(R.id.filter_visible_launcher_apps).isChecked = HailData.iconlessFilterVisibleApps
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.sort_by_name -> changeSort(HailData.SORT_NAME, item)
        R.id.sort_by_install -> changeSort(HailData.SORT_INSTALL, item)
        R.id.sort_by_update -> changeSort(HailData.SORT_UPDATE, item)
        R.id.filter_user_apps -> changeAppTypeFilter(false, item)
        R.id.filter_system_apps -> changeAppTypeFilter(true, item)
        R.id.filter_hidden_launcher_apps -> changeVisibilityFilter(HailData.ICONLESS_FILTER_HIDDEN_APPS, item)
        R.id.filter_visible_launcher_apps -> changeVisibilityFilter(HailData.ICONLESS_FILTER_VISIBLE_APPS, item)
        else -> false
    }

    private fun changeSort(sort: String, item: MenuItem): Boolean {
        item.isChecked = true
        HailData.changeIconlessAppsSort(sort)
        model.updateDisplayAppList()
        return true
    }

    private fun changeAppTypeFilter(systemApps: Boolean, item: MenuItem): Boolean {
        item.isChecked = true
        HailData.changeIconlessAppsFilter(HailData.ICONLESS_FILTER_USER_APPS, !systemApps)
        HailData.changeIconlessAppsFilter(HailData.ICONLESS_FILTER_SYSTEM_APPS, systemApps)
        model.updateDisplayAppList()
        return true
    }

    private fun changeVisibilityFilter(key: String, item: MenuItem): Boolean {
        item.isChecked = !item.isChecked
        HailData.changeIconlessAppsFilter(key, item.isChecked)
        model.updateDisplayAppList()
        return true
    }

    override fun onDestroyView() {
        binding.recyclerView.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
