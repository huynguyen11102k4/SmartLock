package com.example.smartlock.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.example.smartlock.R
import com.example.smartlock.databinding.ItemRecordBinding
import com.example.smartlock.model.Record
import java.text.SimpleDateFormat
import java.util.Locale

class RecordAdapter :
    ListAdapter<Record, RecordAdapter.RecordViewHolder>(RecordDiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecordViewHolder(private val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("ClickableViewAccessibility")
        fun bind(record: Record) {
            val time = dateFormat.format(record.timestamp)
            val displayText = getDisplayText(record)
            val color = getColor(record)
            val iconRes = getIcon(record)

            binding.tvTime.text = time
            binding.tvState.text = if (record.state == "unlocked") "Mở" else "Khóa"
            binding.tvState.setTextColor(color)
            binding.tvEvent.text = displayText
            binding.tvMethod.text = ""
            binding.tvDetail.text = ""

            binding.root.setOnTouchListener { v, event ->
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
        }

        private fun getDisplayText(record: Record): String {
            return when (record.event) {
                "unlock" -> when (record.method) {
                    "pin", "card", "swipe", "remote" -> "Mở khóa thành công bằng [${translateMethod(record.method)}]"
                    "master_pin" -> "Mở bằng mã chính"
                    "temp_pin" -> "Mở bằng mã tạm"
                    else -> record.event.capitalize(Locale.ROOT)
                }
                "wrong_pin" -> "Nhập sai mã PIN"
                "master_set" -> "Thiết lập mã chính lần đầu"
                "master_changed" -> "Thay đổi mã chính thành công"
                "temp_passcode_added" -> "Tạo mã tạm từ điện thoại"
                "card_added" -> "Thêm thẻ ${if (record.method == "swipe") "bằng cách quẹt 2 lần" else "từ điện thoại"}"
                "card_deleted" -> "Xóa thẻ từ điện thoại"
                "lock" -> "Khóa cửa từ ${if (record.method == "remote") "điện thoại" else "tự động"}"
                "card_scan" -> "Quẹt thẻ → ${if (record.state == "unlocked") "thành công" else "thất bại"}"
                else -> "${record.event.replace("_", " ").capitalize(Locale.ROOT)} - ${record.method}"
            }.replace("[PIN/Thẻ/App]", translateMethod(record.method))
        }

        private fun translateMethod(method: String): String {
            return when (method) {
                "pin" -> "Mã PIN"
                "card" -> "Thẻ"
                "swipe" -> "Quẹt thẻ"
                "remote" -> "App"
                "master_pin" -> "Mã chính"
                else -> method
            }
        }

        private fun getColor(record: Record): Int {
            return when (record.event) {
                "unlock" -> 0xFF4CAF50.toInt()
                "wrong_pin" -> 0xFFF44336.toInt()
                "master_set", "master_changed", "temp_passcode_added", "card_added" -> 0xFF2196F3.toInt()  // Blue
                "card_deleted" -> 0xFFFF9800.toInt()
                "lock" -> 0xFF9E9E9E.toInt()
                else -> if (record.state == "unlocked") 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
            }
        }

        private fun getIcon(record: Record): Int {
            return when (record.event) {
                "unlock" -> R.drawable.ic_unlock
                "wrong_pin" -> R.drawable.ic_error
                "master_set", "master_changed" -> R.drawable.ic_settings
                "card_added", "card_deleted" -> R.drawable.ic_card
                "lock" -> R.drawable.ic_lock
                else -> R.drawable.ic_info
            }
        }
    }
}

class RecordDiffCallback : DiffUtil.ItemCallback<Record>() {
    override fun areItemsTheSame(oldItem: Record, newItem: Record): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Record, newItem: Record): Boolean =
        oldItem == newItem
}