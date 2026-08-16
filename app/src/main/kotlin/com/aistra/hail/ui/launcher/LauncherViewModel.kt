package com.aistra.hail.ui.launcher

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aistra.hail.app.HailData
import com.aistra.hail.app.IconlessLauncherApp
import com.aistra.hail.app.IconlessLauncherManager
import com.aistra.hail.utils.FuzzySearch
import com.aistra.hail.utils.PinyinSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    val apps = MutableLiveData<List<IconlessLauncherApp>>(emptyList())
    val displayApps = MutableLiveData<List<IconlessLauncherApp>>(emptyList())
    val isRefreshing = MutableLiveData(false)
    private var query = ""
    private var queryJob: Job? = null

    fun updateAppList() {
        viewModelScope.launch {
            isRefreshing.value = true
            val updated = withContext(Dispatchers.IO) { IconlessLauncherManager.apps() }
            apps.value = updated
            updateDisplayAppList()
            isRefreshing.value = false
        }
    }

    fun postQuery(value: String, delayTime: Long = 300L) {
        queryJob?.cancel()
        queryJob = viewModelScope.launch {
            if (delayTime > 0L) delay(delayTime)
            query = value
            updateDisplayAppList()
        }
    }

    fun updateDisplayAppList() {
        val source = apps.value.orEmpty()
        viewModelScope.launch {
            displayApps.value = withContext(Dispatchers.Default) {
                val collator = Collator.getInstance()
                source.asSequence()
                    .filter { appInfo ->
                        val isSystem = appInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                        ((HailData.iconlessFilterUserApps && !isSystem)
                            || (HailData.iconlessFilterSystemApps && isSystem))
                            && ((HailData.iconlessFilterHiddenApps && appInfo.hidden)
                            || (HailData.iconlessFilterVisibleApps && !appInfo.hidden))
                            && (FuzzySearch.search(appInfo.packageName, query)
                            || FuzzySearch.search(appInfo.label, query)
                            || PinyinSearch.searchPinyinAll(appInfo.label, query))
                    }
                    .sortedWith { left, right ->
                        when {
                            left.operated != right.operated -> if (left.operated) -1 else 1
                            left.operated && left.lastOperatedAt != right.lastOperatedAt ->
                                right.lastOperatedAt.compareTo(left.lastOperatedAt)
                            HailData.iconlessSortBy == HailData.SORT_INSTALL ->
                                left.firstInstallTime.compareTo(right.firstInstallTime)
                            HailData.iconlessSortBy == HailData.SORT_UPDATE ->
                                right.lastUpdateTime.compareTo(left.lastUpdateTime)
                            else -> collator.compare(left.label, right.label)
                        }
                    }
                    .toList()
            }
        }
    }
}
