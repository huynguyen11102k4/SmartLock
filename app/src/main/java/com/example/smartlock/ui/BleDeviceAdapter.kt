package com.example.smartlock.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartlock.databinding.ItemBleDeviceBinding

class BleDeviceAdapter(private val onClick: (name: String, mac: String) -> Unit) : ListAdapter<Pair<String, String>, BleDeviceAdapter.VH>(DIFF){
    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Pair<String, String>>() {
            override fun areItemsTheSame(o: Pair<String, String>, n: Pair<String, String>) = o.second == n.second
            override fun areContentsTheSame(o: Pair<String, String>, n: Pair<String, String>) = o == n
        }
    }

    inner class VH(private val b: ItemBleDeviceBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(p: Pair<String, String>) {
            b.tvName.text = p.first
            b.tvMac.text = p.second
            b.root.setOnClickListener { onClick(p.first, p.second) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemBleDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}