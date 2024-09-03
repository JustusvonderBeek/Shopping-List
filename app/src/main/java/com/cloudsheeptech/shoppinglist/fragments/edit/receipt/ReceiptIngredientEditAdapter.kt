package com.cloudsheeptech.shoppinglist.fragments.edit.receipt

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.data.receipt.ApiDescription
import com.cloudsheeptech.shoppinglist.data.receipt.ApiIngredient
import com.cloudsheeptech.shoppinglist.databinding.ReceiptItemBinding
import com.cloudsheeptech.shoppinglist.databinding.ReceiptItemEditTextBinding

class ReceiptIngredientEditAdapter(val clickListener: ReceiptIngredientEditClickListener) : ListAdapter<ApiIngredient, ReceiptIngredientEditAdapter.ReceiptItemViewHolder>(ItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptItemViewHolder {
        return ReceiptItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ReceiptItemViewHolder, position: Int) {
        holder.bind(clickListener, getItem(position))
    }

    class ReceiptItemViewHolder private constructor(val binding : ReceiptItemEditTextBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(clickListener: ReceiptIngredientEditClickListener, ingredient : ApiIngredient) {
            binding.ingredient = ingredient
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent : ViewGroup) : ReceiptItemViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ReceiptItemEditTextBinding.inflate(layoutInflater, parent, false)
                return ReceiptItemViewHolder(binding)
            }
        }
    }

    class ReceiptIngredientEditClickListener(val clickListener: (itemId: Long, quantity: Int) -> Unit) {
        fun onClick(ingredient: ApiIngredient, quantity: Int) = clickListener(ingredient.id, quantity)
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