package com.cloudsheeptech.shoppinglist.fragments.list_picker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.databinding.FragmentListPickerBinding
import com.cloudsheeptech.shoppinglist.fragments.list_overview.ShoppingListAdapter
import com.cloudsheeptech.shoppinglist.fragments.recipe.RecipeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ListPickerFragment : Fragment(), MenuProvider {

    private val viewModel: RecipeViewModel by viewModels()
    private lateinit var binding: FragmentListPickerBinding

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.list_picker_drop_down_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.add_list -> {
                viewModel.createList()
                return true
            }
        }
        return false
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_list_picker, container, false)

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val adapter =
            ShoppingListAdapter(ShoppingListAdapter.ListClickListener { listId, createdBy, title ->
                viewModel.selectList(listId, createdBy)
            })

        binding.listPickerRecyclerView.adapter = adapter

        viewModel.shoppingLists.observe(viewLifecycleOwner, Observer { list ->
            adapter.submitList(list)
            adapter.notifyDataSetChanged()
        })

        viewModel.navigateUp.observe(viewLifecycleOwner, Observer { navigate ->
            if (navigate) {
                findNavController().navigateUp()
                viewModel.onUpNavigated()
            }
        })

        viewModel.navigateToCreateList.observe(viewLifecycleOwner, Observer { navigate ->
            if (navigate) {
                findNavController().navigate(ListPickerFragmentDirections.actionListPickerFragmentToCreateShoppinglistFragment())
                viewModel.onCreateListNavigated()
            }
        })

        return binding.root
    }


}