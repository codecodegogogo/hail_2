package com.aistra.hail.ui.home

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HPackages.myUserId
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.Job

class PagerAdapter private constructor(
    private val selectedList: List<AppInfo>,
    private val flags: MutableMap<String, Int>,
    private val workingModeHolder: WorkingModeHolder
) : ListAdapter<AppInfo, PagerAdapter.ViewHolder>(
    HomeDiff(selectedList, flags, workingModeHolder)
) {
    constructor(
        selectedList: List<AppInfo>, flags: MutableMap<String, Int> = mutableMapOf()
    ) : this(selectedList, flags, WorkingModeHolder())

    private var loadIconJob: Job? = null
    lateinit var onItemClickListener: OnItemClickListener
    lateinit var onItemLongClickListener: OnItemLongClickListener
    var workingMode: String
        get() = workingModeHolder.value
        set(value) {
            workingModeHolder.value = value
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_home, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val info = currentList[position]
        val state = info.state(workingMode)
        flags[info.packageName] = info.getFlag(selectedList, workingMode)
        holder.itemView.run {
            setOnClickListener { onItemClickListener.onItemClick(info) }
            setOnLongClickListener { onItemLongClickListener.onItemLongClick(info) }
            findViewById<ImageView>(R.id.app_icon).run {
                info.applicationInfo?.let {
                    loadIconJob = AppIconCache.loadIconBitmapAsync(
                        context,
                        it,
                        myUserId,
                        this
                    )
                } ?: run {
                    setImageDrawable(context.packageManager.defaultActivityIcon)
                    colorFilter = null
                }
            }
            findViewById<TextView>(R.id.app_name).run {
                text = buildString {
                    if (state == AppInfo.State.FROZEN) append("\u2744\uFE0F")
                    if (info.whitelisted) append("\uD83D\uDD12")
                    append(info.name)
                }
                isEnabled = true
                when {
                    info in selectedList -> setTextColor(
                        MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary)
                    )

                    state == AppInfo.State.NOT_FOUND -> setTextColor(
                        MaterialColors.getColor(this, androidx.appcompat.R.attr.colorError)
                    )

                    else -> setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                }
                setTextSize(TypedValue.COMPLEX_UNIT_SP, HailData.homeFontSize)
            }
        }
    }

    fun onDestroy() {
        if (loadIconJob?.isActive == true) loadIconJob?.cancel()
    }

    private class HomeDiff(
        private val selectedList: List<AppInfo>,
        private val flags: Map<String, Int>,
        private val workingModeHolder: WorkingModeHolder
    ) : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean = oldItem == newItem

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean =
            flags[oldItem.packageName] == newItem.getFlag(selectedList, workingModeHolder.value)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    interface OnItemClickListener {
        fun onItemClick(info: AppInfo)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(info: AppInfo): Boolean
    }
}

private class WorkingModeHolder(var value: String = HailData.workingMode)

private fun AppInfo.getFlag(selectedList: List<AppInfo>, workingMode: String) =
    (1 shl state(workingMode).ordinal) or (this in selectedList).shl(3) or whitelisted.shl(4)

private fun Boolean.shl(bitCount: Int) = if (this) 1 shl bitCount else 0
