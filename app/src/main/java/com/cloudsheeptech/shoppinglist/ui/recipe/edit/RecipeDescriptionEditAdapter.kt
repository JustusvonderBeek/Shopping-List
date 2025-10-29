package com.cloudsheeptech.shoppinglist.ui.recipe.edit

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.databinding.ReceiptDescriptionEditTextBinding
import com.cloudsheeptech.shoppinglist.recipe.model.ApiDescription

class RecipeDescriptionEditAdapter(
    val clickListener: ReceiptDescriptionEditClickListener,
    val onLastItemFilled: () -> Unit,
) : ListAdapter<ApiDescription, RecipeDescriptionEditAdapter.DescriptionViewHolder>(ItemDiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): DescriptionViewHolder = DescriptionViewHolder.from(parent)

    override fun onBindViewHolder(
        holder: DescriptionViewHolder,
        position: Int,
    ) {
        holder.bind(clickListener, onLastItemFilled, getItem(position), this.itemCount)
    }

    class DescriptionViewHolder private constructor(
        val binding: ReceiptDescriptionEditTextBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            clickListener: ReceiptDescriptionEditClickListener,
            onLastItemFilled: () -> Unit,
            description: ApiDescription,
            totalItemCount: Int,
        ) {
            binding.description = description
            binding.clickListener = clickListener

            // Automatically add another item if the last item has non empty text
            binding.descriptionEditText.addTextChangedListener(
                object : TextWatcher {
                    override fun afterTextChanged(s: Editable) {
                        val pos = absoluteAdapterPosition
                        val content = binding.description?.step

                        if (pos == totalItemCount - 1 && content?.isNotEmpty() == true) {
//                            onLastItemFilled()
                        }
                        if (pos == totalItemCount - 1 && content?.isEmpty() == true) {
                        }
                    }

                    override fun onTextChanged(
                        s: CharSequence,
                        start: Int,
                        before: Int,
                        count: Int,
                    ) {}

                    override fun beforeTextChanged(
                        s: CharSequence,
                        start: Int,
                        count: Int,
                        after: Int,
                    ) {}
                },
            )

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
