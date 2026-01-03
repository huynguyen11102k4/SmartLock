package com.example.smartlock.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartlock.databinding.ItemEkeyBinding
import com.example.smartlock.model.entity.Passcode
import java.text.SimpleDateFormat
import java.util.Locale

class EKeyAdapter(
    private val onEKeyClick: (Passcode) -> Unit,
    private val onDelete: (Passcode) -> Unit
) : ListAdapter<Passcode, EKeyAdapter.ViewHolder>(DiffCallback()) {

    private val revealedCodes = mutableSetOf<String>()

    @SuppressLint("NotifyDataSetChanged")
    fun revealCode(code: String) {
        revealedCodes.add(code)
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemEkeyBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ekey: Passcode, isRevealed: Boolean, onEKeyClick: (Passcode) -> Unit, onDelete: (Passcode) -> Unit) = with(binding) {

            tvCode.text = if (isRevealed) {
                ekey.code?.chunked(3)?.joinToString(" ")
            } else {
                "••••••"
            }

            tvType.text = when(ekey.type) {
                "OneTime" -> "Mã dùng 1 lần"
                "Timed" -> "Mã có thời hạn"
                else -> "Mã khách"
            }

            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

            tvValidity.text = try {
                if (ekey.validFrom != null && ekey.validTo != null) {
                    val start = inputFormat.parse(ekey.validFrom)
                    val end = inputFormat.parse(ekey.validTo)
                    "Hạn: ${outputFormat.format(start!!)} - ${outputFormat.format(end!!)}"
                } else "Dùng 1 lần"
            } catch (e: Exception) {
                "Không xác định"
            }

            root.setOnClickListener { if (!isRevealed) onEKeyClick(ekey) }

            btnDelete.setOnClickListener { onDelete(ekey) }
            btnCopy.isEnabled = isRevealed

            if(btnCopy.isEnabled) {
                btnCopy.setOnClickListener {
                    val clipboard =
                        root.context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("E-key", ekey.code)
                    clipboard.setPrimaryClip(clip)
                    android.widget.Toast.makeText(
                        root.context,
                        "Đã sao chép: ${ekey.code}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemEkeyBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ekey = getItem(position)
        holder.bind(ekey, revealedCodes.contains(ekey.code), onEKeyClick, onDelete)
    }

    class DiffCallback : DiffUtil.ItemCallback<Passcode>() {
        override fun areItemsTheSame(oldItem: Passcode, newItem: Passcode) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Passcode, newItem: Passcode) = oldItem == newItem
    }
}