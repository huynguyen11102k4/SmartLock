package com.example.smartlock.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartlock.R
import com.example.smartlock.databinding.ItemRecordBinding
import com.example.smartlock.model.entity.DoorRecord
import java.text.SimpleDateFormat
import java.util.*

class RecordAdapter : ListAdapter<DoorRecord, RecordAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DoorRecord>() {
            override fun areItemsTheSame(old: DoorRecord, new: DoorRecord) = old.id == new.id
            override fun areContentsTheSame(old: DoorRecord, new: DoorRecord) = old == new
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: DoorRecord) = with(binding) {
            tvEventName.text = record.event ?: "Sự kiện không xác định"
            tvMethod.text = "Phương thức: ${record.method ?: "N/A"}"

            tvTime.text = record.occurredAt?.let { formatTime(it) } ?: "--:--"

            val event = record.event ?: ""
            val iconRes = if (event.contains("Unlock", true)) R.drawable.ic_unlock else R.drawable.ic_lock
            ivEventIcon.setImageResource(iconRes)
        }

        private fun formatTime(isoString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(isoString)

                val outputFormat = SimpleDateFormat("HH:mm\ndd/MM", Locale.getDefault())
                date?.let { outputFormat.format(it) } ?: isoString
            } catch (e: Exception) {
                isoString
            }
        }
    }
}