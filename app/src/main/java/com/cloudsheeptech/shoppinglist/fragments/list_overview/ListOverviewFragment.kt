package com.cloudsheeptech.shoppinglist.fragments.list_overview

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.databinding.FragmentListOverviewBinding
import dagger.hilt.android.AndroidEntryPoint

// This is required for Hilt to inject the viewModel correctly
// See: https://developer.android.com/training/dependency-injection/hilt-jetpack
@AndroidEntryPoint
class ListOverviewFragment : Fragment(), MenuProvider {

    private lateinit var binding : FragmentListOverviewBinding
    private val viewModel : ListOverviewViewModel by viewModels() // Injected by hilt

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.overview_drop_down_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.add_list -> {
                viewModel.createNewList()
                return true
            }
            R.id.delete_user -> {
                viewModel.removeUser()
                return true
            }
            R.id.config -> {
                viewModel.navigateConfig()
                return true
            }
            R.id.clear_all_lists -> {
                viewModel.clearDatabase()
                return true
            }
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_list_overview, container, false)

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        val adapter = ShoppingListAdapter(ShoppingListAdapter.ListClickListener { id, from ->
            Log.d("ListOverviewFragment", "Got ID $id from $from")
            viewModel.navigateToShoppingList(id, from)
        }, requireActivity().resources, listOf())
        binding.listOverviewList.adapter = adapter

        viewModel.shoppingList.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it)
//                adapter.notifyDataSetChanged()
            }
        })

        viewModel.createList.observe(viewLifecycleOwner, Observer { navigate ->
            if (navigate) {
                findNavController().navigate(ListOverviewFragmentDirections.actionOverviewToCreateShoppinglistFragment())
                viewModel.onCreateListNavigated()
            }
        })

        viewModel.navigateList.observe(viewLifecycleOwner, Observer { idAndFrom ->
            val id = idAndFrom.first
            val from = idAndFrom.second
            if (id > 0L) {
                findNavController().navigate(ListOverviewFragmentDirections.actionOverviewToShoppinglist(id, from))
                viewModel.onShoppingListNavigated()
            }
        })

        viewModel.navigateConfig.observe(viewLifecycleOwner, Observer { navigate ->
            if (navigate) {
                findNavController().navigate(ListOverviewFragmentDirections.actionOverviewToConfigFragment())
                viewModel.onConfigNavigated()
            }
        })

        viewModel.user.observe(viewLifecycleOwner, Observer { user ->
            if (user == null) {
                findNavController().navigate(ListOverviewFragmentDirections.actionOverviewToUsernameSelection())
            }
        })

        binding.listOverviewRefresher.setOnRefreshListener {
            Log.d("ListOverviewFragment", "On refresh called")
            viewModel.updateAllLists()
        }

        viewModel.refreshing.observe(viewLifecycleOwner, Observer {  refresh ->
            if (!refresh) {
                binding.listOverviewRefresher.isRefreshing = false
            }
        })

        return binding.root
    }

}