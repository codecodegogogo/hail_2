package com.aistra.hail.ui.launcher

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aistra.hail.R
import com.aistra.hail.app.IconlessLauncherApp
import com.aistra.hail.databinding.ItemIconlessLauncherBinding

class LauncherAdapter(
    private val onVisibilityClick: (IconlessLauncherApp) -> Unit,
    private val onLaunchClick: (IconlessLauncherApp) -> Unit
) : ListAdapter<IconlessLauncherApp, LauncherAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ItemIconlessLauncherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemIconlessLauncherBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.visibilityAction.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onVisibilityClick(getItem(position))
            }
            binding.launchAction.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onLaunchClick(getItem(position))
            }
        }

        fun bind(appInfo: IconlessLauncherApp) {
            binding.appName.text = appInfo.label
            binding.visibilityAction.setImageResource(
                if (appInfo.hidden) R.drawable.ic_iconfont_visibility
                else R.drawable.ic_iconfont_visibility_off
            )
            binding.visibilityAction.contentDescription = binding.root.context.getString(
                if (appInfo.hidden) R.string.action_show_launcher_icon
                else R.string.action_hide_launcher_icon
            )
            binding.launchAction.contentDescription = binding.root.context.getString(R.string.action_launch_app)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<IconlessLauncherApp>() {
            override fun areItemsTheSame(oldItem: IconlessLauncherApp, newItem: IconlessLauncherApp) =
                oldItem.packageName == newItem.packageName

            override fun areContentsTheSame(oldItem: IconlessLauncherApp, newItem: IconlessLauncherApp) =
                oldItem == newItem
        }
    }
}
