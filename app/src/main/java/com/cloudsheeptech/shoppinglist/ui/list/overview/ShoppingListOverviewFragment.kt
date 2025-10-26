package com.cloudsheeptech.shoppinglist.ui.list.overview

import android.os.Bundle
import android.util.Log
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
import com.cloudsheeptech.shoppinglist.databinding.FragmentShoppingListOverviewBinding
import dagger.hilt.android.AndroidEntryPoint

// This is required for Hilt to inject the viewModel correctly
// See: https://developer.android.com/training/dependency-injection/hilt-jetpack
@AndroidEntryPoint
class ShoppingListOverviewFragment :
    Fragment(),
    MenuProvider {
    private lateinit var binding: FragmentShoppingListOverviewBinding
    private val viewModel: ShoppingListOverviewViewModel by viewModels() // Injected by hilt

    override fun onCreateMenu(
        menu: Menu,
        menuInflater: MenuInflater,
    ) {
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_shopping_list_overview,
                container,
                false,
            )

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        val adapter =
            ShoppingListOverviewListAdapter(
                ShoppingListOverviewListAdapter.ListClickListener { id, from, title ->
                    Log.d("ListOverviewFragment", "Got ID $id from $from called $title")
                    viewModel.navigateToShoppingList(id, from, title)
                },
            )
        binding.listOverviewList.adapter = adapter

        viewModel.allShoppingLists.observe(
            viewLifecycleOwner,
            Observer { list ->
                if (list.isNullOrEmpty()) {
                    binding.emptyListsPlaceholder.visibility = View.VISIBLE
                    binding.listOverviewList.visibility = View.GONE
                    adapter.submitList(emptyList())
                } else {
                    binding.emptyListsPlaceholder.visibility = View.GONE
                    binding.listOverviewList.visibility = View.VISIBLE
                    adapter.submitList(list)
                }
            },
        )

        viewModel.createList.observe(
            viewLifecycleOwner,
            Observer { navigate ->
                if (navigate) {
                    findNavController().navigate(
                        ShoppingListOverviewFragmentDirections.actionOverviewToCreateShoppinglistFragment(
                            null,
                            0L,
                        ),
                    )
                    viewModel.onCreateListNavigated()
                }
            },
        )

        viewModel.navigateToList.observe(
            viewLifecycleOwner,
            Observer { idAndFromAndTitle ->
                val id = idAndFromAndTitle.first
                val from = idAndFromAndTitle.second
                val title = idAndFromAndTitle.third
                if (id > 0L) {
                    findNavController().navigate(
                        ShoppingListOverviewFragmentDirections.actionOverviewToShoppinglist(
                            id,
                            from,
                            title,
                        ),
                    )
                    viewModel.onShoppingListNavigated()
                }
            },
        )

        viewModel.navigateToConfig.observe(
            viewLifecycleOwner,
            Observer { navigate ->
                if (navigate) {
                    findNavController().navigate(ShoppingListOverviewFragmentDirections.actionOverviewToConfigFragment())
                    viewModel.onConfigNavigated()
                }
            },
        )

        viewModel.user.observe(
            viewLifecycleOwner,
            Observer { user ->
                if (user == null) {
                    findNavController().navigate(ShoppingListOverviewFragmentDirections.actionOverviewToUsernameSelection())
                }
            },
        )

        binding.listOverviewRefresher.setOnRefreshListener {
            Log.d("ListOverviewFragment", "On refresh called")
            viewModel.updateAllLists()
        }

        viewModel.refreshing.observe(
            viewLifecycleOwner,
            Observer { refresh ->
                if (!refresh) {
                    binding.listOverviewRefresher.isRefreshing = false
                }
            },
        )

        return binding.root
    }
}
