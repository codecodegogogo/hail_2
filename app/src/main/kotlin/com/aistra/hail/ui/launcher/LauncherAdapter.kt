package com.aistra.hail.ui.launcher

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aistra.hail.app.IconlessLauncherEntry
import com.aistra.hail.app.IconlessLauncherManager
import com.aistra.hail.databinding.ItemLauncherBinding
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HPackages
import kotlinx.coroutines.Job

class LauncherAdapter(
    private val onLaunch: (IconlessLauncherEntry) -> Unit,
    private val onRemove: (IconlessLauncherEntry) -> Unit,
    private val onDeleteModeChanged: (Boolean) -> Unit
) : ListAdapter<IconlessLauncherEntry, LauncherAdapter.ViewHolder>(DIFF) {
    var deleteMode = false
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ItemLauncherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    override fun onViewRecycled(holder: ViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    fun enterDeleteMode() {
        if (deleteMode) return
        deleteMode = true
        notifyItemRangeChanged(0, itemCount)
        onDeleteModeChanged(true)
    }

    fun exitDeleteMode(): Boolean {
        if (!deleteMode) return false
        deleteMode = false
        notifyItemRangeChanged(0, itemCount)
        onDeleteModeChanged(false)
        return true
    }

    inner class ViewHolder(private val binding: ItemLauncherBinding) : RecyclerView.ViewHolder(binding.root) {
        private var loadIconJob: Job? = null

        init {
            binding.appContent.setOnClickListener {
                val position = bindingAdapterPosition
                if (!deleteMode && position != RecyclerView.NO_POSITION) onLaunch(getItem(position))
            }
            binding.appContent.setOnLongClickListener {
                enterDeleteMode()
                true
            }
            binding.delete.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onRemove(getItem(position))
            }
        }

        fun bind(entry: IconlessLauncherEntry) {
            val info = IconlessLauncherManager.applicationInfo(entry)
            binding.appName.text = info?.loadLabel(binding.root.context.packageManager) ?: entry.packageName
            binding.delete.isVisible = deleteMode
            binding.appContent.contentDescription = binding.appName.text
            loadIconJob?.cancel()
            if (info == null) {
                binding.appIcon.setImageDrawable(binding.root.context.packageManager.defaultActivityIcon)
            } else {
                loadIconJob = AppIconCache.loadIconBitmapAsync(
                    binding.root.context,
                    info,
                    HPackages.myUserId,
                    binding.appIcon,
                    false
                )
            }
        }

        fun recycle() {
            loadIconJob?.cancel()
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<IconlessLauncherEntry>() {
            override fun areItemsTheSame(oldItem: IconlessLauncherEntry, newItem: IconlessLauncherEntry) =
                oldItem.packageName == newItem.packageName

            override fun areContentsTheSame(oldItem: IconlessLauncherEntry, newItem: IconlessLauncherEntry) =
                oldItem == newItem
        }
    }
}
