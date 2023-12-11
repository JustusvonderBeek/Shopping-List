package com.cloudsheeptech.shoppinglist.list_overview

import android.content.res.Resources
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.databinding.ShoppingListBinding
import com.cloudsheeptech.shoppinglist.datastructures.ItemListWithName

class ShoppingListAdapter(val clickListener: ListClickListener, private val resource : Resources, private val itemList : ItemListWithName<Item>) : ListAdapter<Item, ShoppingListAdapter.ShoppingListViewHolder>(ItemDiffCallback()) {

    suspend fun deleteItemAt(position : Int) {
        Log.i("WordListItemAdapter", "Remove item at $position")
//        vocabulary.removeVocabularyItem(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingListViewHolder {
        return ShoppingListViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ShoppingListViewHolder, position: Int) {
        holder.bind(clickListener, getItem(position), resource)
    }

    class ShoppingListViewHolder private constructor(val binding : ShoppingListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(clickListener: ListClickListener, item : Item, resource: Resources) {
            binding.item = item
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent : ViewGroup) : ShoppingListViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ShoppingListBinding.inflate(layoutInflater, parent, false)
                return ShoppingListViewHolder(binding)
            }
        }
    }

    class ListClickListener(val clickListener: (id: Int) -> Unit) {
        fun onClick(item: Item) = clickListener(item.ID.toInt())
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.ID == newItem.ID && oldItem.Name == newItem.Name && oldItem.ImagePath == newItem.ImagePath
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }
}