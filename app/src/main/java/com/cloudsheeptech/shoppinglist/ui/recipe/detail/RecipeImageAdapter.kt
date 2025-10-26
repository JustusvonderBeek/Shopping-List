package com.cloudsheeptech.shoppinglist.ui.recipe.detail

import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cloudsheeptech.shoppinglist.R

class RecipeImageAdapter(
    private var imagePaths: List<String>,
    val clickListener: RecipeImageClickListener?,
) : RecyclerView.Adapter<RecipeImageAdapter.ImageViewHolder>() {
    inner class ImageViewHolder(
        val imageView: ImageView,
    ) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ImageViewHolder {
        val imageView =
            ImageView(parent.context).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        return ImageViewHolder(imageView)
    }

    override fun onBindViewHolder(
        holder: ImageViewHolder,
        position: Int,
    ) {
        val path = imagePaths[position]
        Log.d("RecipeImageAdapter", "Binding image $path")
        if (path.isNotEmpty()) {
            Glide
                .with(holder.imageView.context)
                .load(path) // Or just `path` if it's a URL
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(holder.imageView)
        } else {
            // Prevent ugly scaling of Glide loading
            holder.imageView.setImageResource(R.drawable.ic_image)
        }
        holder.imageView.setOnClickListener {
            clickListener?.onClick()
        }
    }

    override fun getItemCount(): Int = imagePaths.size

    fun updateImages(newImages: List<String>) {
        imagePaths = newImages
        notifyDataSetChanged()
    }

    class RecipeImageClickListener(
        val clickListener: () -> Unit,
    ) {
        fun onClick() = clickListener()
    }
}
