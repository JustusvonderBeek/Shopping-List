package com.cloudsheeptech.shoppinglist.fragments.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.data.items.DbItem
import com.cloudsheeptech.shoppinglist.databinding.ShoppingItemPreviewBinding

class ItemPreviewAdapter(val clickListener: ItemPreviewClickListener) : ListAdapter<DbItem, ItemPreviewAdapter.ItemViewHolder>(
    ItemDiffCallback()
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(clickListener, getItem(position))
    }

    class ItemViewHolder private constructor(val binding : ShoppingItemPreviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(clickListener: ItemPreviewClickListener, dbItem : DbItem) {
            binding.item = dbItem
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
        fun onClick(dbItem: DbItem) = clickListener(dbItem.id)
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<DbItem>() {
        override fun areItemsTheSame(oldDbItem: DbItem, newDbItem: DbItem): Boolean {
            return oldDbItem.id == newDbItem.id && oldDbItem.name == newDbItem.name && oldDbItem.icon == newDbItem.icon
        }

        override fun areContentsTheSame(oldDbItem: DbItem, newDbItem: DbItem): Boolean {
            return oldDbItem == newDbItem
        }
    }
}