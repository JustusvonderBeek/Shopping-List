package com.cloudsheeptech.shoppinglist.ui.recipe.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cloudsheeptech.shoppinglist.databinding.FragmentRecipeViewpagerWithArrowsBinding

class RecipeImageWithArrowsAdapter : ListAdapter<String, RecipeImageWithArrowsAdapter.RecipeImageWithArrowsViewHolder>(ItemDiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecipeImageWithArrowsViewHolder = RecipeImageWithArrowsViewHolder.from(parent)

    override fun onBindViewHolder(
        holder: RecipeImageWithArrowsViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    class RecipeImageWithArrowsViewHolder private constructor(
        val binding: FragmentRecipeViewpagerWithArrowsBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(imagePath: String) {
            val imageView = binding.scrollImageView
            Glide.with(binding.root.context).load(imagePath).into(imageView)
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): RecipeImageWithArrowsViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = FragmentRecipeViewpagerWithArrowsBinding.inflate(layoutInflater, parent, false)
                return RecipeImageWithArrowsViewHolder(binding)
            }
        }
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(
            oldItem: String,
            newItem: String,
        ): Boolean = oldItem == newItem

        override fun areContentsTheSame(
            oldItem: String,
            newItem: String,
        ): Boolean = oldItem.equals(newItem)
    }
}
