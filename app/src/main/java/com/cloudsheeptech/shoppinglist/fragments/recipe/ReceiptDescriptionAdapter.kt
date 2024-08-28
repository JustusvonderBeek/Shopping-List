package com.cloudsheeptech.shoppinglist.fragments.recipe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.data.receipt.ApiDescription
import com.cloudsheeptech.shoppinglist.databinding.ReceiptDescriptionBinding

class ReceiptDescriptionAdapter() : ListAdapter<ApiDescription, ReceiptDescriptionAdapter.DescriptionViewHolder>(ItemDiffCallback())
{
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DescriptionViewHolder {
        return DescriptionViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: DescriptionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DescriptionViewHolder private constructor(val binding : ReceiptDescriptionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(description : ApiDescription) {
            binding.description = description
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent : ViewGroup) : DescriptionViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ReceiptDescriptionBinding.inflate(layoutInflater, parent, false)
                return DescriptionViewHolder(binding)
            }
        }
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<ApiDescription>() {
        override fun areItemsTheSame(oldItem: ApiDescription, newItem: ApiDescription): Boolean {
            return oldItem.step == newItem.step
        }

        override fun areContentsTheSame(oldItem: ApiDescription, newItem: ApiDescription): Boolean {
            return oldItem == newItem
        }
    }

}