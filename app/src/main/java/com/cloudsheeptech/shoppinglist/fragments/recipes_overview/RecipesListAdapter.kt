package com.cloudsheeptech.shoppinglist.fragments.recipes_overview

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.data.recipe.DbRecipe
import com.cloudsheeptech.shoppinglist.databinding.ReceiptBinding

class RecipesListAdapter(val clickListener: ReceiptClickListener, private val resource : Resources) : ListAdapter<DbRecipe, RecipesListAdapter.ReceiptListViewHolder>(
    ItemDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptListViewHolder {
        return ReceiptListViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ReceiptListViewHolder, position: Int) {
        holder.bind(clickListener, getItem(position), resource)
    }

    class ReceiptListViewHolder private constructor(val binding : ReceiptBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(clickListener: ReceiptClickListener, receipt : DbRecipe, resource: Resources) {
            binding.receipt = receipt
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent : ViewGroup) : ReceiptListViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ReceiptBinding.inflate(layoutInflater, parent, false)
                return ReceiptListViewHolder(binding)
            }
        }
    }

    class ReceiptClickListener(val clickListener: (id: Long, from : Long) -> Unit) {
        fun onClick(item: DbRecipe) = clickListener(item.id, item.createdBy)
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<DbRecipe>() {
        override fun areItemsTheSame(oldItem: DbRecipe, newItem: DbRecipe): Boolean {
            return oldItem.id == newItem.id && oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: DbRecipe, newItem: DbRecipe): Boolean {
            return oldItem == newItem
        }
    }
}