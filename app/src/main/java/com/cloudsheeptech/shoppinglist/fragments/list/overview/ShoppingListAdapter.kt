package com.cloudsheeptech.shoppinglist.fragments.list.overview

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.data.list.ShoppingList
import com.cloudsheeptech.shoppinglist.databinding.ShoppingListOverviewItemBinding

class ShoppingListAdapter(
    val clickListener: ListClickListener,
) : ListAdapter<ShoppingList, ShoppingListAdapter.ShoppingListViewHolder>(
        ItemDiffCallback(),
    ) {
    suspend fun deleteItemAt(position: Int) {
        Log.i("WordListItemAdapter", "Remove item at $position")
//        vocabulary.removeVocabularyItem(position)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ShoppingListViewHolder = ShoppingListViewHolder.from(parent)

    override fun onBindViewHolder(
        holder: ShoppingListViewHolder,
        position: Int,
    ) {
        holder.bind(clickListener, getItem(position))
    }

    class ShoppingListViewHolder private constructor(
        val binding: ShoppingListOverviewItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            clickListener: ListClickListener,
            list: ShoppingList,
        ) {
            binding.list = list
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ShoppingListViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ShoppingListOverviewItemBinding.inflate(layoutInflater, parent, false)
                return ShoppingListViewHolder(binding)
            }
        }
    }

    class ListClickListener(
        val clickListener: (id: Long, from: Long, title: String) -> Unit,
    ) {
        fun onClick(list: ShoppingList) = clickListener(list.listId, list.createdBy.onlineId, list.title)
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<ShoppingList>() {
        override fun areItemsTheSame(
            oldItem: ShoppingList,
            newItem: ShoppingList,
        ): Boolean = oldItem.listId == newItem.listId && oldItem.title == newItem.title

        override fun areContentsTheSame(
            oldItem: ShoppingList,
            newItem: ShoppingList,
        ): Boolean = oldItem == newItem
    }
}
