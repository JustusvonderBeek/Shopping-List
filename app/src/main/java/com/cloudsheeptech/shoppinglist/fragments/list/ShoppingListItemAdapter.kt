package com.cloudsheeptech.shoppinglist.fragments.list

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.data.items.AppItem
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRepository
import com.cloudsheeptech.shoppinglist.databinding.ShoppingItemBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShoppingListItemAdapter(
    val clickListener: ShoppingItemClickListener,
    val checkboxClickListener: ShoppingItemCheckboxClickListener,
    private val amountName: String,
    private val listPK: Pair<Long, Long>,
    private val shoppingListRepository: ShoppingListRepository,
) : ListAdapter<AppItem, ShoppingListItemAdapter.WordListItemViewHolder>(
        WordDiffCallback(),
    ) {
    suspend fun deleteItemAt(position: Int) {
        withContext(Dispatchers.IO) {
            val item = currentList[position]
            Log.d("ShoppingListItemAdapter", "Removing item ${item.name} at $position")
            shoppingListRepository.removeItem(listPK.first, listPK.second, item.id)
        }
    }

    override fun getItemId(position: Int): Long = currentList[position].id

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
        val clickListener: (wordId: Int, count: Int) -> Unit,
    ) {
        fun onClick(
            item: AppItem,
            count: Int,
        ) = clickListener(item.id.toInt(), count)
    }

    class ShoppingItemCheckboxClickListener(
        val clickListener: (itemId: Int) -> Unit,
    ) {
        fun onClick(item: AppItem) = clickListener(item.id.toInt())
    }

    class WordDiffCallback : DiffUtil.ItemCallback<AppItem>() {
        override fun areItemsTheSame(
            oldItem: AppItem,
            newItem: AppItem,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: AppItem,
            newItem: AppItem,
        ): Boolean = oldItem == newItem
    }
}
