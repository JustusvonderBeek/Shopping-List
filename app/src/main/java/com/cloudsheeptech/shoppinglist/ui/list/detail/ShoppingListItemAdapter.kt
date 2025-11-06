package com.cloudsheeptech.shoppinglist.ui.list.detail

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.databinding.ShoppingItemBinding
import com.cloudsheeptech.shoppinglist.list.model.AppItem
import com.cloudsheeptech.shoppinglist.list.model.ShoppingListPK
import com.cloudsheeptech.shoppinglist.list.repo.ShoppingListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShoppingListItemAdapter(
    val clickListener: ShoppingItemClickListener,
    val checkboxClickListener: ShoppingItemCheckboxClickListener,
    private val amountName: String,
    private val listPk: ShoppingListPK,
    private val shoppingListRepository: ShoppingListRepository,
) : ListAdapter<AppItem, ShoppingListItemAdapter.WordListItemViewHolder>(
        WordDiffCallback(),
    ) {
    suspend fun deleteItemAt(position: Int) {
        withContext(Dispatchers.IO) {
            try {
                if (position >= currentList.size) {
                    Log.e("ShoppingListItemAdapter", "Index $position is greater than list ${currentList.size}")
                    return@withContext
                }
                val item = currentList[position]
                if (item.name == null) {
                    Log.e("ShoppingListItemAdapter", "Cannot remove item ${item.name} in list $listPk because id is not set")
                    return@withContext
                }
                Log.d("ShoppingListItemAdapter", "Removing item ${item.name} at $position")
                shoppingListRepository.removeItemByName(listPk, item.name!!)
            } catch (ex: Exception) {
                Log.e("ShoppingListItemAdapter", "Failed to remove item: $ex")
            }
        }
    }

    override fun getItemId(position: Int): Long = currentList[position].name.toByte().toLong()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): WordListItemViewHolder = WordListItemViewHolder.from(parent)

    override fun onBindViewHolder(
        holder: WordListItemViewHolder,
        position: Int,
    ) {
        holder.bind(clickListener, checkboxClickListener, getItem(position), amountName)
    }

    class WordListItemViewHolder private constructor(
        val binding: ShoppingItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            clickListener: ShoppingItemClickListener,
            checkClickListener: ShoppingItemCheckboxClickListener,
            item: AppItem,
            amountName: String,
        ) {
            binding.item = item
            binding.amountName = amountName
            binding.clickListener = clickListener
            binding.checkClickListener = checkClickListener
            // When pressing the checkbox itself also update
            binding.itemCheckbox.setOnCheckedChangeListener { _, checked ->
//                Log.d("ShoppingListItemAdapter", "Checkbox itself pressed")
                if (item.checked != checked) {
                    checkClickListener.onClick(item)
                }
            }
//            Glide.with(binding.root).load(R.drawable.ic_item).into(binding.itemIcon)
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): WordListItemViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ShoppingItemBinding.inflate(layoutInflater, parent, false)
                return WordListItemViewHolder(binding)
            }
        }
    }

    class ShoppingItemClickListener(
        val clickListener: (wordId: String, count: Int) -> Unit,
    ) {
        fun onClick(
            item: AppItem,
            count: Int,
        ) = clickListener(item.name, count)
    }

    class ShoppingItemCheckboxClickListener(
        val clickListener: (itemId: String) -> Unit,
    ) {
        fun onClick(item: AppItem) = clickListener(item.name)
    }

    class WordDiffCallback : DiffUtil.ItemCallback<AppItem>() {
        override fun areItemsTheSame(
            oldItem: AppItem,
            newItem: AppItem,
        ): Boolean = oldItem.name.equals(newItem.name, ignoreCase = true)

        override fun areContentsTheSame(
            oldItem: AppItem,
            newItem: AppItem,
        ): Boolean = oldItem == newItem
    }
}
