package com.cloudsheeptech.shoppinglist.fragments.recipes_overview

import androidx.fragment.app.viewModels
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.databinding.FragmentReceiptsOverviewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecipesOverviewFragment : Fragment(), MenuProvider {

    private lateinit var binding : FragmentReceiptsOverviewBinding
    private val viewModel : RecipesOverviewViewModel by viewModels()

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_receipts_overview, container, false)

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        val adapter = RecipesListAdapter(RecipesListAdapter.ReceiptClickListener { id, from ->
            Log.d("ReceiptsOverviewFragment", "Got receipt $id from $from")
            viewModel.navigateToReceipt(id, from)
        }, requireActivity().resources)
        binding.receiptOverviewList.adapter = adapter

        viewModel.receipts.observe(viewLifecycleOwner, Observer { list ->
            list?.let { x ->
                adapter.submitList(x)
            }
        })

        viewModel.navigateToCreateReceipt.observe(viewLifecycleOwner, Observer { navigate ->
            if (navigate) {
                findNavController().navigate(RecipesOverviewFragmentDirections.actionReceiptsOverviewToReceiptEditFragment(-1, -1))
                viewModel.onCreateReceiptNavigate()
            }
        })

        viewModel.navigateToReceipt.observe(viewLifecycleOwner, Observer { idAndfrom ->
            val id = idAndfrom.first
            val from = idAndfrom.second
            if (id > 0L) {
                findNavController().navigate(RecipesOverviewFragmentDirections.actionReceiptsOverviewToReceipts(id, from))
                viewModel.onReceiptNavigated()
            }
        })

        return binding.root
    }
}