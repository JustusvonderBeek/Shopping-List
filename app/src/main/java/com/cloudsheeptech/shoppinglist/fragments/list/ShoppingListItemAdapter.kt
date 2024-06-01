package com.cloudsheeptech.shoppinglist.fragments.list

import android.content.res.Resources
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.data.ItemWithQuantity
import com.cloudsheeptech.shoppinglist.data.database.ItemListMappingDao
import com.cloudsheeptech.shoppinglist.databinding.ShoppingItemBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShoppingListItemAdapter(val clickListener: ShoppingItemClickListener, val checkboxClickListener: ShoppingItemCheckboxClickListener, private val resource : Resources, private val mappingDao: ItemListMappingDao, private val listId : Long) : ListAdapter<ItemWithQuantity, ShoppingListItemAdapter.WordListItemViewHolder>(
    WordDiffCallback()
) {

    suspend fun deleteItemAt(position : Int) {
        Log.i("WordListItemAdapter", "Remove item at $position")
        withContext(Dispatchers.IO) {
            val item = currentList[position]
            // TODO: Replace with list handler
            mappingDao.deleteMappingItemListId(item.ID, listId)
        }
        notifyItemRemoved(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordListItemViewHolder {
        return WordListItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: WordListItemViewHolder, position: Int) {
        holder.bind(clickListener, checkboxClickListener, getItem(position),  resource)
    }

    class WordListItemViewHolder private constructor(val binding : ShoppingItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(clickListener: ShoppingItemClickListener, checkClickListener : ShoppingItemCheckboxClickListener, item : ItemWithQuantity, resource: Resources) {
            binding.item = item
            binding.clickListener = clickListener
            binding.checkClickListener = checkClickListener
            // When pressing the checkbox itself also update
            binding.itemCheckbox.setOnCheckedChangeListener { _, checked ->
//                Log.d("ShoppingListItemAdapter", "Checkbox itself pressed")
                if (item.Checked != checked)
                    checkClickListener.onClick(item)
            }
//            Glide.with(binding.root).load(R.drawable.ic_item).into(binding.itemIcon)
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

    class ShoppingItemClickListener(val clickListener: (wordId: Int, count : Int) -> Unit) {
        fun onClick(item: ItemWithQuantity, count : Int) = clickListener(item.ID.toInt(), count)
    }

    class ShoppingItemCheckboxClickListener(val clickListener : (itemId : Int) -> Unit) {
        fun onClick(item: ItemWithQuantity) = clickListener(item.ID.toInt())
    }

    class WordDiffCallback : DiffUtil.ItemCallback<ItemWithQuantity>() {
        override fun areItemsTheSame(oldItem: ItemWithQuantity, newItem: ItemWithQuantity): Boolean {
            return oldItem.ID == newItem.ID && oldItem.Name == newItem.Name && oldItem.IconPath == newItem.IconPath
        }

        override fun areContentsTheSame(oldItem: ItemWithQuantity, newItem: ItemWithQuantity): Boolean {
            return oldItem == newItem
        }
    }
}