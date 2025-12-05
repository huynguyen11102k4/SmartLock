package com.example.smartlock.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartlock.databinding.ItemEkeyBinding
import com.example.smartlock.model.Passcode

class EKeyAdapter(
    private val onDeleteClick: (Passcode) -> Unit
) : ListAdapter<Passcode, EKeyAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(
        val binding: ItemEkeyBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ekey: Passcode, onDeleteClick: (Passcode) -> Unit) = with(binding) {
            tvCode.text = ekey.code.chunked(3).joinToString(" ")

            tvType.text = ekey.type

            tvValidity.text = "Hiệu lực: ${ekey.validity}"

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
