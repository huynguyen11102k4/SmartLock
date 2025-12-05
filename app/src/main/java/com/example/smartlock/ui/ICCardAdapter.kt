package com.example.smartlock.ui

import android.view.LayoutInflater
import android.view.ViewGroup
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
        fun bind(c: ICCard) {
            b.tvCardName.text = c.name
            b.tvStatus.text = c.status
            b.root.setOnLongClickListener {
                onDelete(c); true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemIcCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}