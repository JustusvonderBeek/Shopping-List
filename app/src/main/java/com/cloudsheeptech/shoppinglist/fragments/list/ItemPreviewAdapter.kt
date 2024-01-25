package com.cloudsheeptech.shoppinglist.fragments.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.databinding.ShoppingItemPreviewBinding

class ItemPreviewAdapter(val clickListener: ItemPreviewClickListener) : ListAdapter<Item, ItemPreviewAdapter.ItemViewHolder>(
    ItemDiffCallback()
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(clickListener, getItem(position))
    }

    class ItemViewHolder private constructor(val binding : ShoppingItemPreviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(clickListener: ItemPreviewClickListener, item : Item) {
            binding.item = item
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent : ViewGroup) : ItemViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ShoppingItemPreviewBinding.inflate(layoutInflater, parent, false)
                return ItemViewHolder(binding)
            }
        }
    }

    class ItemPreviewClickListener(val clickListener: (id : Long) -> Unit) {
        fun onClick(item: Item) = clickListener(item.ID)
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.ID == newItem.ID && oldItem.Name == newItem.Name && oldItem.Icon == newItem.Icon
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }
}