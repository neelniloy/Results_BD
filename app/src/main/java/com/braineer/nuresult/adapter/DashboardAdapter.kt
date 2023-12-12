package com.braineer.nuresult.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.braineer.nuresult.DashboardItem
import com.braineer.nuresult.DashboardItemType
import com.braineer.nuresult.dashboardItemList
import com.braineer.nuresult.databinding.DashboardItemBinding

class DashboardAdapter(val callback: (DashboardItemType) -> Unit, val callbackDark: (DashboardItem, Int) -> Unit) : RecyclerView.Adapter<DashboardAdapter.DashboardItemViewHolder>(){


    class DashboardItemViewHolder(val binding: DashboardItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DashboardItem) {
            binding.item = item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardItemViewHolder {
        val binding = DashboardItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DashboardItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DashboardItemViewHolder, position: Int) {
        val item = dashboardItemList[position]
        holder.bind(item)
        holder.itemView.setOnClickListener {
            callback(item.type)
        }
        callbackDark(item,position)
    }

    override fun getItemCount() = dashboardItemList.size
}