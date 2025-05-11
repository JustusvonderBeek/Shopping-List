package com.cloudsheeptech.shoppinglist.fragments.recipe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.data.recipe.ApiDescription
import com.cloudsheeptech.shoppinglist.databinding.ReceiptDescriptionBinding

class RecipeDescriptionAdapter : ListAdapter<ApiDescription, RecipeDescriptionAdapter.DescriptionViewHolder>(ItemDiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): DescriptionViewHolder = DescriptionViewHolder.from(parent)

    override fun onBindViewHolder(
        holder: DescriptionViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    class DescriptionViewHolder private constructor(
        val binding: ReceiptDescriptionBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(description: ApiDescription) {
            binding.description = description
            val context = binding.root.context
            val evenColor = context.resources.getColor(R.color.app_gray_700, context.theme)
            val unevenColor = context.resources.getColor(R.color.app_gray_900, context.theme)
            val backgroundColor = if (bindingAdapterPosition % 2 == 0) evenColor else unevenColor
            binding.descriptionWrapperCard.setCardBackgroundColor(backgroundColor)
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): DescriptionViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ReceiptDescriptionBinding.inflate(layoutInflater, parent, false)
                return DescriptionViewHolder(binding)
            }
        }
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
