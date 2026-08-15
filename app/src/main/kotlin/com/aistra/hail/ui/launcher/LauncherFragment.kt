package com.aistra.hail.ui.launcher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.aistra.hail.R
import com.aistra.hail.app.IconlessLauncherEntry
import com.aistra.hail.app.IconlessLauncherManager
import com.aistra.hail.databinding.FragmentLauncherBinding
import com.aistra.hail.extensions.applyDefaultInsetter
import com.aistra.hail.extensions.isLandscape
import com.aistra.hail.extensions.isRtl
import com.aistra.hail.extensions.paddingRelative
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.utils.HUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherFragment : MainFragment() {
    private var _binding: FragmentLauncherBinding? = null
    private val binding get() = _binding!!
    private lateinit var launcherAdapter: LauncherAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLauncherBinding.inflate(inflater, container, false)
        launcherAdapter = LauncherAdapter(::launchApp, ::removeApp) {
            activity.supportActionBar?.title = getString(
                if (it) R.string.iconless_launcher_delete_mode else R.string.title_launcher
            )
        }
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(
                activity, resources.getInteger(
                    if (com.aistra.hail.app.HailData.compactIcon) R.integer.home_span_compact
                    else R.integer.home_span
                )
            )
            adapter = launcherAdapter
            applyDefaultInsetter { paddingRelative(isRtl, bottom = isLandscape) }
        }
        activity.appbar.setLiftOnScrollTargetView(binding.recyclerView)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!launcherAdapter.exitDeleteMode()) {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        activity.fab.setOnClickListener { showAddAppsDialog() }
        updateList()
    }

    private fun updateList() {
        val entries = IconlessLauncherManager.entries()
        binding.empty.isVisible = entries.isEmpty()
        launcherAdapter.submitList(entries)
        if (entries.isEmpty()) launcherAdapter.exitDeleteMode()
    }

    private fun showAddAppsDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) { IconlessLauncherManager.availableApps() }
            if (apps.isEmpty()) {
                HUI.showToast(R.string.iconless_launcher_no_apps)
                return@launch
            }
            val checked = BooleanArray(apps.size)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.action_add_launcher_app)
                .setMultiChoiceItems(apps.map { it.label }.toTypedArray(), checked) { _, which, isChecked ->
                    checked[which] = isChecked
                }
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    addApps(apps.filterIndexed { index, _ -> checked[index] }.map { it.packageName })
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun addApps(packageNames: List<String>) {
        if (packageNames.isEmpty()) return
        viewLifecycleOwner.lifecycleScope.launch {
            val failed = withContext(Dispatchers.IO) {
                packageNames.count { !IconlessLauncherManager.add(it) }
            }
            updateList()
            if (failed > 0) HUI.showToast(R.string.iconless_launcher_operation_failed)
        }
    }

    private fun launchApp(entry: IconlessLauncherEntry) {
        viewLifecycleOwner.lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                IconlessLauncherManager.launch(requireContext().applicationContext, entry)
            }
            if (!success) HUI.showToast(R.string.iconless_launcher_launch_failed)
        }
    }

    private fun removeApp(entry: IconlessLauncherEntry) {
        viewLifecycleOwner.lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) { IconlessLauncherManager.remove(entry) }
            if (success) updateList()
            else HUI.showToast(R.string.iconless_launcher_operation_failed)
        }
    }

    override fun onDestroyView() {
        launcherAdapter.exitDeleteMode()
        super.onDestroyView()
        _binding = null
    }
}
