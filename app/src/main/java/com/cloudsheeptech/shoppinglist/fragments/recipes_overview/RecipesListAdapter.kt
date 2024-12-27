package com.cloudsheeptech.shoppinglist.fragments.recipes_overview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.data.recipe.DbRecipe
import com.cloudsheeptech.shoppinglist.databinding.RecipeOverviewListItemBinding

class RecipesListAdapter(
    val clickListener: ReceiptClickListener
) : ListAdapter<DbRecipe, RecipesListAdapter.ReceiptListViewHolder>(
    ItemDiffCallback(),
) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ReceiptListViewHolder = ReceiptListViewHolder.from(parent)

    override fun onBindViewHolder(
        holder: ReceiptListViewHolder,
        position: Int,
    ) {
        holder.bind(clickListener, getItem(position))
    }

    class ReceiptListViewHolder private constructor(
        val binding: RecipeOverviewListItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            clickListener: ReceiptClickListener,
            receipt: DbRecipe
        ) {
            binding.receipt = receipt
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ReceiptListViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = RecipeOverviewListItemBinding.inflate(layoutInflater, parent, false)
                return ReceiptListViewHolder(binding)
            }
        }
    }

    class ReceiptClickListener(
        val clickListener: (id: Long, from: Long, title: String) -> Unit,
    ) {
        fun onClick(item: DbRecipe) = clickListener(item.id, item.createdBy, item.name)
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<DbRecipe>() {
        override fun areItemsTheSame(
            oldItem: DbRecipe,
            newItem: DbRecipe,
        ): Boolean = oldItem.id == newItem.id && oldItem.name == newItem.name

        override fun areContentsTheSame(
            oldItem: DbRecipe,
            newItem: DbRecipe,
        ): Boolean = oldItem == newItem
    }
}
