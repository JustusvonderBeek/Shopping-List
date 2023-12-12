package com.cloudsheeptech.shoppinglist.list_overview

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.data.ShoppingList
import com.cloudsheeptech.shoppinglist.databinding.FragmentListOverviewBinding
import com.cloudsheeptech.shoppinglist.datastructures.ItemListWithName

class ListOverviewFragment : Fragment() {

    private lateinit var binding : FragmentListOverviewBinding
    private val viewModel : ListOverviewViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_list_overview, container, false)
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

        return binding.root
    }

}