package com.cloudsheeptech.shoppinglist.fragments.list_overview

import android.content.res.Resources
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.data.list.DbShoppingList
import com.cloudsheeptech.shoppinglist.databinding.ShoppingListBinding

class ShoppingListAdapter(val clickListener: ListClickListener, private val resource : Resources, private val itemList : List<DbShoppingList>) : ListAdapter<DbShoppingList, ShoppingListAdapter.ShoppingListViewHolder>(
    ItemDiffCallback()
) {

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
        fun bind(clickListener: ListClickListener, list : DbShoppingList, resource: Resources) {
            binding.list = list
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

    class ListClickListener(val clickListener: (id: Long, from : Long) -> Unit) {
        fun onClick(list: DbShoppingList) = clickListener(list.listId, list.createdBy)
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<DbShoppingList>() {
        override fun areItemsTheSame(oldItem: DbShoppingList, newItem: DbShoppingList): Boolean {
            return oldItem.listId == newItem.listId && oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: DbShoppingList, newItem: DbShoppingList): Boolean {
            return oldItem == newItem
        }
    }
}