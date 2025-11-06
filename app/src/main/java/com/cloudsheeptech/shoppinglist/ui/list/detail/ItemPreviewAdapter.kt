package com.cloudsheeptech.shoppinglist.ui.list.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.databinding.ShoppingItemPreviewBinding
import com.cloudsheeptech.shoppinglist.list.model.DbItem

class ItemPreviewAdapter(
    val clickListener: ItemPreviewClickListener,
) : ListAdapter<DbItem, ItemPreviewAdapter.ItemViewHolder>(
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
            item: DbItem,
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
        val clickListener: (id: String) -> Unit,
    ) {
        fun onClick(item: DbItem) = clickListener(item.name)
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<DbItem>() {
        override fun areItemsTheSame(
            oldAppItem: DbItem,
            newAppItem: DbItem,
        ): Boolean = oldAppItem.name.equals(newAppItem.name, ignoreCase = true) && oldAppItem.icon == newAppItem.icon

        override fun areContentsTheSame(
            oldAppItem: DbItem,
            newAppItem: DbItem,
        ): Boolean = oldAppItem == newAppItem
    }
}
