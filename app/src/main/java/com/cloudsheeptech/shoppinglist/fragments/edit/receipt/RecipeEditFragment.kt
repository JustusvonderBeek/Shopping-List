package com.cloudsheeptech.shoppinglist.fragments.edit.receipt

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.databinding.FragmentRecipeEditBinding
import dagger.hilt.android.AndroidEntryPoint
import org.imaginativeworld.whynotimagecarousel.listener.CarouselListener
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem

@AndroidEntryPoint
class RecipeEditFragment : Fragment() {
    private val viewModel: RecipeEditViewModel by viewModels()
    private lateinit var binding: FragmentRecipeEditBinding

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
            if (uris.isEmpty()) {
                Log.d("ReceiptEditFragment", "No images selected")
                return@registerForActivityResult
            }
            viewModel.setImages(uris)
            viewModel.onImageSelected()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_recipe_edit, container, false)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val receiptDescAdapter =
            RecipeDescriptionEditAdapter(
                RecipeDescriptionEditAdapter.ReceiptDescriptionEditClickListener { order ->
                    if (order >= 0) {
                        viewModel.deleteDescription(order)
                    }
                },
            )
        binding.descriptionRecyclerView.adapter = receiptDescAdapter

        val receiptIngredientAdapter =
            RecipeIngredientEditAdapter(
                RecipeIngredientEditAdapter.ReceiptIngredientEditClickListener { ingredient, quantity ->
                    viewModel.changeIngredientQuantity(ingredient, quantity)
                },
            )
        binding.itemRecyclerView.adapter = receiptIngredientAdapter
        binding.imageCarousel.registerLifecycle(viewLifecycleOwner)
        binding.imageCarousel.carouselListener =
            object : CarouselListener {
                override fun onClick(
                    position: Int,
                    carouselItem: CarouselItem,
                ) {
                    viewModel.selectImages()
                }
            }

        viewModel.receiptIngredients.observe(
            viewLifecycleOwner,
            Observer { items ->
                receiptIngredientAdapter.submitList(items)
                receiptIngredientAdapter.notifyDataSetChanged()
            },
        )

        viewModel.receiptDescription.observe(
            viewLifecycleOwner,
            Observer { descriptions ->
                receiptDescAdapter.submitList(descriptions)
            },
        )

        viewModel.images.observe(
            viewLifecycleOwner,
            Observer { images ->
                if (images.isNotEmpty()) {
                    binding.imageCarousel.setData(images)
                } else {
                    binding.imageCarousel.setData(listOf(CarouselItem(imageDrawable = R.drawable.receipt_stock)))
                }
            },
        )

        viewModel.store.observe(
            viewLifecycleOwner,
            Observer { store ->
                if (store) {
                    viewModel.storeUpdate()
                }
            },
        )

        viewModel.takeImage.observe(
            viewLifecycleOwner,
            Observer { takeImage ->
                if (takeImage) {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            },
        )

        viewModel.navigateUp.observe(
            viewLifecycleOwner,
            Observer { navigate ->
                if (navigate) {
                    findNavController().navigateUp()
                    viewModel.onUpNavigated()
                }
            },
        )

        return binding.root
    }
}
