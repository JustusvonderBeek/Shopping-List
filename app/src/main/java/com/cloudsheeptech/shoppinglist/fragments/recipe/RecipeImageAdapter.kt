package com.cloudsheeptech.shoppinglist.fragments.recipe

import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

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
        Glide
            .with(holder.imageView.context)
            .load(path) // Or just `path` if it's a URL
            .into(holder.imageView)
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
