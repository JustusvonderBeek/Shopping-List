package com.cloudsheeptech.shoppinglist.list

import android.content.res.Resources
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.databinding.ShoppingItemBinding

class ShoppingListItemAdapter(val clickListener: ShoppingItemClickListener, private val resource : Resources) : ListAdapter<Item, ShoppingListItemAdapter.WordListItemViewHolder>(WordDiffCallback()) {

    suspend fun deleteItemAt(position : Int) {
        Log.i("WordListItemAdapter", "Remove item at $position")
//        vocabulary.removeVocabularyItem(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordListItemViewHolder {
        return WordListItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: WordListItemViewHolder, position: Int) {
        holder.bind(clickListener, getItem(position), resource)
    }

    class WordListItemViewHolder private constructor(val binding : ShoppingItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(clickListener: ShoppingItemClickListener, item : Item, resource: Resources) {
            binding.item = item
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent : ViewGroup) : WordListItemViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ShoppingItemBinding.inflate(layoutInflater, parent, false)
                return WordListItemViewHolder(binding)
            }
        }
    }

    class ShoppingItemClickListener(val clickListener: (wordId: Int) -> Unit) {
        fun onClick(item: Item) = clickListener(item.ID.toInt())
    }

    class WordDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.ID == newItem.ID && oldItem.Name == newItem.Name && oldItem.ImagePath == newItem.ImagePath
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }
}