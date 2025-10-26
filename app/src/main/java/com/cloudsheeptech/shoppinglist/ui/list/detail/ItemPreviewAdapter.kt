package com.cloudsheeptech.shoppinglist.ui.list.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.databinding.ShoppingItemPreviewBinding
import com.cloudsheeptech.shoppinglist.list.model.AppItem

class ItemPreviewAdapter(
    val clickListener: ItemPreviewClickListener,
) : ListAdapter<AppItem, ItemPreviewAdapter.ItemViewHolder>(
        ItemDiffCallback(),
    ) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ItemViewHolder = ItemViewHolder.from(parent)

    override fun onBindViewHolder(
        holder: ItemViewHolder,
        position: Int,
    ) {
        holder.bind(clickListener, getItem(position))
    }

    class ItemViewHolder private constructor(
        val binding: ShoppingItemPreviewBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            clickListener: ItemPreviewClickListener,
            item: AppItem,
        ) {
            binding.item = item
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ItemViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ShoppingItemPreviewBinding.inflate(layoutInflater, parent, false)
                return ItemViewHolder(binding)
            }
        }
    }

    class ItemPreviewClickListener(
        val clickListener: (id: Long) -> Unit,
    ) {
        fun onClick(item: AppItem) = clickListener(item.id!!)
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<AppItem>() {
        override fun areItemsTheSame(
            oldAppItem: AppItem,
            newAppItem: AppItem,
        ): Boolean = oldAppItem.id == newAppItem.id && oldAppItem.name == newAppItem.name && oldAppItem.icon == newAppItem.icon

        override fun areContentsTheSame(
            oldAppItem: AppItem,
            newAppItem: AppItem,
        ): Boolean = oldAppItem == newAppItem
    }
}
