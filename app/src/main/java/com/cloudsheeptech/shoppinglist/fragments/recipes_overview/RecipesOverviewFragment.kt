package com.cloudsheeptech.shoppinglist.fragments.recipes_overview

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
import com.cloudsheeptech.shoppinglist.databinding.FragmentReceiptsOverviewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecipesOverviewFragment :
    Fragment(),
    MenuProvider {
    private lateinit var binding: FragmentReceiptsOverviewBinding
    private val viewModel: RecipesOverviewViewModel by viewModels()

    override fun onCreateMenu(
        menu: Menu,
        menuInflater: MenuInflater,
    ) {
        menuInflater.inflate(R.menu.receipts_drop_down_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.add_receipt -> {
                viewModel.createReceipt()
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
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_receipts_overview, container, false)

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        val adapter =
            RecipesListAdapter(
                RecipesListAdapter.ReceiptClickListener { id, from, title ->
                    Log.d("ReceiptsOverviewFragment", "Got receipt $id from $from")
                    viewModel.navigateToReceipt(id, from, title)
                },
            )
        binding.receiptOverviewList.adapter = adapter

        binding.receiptListOverviewRefresh.setOnRefreshListener {
            Log.d("ReceiptsOverviewFragment", "Refresh called")
            viewModel.updateAllRecipes()
        }

        viewModel.recipesWithImages.observe(
            viewLifecycleOwner,
            Observer { list ->
                list?.let { x ->
                    adapter.submitList(x)
                }
            },
        )

        viewModel.navigateToCreateReceipt.observe(
            viewLifecycleOwner,
            Observer { navigate ->
                if (navigate) {
                    findNavController().navigate(
                        RecipesOverviewFragmentDirections.actionReceiptsOverviewToReceiptEditFragment(
                            -1,
                            -1,
                        ),
                    )
                    viewModel.onCreateReceiptNavigate()
                }
            },
        )

        viewModel.navigateToReceipt.observe(
            viewLifecycleOwner,
            Observer { idAndfromAndTitle ->
                val id = idAndfromAndTitle.first
                val from = idAndfromAndTitle.second
                val title = idAndfromAndTitle.third
                if (id > 0L) {
                    findNavController().navigate(
                        RecipesOverviewFragmentDirections.actionReceiptsOverviewToReceipts(
                            receiptId = id,
                            createdBy = from,
                            title = title,
                        ),
                    )
                    viewModel.onReceiptNavigated()
                }
            },
        )

        viewModel.refreshing.observe(
            viewLifecycleOwner,
            Observer { refresh ->
                if (!refresh) {
                    binding.receiptListOverviewRefresh.isRefreshing = false
                }
            },
        )

        return binding.root
    }
}
