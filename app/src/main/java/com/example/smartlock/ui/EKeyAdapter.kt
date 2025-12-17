package com.example.smartlock.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartlock.databinding.ItemEkeyBinding
import com.example.smartlock.model.Passcode
import org.json.JSONObject

class EKeyAdapter(
    private val onDeleteClick: (Passcode) -> Unit
) : ListAdapter<Passcode, EKeyAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(
        val binding: ItemEkeyBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("ClickableViewAccessibility")
        fun bind(ekey: Passcode, onDeleteClick: (Passcode) -> Unit) = with(binding) {
            tvCode.text = ekey.code.chunked(3).joinToString(" ")

            tvType.text = ekey.type

            tvValidity.text = when (ekey.type) {
                "one-time" -> "Hiệu lực: Một lần"

                "timed" -> {
                    try {
                        val json = JSONObject(ekey.validity)
                        val start = json.getString("start")
                        val end = json.getString("end")

                        "Hiệu lực:\nTừ: $start\nĐến: $end"
                    } catch (e: Exception) {
                        "Hiệu lực: ${ekey.validity}"
                    }
                }
                else -> "Hiệu lực: ${ekey.validity}"
            }

            btnCopy.setOnClickListener {
                val context = itemView.context
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("E-key", ekey.code)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Đã sao chép: ${ekey.code}", Toast.LENGTH_SHORT).show()
            }

            btnDelete.setOnClickListener {
                onDeleteClick(ekey)
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
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEkeyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ekey = getItem(position)
        holder.bind(ekey, onDeleteClick)
    }

    class DiffCallback : DiffUtil.ItemCallback<Passcode>() {
        override fun areItemsTheSame(oldItem: Passcode, newItem: Passcode): Boolean =
            oldItem.code == newItem.code

        override fun areContentsTheSame(oldItem: Passcode, newItem: Passcode): Boolean =
            oldItem == newItem
    }
}
