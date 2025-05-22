package com.cloudsheeptech.shoppinglist.fragments.recipe

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.databinding.FragmentRecipeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi

@AndroidEntryPoint
class RecipeFragment :
    Fragment(),
    MenuProvider {
    private val viewModel: RecipeViewModel by activityViewModels<RecipeViewModel>()
    private lateinit var binding: FragmentRecipeBinding

    private val args: RecipeFragmentArgs by navArgs<RecipeFragmentArgs>()

    override fun onCreateMenu(
        menu: Menu,
        menuInflater: MenuInflater,
    ) {
        menuInflater.inflate(R.menu.receipt_drop_down_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.delete_receipt -> {
                viewModel.removeRecipe()
                return true
            }

            R.id.edit_receipt -> {
                viewModel.editReceipt()
                return true
            }

            R.id.add_recipe_items -> {
                viewModel.addRecipeToShoppingList()
                return true
            }

            R.id.share_recipe -> {
                viewModel.shareRecipe()
                return true
            }
        }
        return false
    }

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_recipe, container, false)

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.recipeVM = viewModel
        binding.lifecycleOwner = this

        // In this case, we scope the viewModel differently because we need to share
        // data between two fragments. Therefore, we need to manually inject the args
        // into the state to be able to load the correct recipe
        viewModel.setRecipeIds(args.receiptId, args.createdBy)
        viewModel.setTitle(args.title)
        val portionsText = getString(R.string.recipe_portions_text)
        viewModel.setPortionsText(portionsText)

        val descriptionAdapter = RecipeDescriptionAdapter()
        binding.receiptDescriptionListView.adapter = descriptionAdapter
        val ingredientAdapter = RecipeIngredientAdapter()
        binding.receiptIngredientListView.adapter = ingredientAdapter
        val recipeImageAdapter =
            RecipeImageAdapter(
                emptyList<String>(),
                null,
            )
//        val recipeImageAdapterWithArrows = RecipeImageWithArrowsAdapter()
        binding.viewPager.adapter = recipeImageAdapter
//        binding.viewPager.adapter = recipeImageAdapterWithArrows

        binding.dotsIndicator.attachTo(binding.viewPager)

        viewModel.navigateToEdit.observe(
            viewLifecycleOwner,
            Observer { receiptIdAndCreatedBy ->
                val receiptId = receiptIdAndCreatedBy.first
                val createdBy = receiptIdAndCreatedBy.second
                if (receiptId > 0 && createdBy >= 0) {
                    findNavController().navigate(
                        RecipeFragmentDirections.actionReceiptToReceiptEditFragment(
                            receiptId,
                            createdBy,
                        ),
                    )
                    viewModel.navigatedToEditWord()
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

        viewModel.ingredientWithPortionsApplied.observe(
            viewLifecycleOwner,
            Observer { x ->
                Log.d("RecipeFragment", "Ingredients: $x")
                ingredientAdapter.submitList(x)
            },
        )

        viewModel.recipe.observe(
            viewLifecycleOwner,
            Observer { x ->
                descriptionAdapter.submitList(x.description)
            },
        )

        viewModel.images.observe(
            viewLifecycleOwner,
            Observer { images ->
                if (images.isNotEmpty()) {
                    recipeImageAdapter.updateImages(images)
//                    recipeImageAdapterWithArrows.submitList(images)
                } else {
                    val defaultList =
                        listOf(
                            "file:///data/user/0/com.cloudsheeptech.shoppinglist/files/1_262053270_0.png",
                        )
                    recipeImageAdapter.updateImages(
                        defaultList,
                    )
//                    recipeImageAdapterWithArrows.submitList(defaultList)
                }
            },
        )

        binding.refreshRecipeLayout.setOnRefreshListener {
            Log.d("RecipeFragment", "Refreshing recipe")
            viewModel.updateRecipe()
        }

        viewModel.refreshing.observe(
            viewLifecycleOwner,
            Observer { refreshing ->
                if (!refreshing) {
                    binding.refreshRecipeLayout.isRefreshing = false
                }
            },
        )

        val confirmDeleteDialog =
            AlertDialog
                .Builder(context)
                .setMessage(getString(R.string.recipe_confirm_delete))
                .setTitle(getString(R.string.recipe_confirm_delete_title))
                .setPositiveButton(getString(R.string.delete_list_dialog_yes)) { dialog, which ->
                    viewModel.onDeleteConfirmed()
                }.setNegativeButton(getString(R.string.delete_list_dialog_no)) { dialog, which ->
                    viewModel.onDeleteCanceled()
                }.create()

        viewModel.confirmDelete.observe(
            viewLifecycleOwner,
            Observer { delete ->
                if (delete) {
                    confirmDeleteDialog.show()
                } else {
                    confirmDeleteDialog.dismiss()
                }
            },
        )

        viewModel.navigateToSelectList.observe(
            viewLifecycleOwner,
            Observer { navigate ->
                if (navigate) {
                    findNavController().navigate(RecipeFragmentDirections.actionReceiptToListPickerFragment())
                    viewModel.onSelectListNavigated()
                }
            },
        )

        viewModel.navigateToShare.observe(
            viewLifecycleOwner,
            Observer { (recipeId, createdBy) ->
                if (recipeId > 0L && createdBy > 0L) {
                    findNavController().navigate(
                        RecipeFragmentDirections.actionReceiptToShareFragment(
                            recipeId = recipeId,
                            createdBy = createdBy,
                            title = viewModel.title.value!!,
                        ),
                    )
                    viewModel.onShareNavigated()
                }
            },
        )

        viewModel.toastMessage.observe(
            viewLifecycleOwner,
            Observer { listAndItems ->
                val list = listAndItems.first
                val items = listAndItems.second
                if (list.isNotEmpty() && items > 0) {
                    val toastString = resources.getString(R.string.recipe_add_items_finish_toast)
                    val formattedString = String.format(toastString, items, list)
                    Toast.makeText(context, formattedString, Toast.LENGTH_LONG).show()
                    viewModel.onToastMessageShown()
                }
            },
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.title = viewModel.title.value
    }
}
