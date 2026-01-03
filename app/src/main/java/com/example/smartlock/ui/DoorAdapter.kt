package com.example.smartlock.ui

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartlock.databinding.ItemDoorBinding
import com.example.smartlock.model.entity.Door
import java.text.SimpleDateFormat
import java.util.*

class DoorAdapter(
    private val onDoorClick: (Door) -> Unit,
    private val onMoreClick: (View, Door) -> Unit
) : ListAdapter<Door, DoorAdapter.DoorViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Door>() {
            override fun areItemsTheSame(oldItem: Door, newItem: Door): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Door, newItem: Door): Boolean =
                oldItem == newItem

            override fun getChangePayload(oldItem: Door, newItem: Door): Any? {
                return if (oldItem.battery != newItem.battery) "PAYLOAD_BATTERY" else null
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoorViewHolder {
        val binding = ItemDoorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DoorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DoorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: DoorViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains("PAYLOAD_BATTERY")) {
            holder.updateBatteryUI(getItem(position).battery)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class DoorViewHolder(private val binding: ItemDoorBinding) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
        fun bind(door: Door) = with(binding) {
            tvDoorName.text = door.name

            // Xử lý hiển thị Quyền hạn và Thời gian
            when (door.permission) {
                0 -> {
                    tvPermission.text = "Chủ sở hữu"
                    tvPermission.setTextColor(Color.parseColor("#C4A26A"))
                }
                1 -> {
                    tvPermission.text = "Quản trị viên"
                    tvPermission.setTextColor(Color.parseColor("#B3FFFFFF"))
                }
                2 -> {
                    tvPermission.text = "Người dùng"
                    tvPermission.setTextColor(Color.parseColor("#B3FFFFFF"))
                }
                3 -> {
                    // Chuyển đổi String ISO sang định dạng hiển thị dd/MM/yy
                    val from = door.validFrom?.let { formatDate(it) } ?: "..."
                    val to = door.validTo?.let { formatDate(it) } ?: "..."
                    tvPermission.text = "Hạn dùng: $from - $to"
                    tvPermission.setTextColor(Color.parseColor("#FFAB91"))
                }
                else -> {
                    tvPermission.text = "Thành viên"
                    tvPermission.setTextColor(Color.parseColor("#B3FFFFFF"))
                }
            }

            updateBatteryUI(door.battery)

            btnMore.setOnClickListener { view ->
                onMoreClick(view, door)
            }

            root.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(200)
                            .setInterpolator(OvershootInterpolator()).start()
                    }
                }
                false
            }

            root.setOnClickListener { onDoorClick(door) }
        }

        @SuppressLint("SetTextI18n")
        fun updateBatteryUI(level: Int) = with(binding) {
            pbBattery.progress = level
            tvBattery.text = "$level%"

            val color = when {
                level >= 70 -> "#4CAF50"
                level >= 30 -> "#FFC107"
                else -> "#F44336"
            }
            pbBattery.progressTintList = ColorStateList.valueOf(Color.parseColor(color))
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(dateString)

                val outputFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                date?.let { outputFormat.format(it) } ?: dateString
            } catch (e: Exception) {
                dateString.take(10)
            }
        }
    }
}