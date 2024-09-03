package com.cloudsheeptech.shoppinglist.fragments.recipe

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.databinding.FragmentReceiptBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecipeFragment : Fragment(), MenuProvider {

    private val viewModel: RecipeViewModel by viewModels()
    private lateinit var binding : FragmentReceiptBinding

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
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
        }
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_receipt, container, false)

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.recipeVM = viewModel
        binding.lifecycleOwner = this

        val descriptionAdapter = ReceiptDescriptionAdapter()
        binding.receiptDescriptionListView.adapter = descriptionAdapter
        val ingredientAdapter = ReceiptIngredientAdapter()
        binding.receiptIngredientListView.adapter = ingredientAdapter

        viewModel.navigateToEdit.observe(viewLifecycleOwner, Observer { receiptIdAndCreatedBy ->
            val receiptId = receiptIdAndCreatedBy.first
            val createdBy = receiptIdAndCreatedBy.second
            if (receiptId > 0 && createdBy > 0) {
                findNavController().navigate(RecipeFragmentDirections.actionReceiptToReceiptEditFragment(receiptId, createdBy))
                viewModel.navigatedToEditWord()
            }
        })

        viewModel.navigateUp.observe(viewLifecycleOwner, Observer { navigate ->
            if (navigate) {
                findNavController().navigateUp()
                viewModel.onUpNavigated()
            }
        })

        viewModel.ingredients.observe(viewLifecycleOwner, Observer { x ->
            Log.d("RecipeFragment", "Ingredients: $x")
            ingredientAdapter.submitList(x)
            ingredientAdapter.notifyDataSetChanged()
        })

        viewModel.receipt.observe(viewLifecycleOwner, Observer { x ->
            descriptionAdapter.submitList(x.description)
        })

        return binding.root
    }
}