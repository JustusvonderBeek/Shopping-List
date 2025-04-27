package com.cloudsheeptech.shoppinglist.fragments.edit.recipe

import android.content.Context
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
import com.cloudsheeptech.shoppinglist.fragments.recipe.RecipeImageAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecipeEditFragment : Fragment() {
    private val viewModel: RecipeEditViewModel by viewModels()
    private lateinit var binding: FragmentRecipeEditBinding

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
            if (uris.isEmpty()) {
                Log.d("ReceiptEditFragment", "No images selected")
                return@registerForActivityResult
            }
            viewModel.setImagesUris(uris)
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
        val recipeImageAdapter =
            RecipeImageAdapter(
                emptyList(),
                RecipeImageAdapter.RecipeImageClickListener {
                    viewModel.selectImages()
                },
            )
        binding.viewPager.adapter = recipeImageAdapter
        binding.dotsIndicator.attachTo(binding.viewPager)

        // Listen to the result of the camera fragment which gives us a list
        // of image uris to all new images taken
        parentFragmentManager.setFragmentResultListener(
            "captured_image_uris",
            viewLifecycleOwner,
        ) { _, bundle ->
            val images = bundle.getStringArrayList("uris") ?: emptyList<String>()
            viewModel.setImages(images)
        }

        viewModel.receiptIngredients.observe(
            viewLifecycleOwner,
            Observer { items ->
                receiptIngredientAdapter.submitList(items)
                // Necessary to update quantity correctly
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
//                    binding.imageCarousel.setData(images)
                    recipeImageAdapter.updateImages(images)
                } else {
                    val defaultList =
                        listOf(
                            "file:///data/user/0/com.cloudsheeptech.shoppinglist/files/22_262053270_0.png",
                            "file:///data/user/0/com.cloudsheeptech.shoppinglist/files/7_1006887184_0.png",
                        )
                    recipeImageAdapter.updateImages(defaultList)
//                    binding.imageCarousel.setData(listOf(CarouselItem(imageDrawable = R.drawable.receipt_stock)))
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
                    showImageSelectionDialog(requireContext())
                    viewModel.onImageSelected()
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

    private fun showImageSelectionDialog(context: Context) {
        val dialog = BottomSheetDialog(context)
        val view =
            LayoutInflater
                .from(context)
                .inflate(R.layout.image_bottom_dialog, null)
        dialog.setContentView(view)

        val takePhotoButton = view.findViewById<MaterialButton>(R.id.takeImageButton)
        takePhotoButton.setOnClickListener {
            dialog.dismiss()
            Log.d("ReceiptEditFragment", "Take photo button clicked")
            findNavController().navigate(RecipeEditFragmentDirections.actionReceiptEditFragmentToCameraFragment())
        }
        val selectPhotoButton = view.findViewById<MaterialButton>(R.id.selectImageButton)
        selectPhotoButton.setOnClickListener {
            dialog.dismiss()
            Log.d("ReceiptEditFragment", "Select photo button clicked")
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        dialog.show()
    }
}
