package com.example.smartlock.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartlock.databinding.ItemIcCardBinding
import com.example.smartlock.model.ICCard

class ICCardAdapter(
    private val onDelete: (ICCard) -> Unit
) : ListAdapter<ICCard, ICCardAdapter.VH>(DIFF) {
    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ICCard>() {
            override fun areItemsTheSame(old: ICCard, new: ICCard) = old.id == new.id
            override fun areContentsTheSame(old: ICCard, new: ICCard) = old == new
        }
    }

    inner class VH(private val b: ItemIcCardBinding) : RecyclerView.ViewHolder(b.root) {
        @SuppressLint("ClickableViewAccessibility")
        fun bind(c: ICCard) {
            b.tvCardName.text = c.name
            b.tvStatus.text = c.status
            b.root.setOnLongClickListener {
                onDelete(c); true
            }
            b.root.setOnTouchListener { v, event ->
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemIcCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}