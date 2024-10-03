package com.cloudsheeptech.shoppinglist.fragments.recipe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.data.recipe.ApiIngredient
import com.cloudsheeptech.shoppinglist.databinding.ReceiptItemBinding

class RecipeIngredientAdapter() : ListAdapter<ApiIngredient, RecipeIngredientAdapter.ReceiptItemViewHolder>(ItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptItemViewHolder {
        return ReceiptItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ReceiptItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReceiptItemViewHolder private constructor(val binding : ReceiptItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ingredient : ApiIngredient) {
            binding.apiIngredient = ingredient
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent : ViewGroup) : ReceiptItemViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ReceiptItemBinding.inflate(layoutInflater, parent, false)
                return ReceiptItemViewHolder(binding)
            }
        }
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<ApiIngredient>() {
        override fun areItemsTheSame(oldItem: ApiIngredient, newItem: ApiIngredient): Boolean {
            return oldItem.name == newItem.name && oldItem.quantity == newItem.quantity && oldItem.quantityType == newItem.quantityType
        }

        override fun areContentsTheSame(oldItem: ApiIngredient, newItem: ApiIngredient): Boolean {
            return oldItem == newItem
        }
    }

}