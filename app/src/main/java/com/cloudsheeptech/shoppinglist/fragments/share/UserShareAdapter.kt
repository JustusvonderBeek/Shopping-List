package com.cloudsheeptech.shoppinglist.fragments.share

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.data.ListCreator
import com.cloudsheeptech.shoppinglist.data.ShareUserPreview
import com.cloudsheeptech.shoppinglist.databinding.UserSharePreviewBinding

class UserShareAdapter(val clickListener: UserShareClickListener, val unshareListener : UserShareClickListener) : ListAdapter<ShareUserPreview, UserShareAdapter.ListCreatorViewHolder>(
    UserShareDiffCallback()
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListCreatorViewHolder {
        return ListCreatorViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ListCreatorViewHolder, position: Int) {
        holder.bind(clickListener, unshareListener, getItem(position))
    }

    class ListCreatorViewHolder private constructor(val binding : UserSharePreviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(clickListener: UserShareClickListener, unshareListener: UserShareClickListener, item : ShareUserPreview) {
            binding.creator = item
            binding.clickListener = clickListener
            binding.unshareClickListener = unshareListener
            if (item.Shared) {
                binding.previewUnshareButton.visibility = View.VISIBLE
                binding.previewShareButton.visibility = View.GONE
            } else {
                // This is necessary because recyclerview reuses items and would show the wrong button
                // if we unshared and search again
                binding.previewShareButton.visibility = View.VISIBLE
                binding.previewUnshareButton.visibility = View.GONE
            }
//            if (item.ID % 2L == 0L)
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent : ViewGroup) : ListCreatorViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = UserSharePreviewBinding.inflate(layoutInflater, parent, false)
                return ListCreatorViewHolder(binding)
            }
        }
    }

    class UserShareClickListener(val clickListener: (id : Long) -> Unit) {
        fun onClick(creator : ShareUserPreview) = clickListener(creator.UserId)
    }

    class UserShareDiffCallback : DiffUtil.ItemCallback<ShareUserPreview>() {
        override fun areItemsTheSame(oldItem: ShareUserPreview, newItem: ShareUserPreview): Boolean {
            return oldItem.UserId == newItem.UserId && oldItem.Name == newItem.Name && oldItem.Shared == newItem.Shared
        }

        override fun areContentsTheSame(oldItem: ShareUserPreview, newItem: ShareUserPreview): Boolean {
            return oldItem == newItem
        }
    }
}