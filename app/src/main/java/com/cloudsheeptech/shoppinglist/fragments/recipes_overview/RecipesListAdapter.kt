package com.cloudsheeptech.shoppinglist.fragments.recipes_overview

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.data.recipe.DbRecipe
import com.cloudsheeptech.shoppinglist.data.recipe.RecipeImage
import com.cloudsheeptech.shoppinglist.databinding.RecipeOverviewItemBinding

class RecipesListAdapter(
    val clickListener: ReceiptClickListener,
) : ListAdapter<Pair<DbRecipe, List<RecipeImage>>, RecipesListAdapter.ReceiptListViewHolder>(
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
        val binding: RecipeOverviewItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            clickListener: ReceiptClickListener,
            recipeAndImage: Pair<DbRecipe, List<RecipeImage>>,
        ) {
            val recipe = recipeAndImage.first
            val imagePath = recipeAndImage.second
            binding.recipe = recipe
            binding.clickListener = clickListener
            if (imagePath.isEmpty()) {
                Log.d("ReceiptListViewHolder", "Loading default image")
                Glide
                    .with(binding.root.context)
                    .load(R.drawable.receipt_stock)
                    .into(binding.recipePreviewImage)
            } else {
                Glide
                    .with(binding.root.context)
                    .load(imagePath.get(0).fileLocation)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(binding.recipePreviewImage)
            }
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ReceiptListViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = RecipeOverviewItemBinding.inflate(layoutInflater, parent, false)
                return ReceiptListViewHolder(binding)
            }
        }
    }

    class ReceiptClickListener(
        val clickListener: (id: Long, from: Long, title: String) -> Unit,
    ) {
        fun onClick(item: DbRecipe) = clickListener(item.id, item.createdBy, item.name)
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<Pair<DbRecipe, List<RecipeImage>>>() {
        override fun areItemsTheSame(
            oldItem: Pair<DbRecipe, List<RecipeImage>>,
            newItem: Pair<DbRecipe, List<RecipeImage>>,
        ): Boolean = oldItem.first.id == newItem.first.id && oldItem.first.name == newItem.first.name

        override fun areContentsTheSame(
            oldItem: Pair<DbRecipe, List<RecipeImage>>,
            newItem: Pair<DbRecipe, List<RecipeImage>>,
        ): Boolean = oldItem.first == newItem.first && oldItem.second.size == newItem.second.size
    }
}
