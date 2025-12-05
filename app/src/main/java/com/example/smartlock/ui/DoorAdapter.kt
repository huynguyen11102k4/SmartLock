package com.example.smartlock.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartlock.databinding.ItemDoorBinding
import com.example.smartlock.model.Door

class DoorAdapter(
    private val onDoorClick: (Door) -> Unit
) : ListAdapter<Door, DoorAdapter.DoorViewHolder>(DIFF) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.hashCode().toLong()
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Door>() {
            override fun areItemsTheSame(oldItem: Door, newItem: Door): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Door, newItem: Door): Boolean =
                oldItem == newItem
        }
    }

    inner class DoorViewHolder(
        private val binding: ItemDoorBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(door: Door) = with(binding) {
            tvDoorName.text = door.name
            tvPermission.text = door.permission
            pbBattery.progress = door.battery
            tvBattery.text = "${door.battery}%"

            val tint = when {
                door.battery >= 70 -> 0xFF4CAF50
                door.battery >= 30 -> 0xFFFFC107
                else -> 0xFFF44336
            }.toInt()
            pbBattery.progressTintList = ColorStateList.valueOf(tint)

            root.setOnClickListener { onDoorClick(door) }
        }

        fun bindBatteryOnly(door: Door) = with(binding) {
            pbBattery.progress = door.battery
            tvBattery.text = "${door.battery}%"
            val tint = when {
                door.battery >= 70 -> 0xFF4CAF50
                door.battery >= 30 -> 0xFFFFC107
                else -> 0xFFF44336
            }.toInt()
            pbBattery.progressTintList = ColorStateList.valueOf(tint)
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
        if (payloads.isNotEmpty() && payloads.any { it == "payload_battery" }) {
            holder.bindBatteryOnly(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }
}
