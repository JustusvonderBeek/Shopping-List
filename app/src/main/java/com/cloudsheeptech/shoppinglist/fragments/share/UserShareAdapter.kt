package com.cloudsheeptech.shoppinglist.fragments.share

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.data.ListCreator
import com.cloudsheeptech.shoppinglist.databinding.UserSharePreviewBinding

class UserShareAdapter(val clickListener: UserShareClickListener) : ListAdapter<ListCreator, UserShareAdapter.ListCreatorViewHolder>(
    UserShareDiffCallback()
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListCreatorViewHolder {
        return ListCreatorViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ListCreatorViewHolder, position: Int) {
        holder.bind(clickListener, getItem(position))
    }

    class ListCreatorViewHolder private constructor(val binding : UserSharePreviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(clickListener: UserShareClickListener, item : ListCreator) {
            binding.creator = item
            binding.clickListener = clickListener
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
        fun onClick(creator : ListCreator) = clickListener(creator.ID)
    }

    class UserShareDiffCallback : DiffUtil.ItemCallback<ListCreator>() {
        override fun areItemsTheSame(oldItem: ListCreator, newItem: ListCreator): Boolean {
            return oldItem.ID == newItem.ID && oldItem.Name == newItem.Name
        }

        override fun areContentsTheSame(oldItem: ListCreator, newItem: ListCreator): Boolean {
            return oldItem == newItem
        }
    }
}