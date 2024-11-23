package com.cloudsheeptech.shoppinglist.fragments.edit.receipt

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.data.recipe.ApiDescription
import com.cloudsheeptech.shoppinglist.databinding.ReceiptDescriptionEditTextBinding

class RecipeDescriptionEditAdapter(
    val clickListener: ReceiptDescriptionEditClickListener,
) : ListAdapter<ApiDescription, RecipeDescriptionEditAdapter.DescriptionViewHolder>(ItemDiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): DescriptionViewHolder = DescriptionViewHolder.from(parent)

    override fun onBindViewHolder(
        holder: DescriptionViewHolder,
        position: Int,
    ) {
        holder.bind(clickListener, getItem(position))
    }

    class DescriptionViewHolder private constructor(
        val binding: ReceiptDescriptionEditTextBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            clickListener: ReceiptDescriptionEditClickListener,
            description: ApiDescription,
        ) {
            binding.description = description
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): DescriptionViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ReceiptDescriptionEditTextBinding.inflate(layoutInflater, parent, false)
                return DescriptionViewHolder(binding)
            }
        }
    }

    class ReceiptDescriptionEditClickListener(
        val clickListener: (descOrder: Int) -> Unit,
    ) {
        fun onClick(description: ApiDescription) = clickListener(description.order)
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<ApiDescription>() {
        override fun areItemsTheSame(
            oldItem: ApiDescription,
            newItem: ApiDescription,
        ): Boolean = oldItem.step == newItem.step

        override fun areContentsTheSame(
            oldItem: ApiDescription,
            newItem: ApiDescription,
        ): Boolean = oldItem == newItem
    }
}
