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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.databinding.FragmentListOverviewBinding

class ListOverviewFragment : Fragment(), MenuProvider {

    private lateinit var binding : FragmentListOverviewBinding
    private val viewModel : ListOverviewViewModel by activityViewModels()

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.list_dropdown, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.dd_edit_btn -> {
//                viewModel.navigateToAddWord()
                return true
            }
            R.id.dd_delete_btn -> {
                viewModel.removeUser()
                return true
            }
            R.id.dd_clear_btn -> {
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
        val adapter = ShoppingListAdapter(ShoppingListAdapter.ListClickListener { id ->
            Log.d("ListOverviewFragment", "Got ID $id")
            viewModel.navigateToShoppingList(id)
        }, requireActivity().resources, listOf())
        binding.listOverviewList.adapter = adapter

        viewModel.shoppingList.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it)
                adapter.notifyDataSetChanged()
            }
        })

        viewModel.createList.observe(viewLifecycleOwner, Observer { navigate ->
            if (navigate) {
                findNavController().navigate(ListOverviewFragmentDirections.actionOverviewToCreateShoppinglistFragment())
                viewModel.onCreateListNavigated()
            }
        })

        viewModel.navigateList.observe(viewLifecycleOwner, Observer { id ->
            if (id > 0L) {
                findNavController().navigate(ListOverviewFragmentDirections.actionOverviewToShoppinglist(id))
                viewModel.onShoppingListNavigated()
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