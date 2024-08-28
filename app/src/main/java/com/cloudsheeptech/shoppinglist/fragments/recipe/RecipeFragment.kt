package com.cloudsheeptech.shoppinglist.fragments.recipe

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
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
import com.cloudsheeptech.shoppinglist.databinding.FragmentRecipeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecipeFragment : Fragment(), MenuProvider {

    private val viewModel: RecipeViewModel by viewModels()
    private lateinit var binding : FragmentRecipeBinding

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.list_drop_down_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.delete_list -> {
                viewModel.editWord()
                return true
            }
            R.id.share_list -> {
                viewModel.removeRecipe()
                return true
            }
        }
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_recipe, container, false)

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

//        val viewModelFactory = RecipeViewModelFactory()
//        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)[RecipeViewModel::class.java]
        binding.recipeVM = viewModel
        binding.lifecycleOwner = this

        viewModel.navigateToEdit.observe(viewLifecycleOwner, Observer { selectedId ->
            if (selectedId > 0) {
                findNavController().navigate(RecipeFragmentDirections.actionRecipesToShoppinglist())
                viewModel.navigatedToEditWord()
            }
        })

//        viewModel..observe(viewLifecycleOwner, Observer { url ->
//            try {
//                Glide.with(this).load(url).into(binding.recipeImage)
//            } catch (ex : Exception) {
//                Log.i("LearningFragment", "Failed to load URL: $url")
//            }
//        })

        return binding.root
    }
}