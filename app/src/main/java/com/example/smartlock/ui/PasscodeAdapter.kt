package com.example.smartlock.ui

import android.view.LayoutInflater
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.smartlock.databinding.ItemPasscodeBinding
import com.example.smartlock.model.Passcode

class PasscodeAdapter(private val onDelete: (Passcode) -> Unit ): ListAdapter<Passcode, PasscodeAdapter.PasscodeViewHolder>(DIFF) {
    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Passcode>() {
            override fun areItemsTheSame(oldItem: Passcode, newItem: Passcode): Boolean {
                return oldItem.code == newItem.code && oldItem.doorId == newItem.doorId
            }

            override fun areContentsTheSame(oldItem: Passcode, newItem: Passcode): Boolean {
                return oldItem == newItem
            }
        }
    }
    inner class PasscodeViewHolder(private val binding: ItemPasscodeBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(passcode: Passcode){
            binding.tvCode.text = "Mã: ${passcode.code}"
            binding.tvValidity.text = "Hiệu lực: ${passcode.validity}"
            binding.root.setOnLongClickListener {
                onDelete(passcode)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): PasscodeViewHolder {
        val binding = ItemPasscodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PasscodeViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: PasscodeViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }
}