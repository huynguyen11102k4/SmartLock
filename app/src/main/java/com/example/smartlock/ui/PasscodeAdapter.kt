package com.example.smartlock.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartlock.databinding.ItemPasscodeBinding
import com.example.smartlock.model.entity.Passcode

class PasscodeAdapter(private val onDelete: (Passcode) -> Unit ): ListAdapter<Passcode, PasscodeAdapter.PasscodeViewHolder>(DIFF) {
    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Passcode>() {
            override fun areItemsTheSame(oldItem: Passcode, newItem: Passcode): Boolean {
                return oldItem.code == newItem.code
            }

            override fun areContentsTheSame(oldItem: Passcode, newItem: Passcode): Boolean {
                return oldItem == newItem
            }
        }
    }
    inner class PasscodeViewHolder(private val binding: ItemPasscodeBinding): RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("ClickableViewAccessibility")
        fun bind(passcode: Passcode){
            binding.tvCode.text = "Mã: ${passcode.code}"
//            binding.tvValidity.text = "Hiệu lực: ${passcode.validity}"
            binding.root.setOnLongClickListener {
                onDelete(passcode)
                true
            }
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