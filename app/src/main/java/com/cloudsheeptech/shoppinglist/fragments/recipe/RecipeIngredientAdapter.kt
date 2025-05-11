package com.cloudsheeptech.shoppinglist.fragments.recipe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.data.recipe.ApiIngredient
import com.cloudsheeptech.shoppinglist.databinding.ReceiptItemBinding

class RecipeIngredientAdapter : ListAdapter<ApiIngredient, RecipeIngredientAdapter.ReceiptItemViewHolder>(ItemDiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ReceiptItemViewHolder = ReceiptItemViewHolder.from(parent)

    override fun onBindViewHolder(
        holder: ReceiptItemViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    class ReceiptItemViewHolder private constructor(
        val binding: ReceiptItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ingredient: ApiIngredient) {
            binding.apiIngredient = ingredient
            val context = binding.root.context
            val evenColor = context.resources.getColor(R.color.app_green_700, context.theme)
            val unevenColor = context.resources.getColor(R.color.app_green_900, context.theme)
            val backgroundColor = if (bindingAdapterPosition % 2 == 0) evenColor else unevenColor
            binding.recipeItemCard.setCardBackgroundColor(backgroundColor)
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ReceiptItemViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ReceiptItemBinding.inflate(layoutInflater, parent, false)
                return ReceiptItemViewHolder(binding)
            }
        }
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<ApiIngredient>() {
        override fun areItemsTheSame(
            oldItem: ApiIngredient,
            newItem: ApiIngredient,
        ): Boolean = oldItem.name == newItem.name && oldItem.quantity == newItem.quantity && oldItem.quantityType == newItem.quantityType

        override fun areContentsTheSame(
            oldItem: ApiIngredient,
            newItem: ApiIngredient,
        ): Boolean = oldItem == newItem
    }
}
