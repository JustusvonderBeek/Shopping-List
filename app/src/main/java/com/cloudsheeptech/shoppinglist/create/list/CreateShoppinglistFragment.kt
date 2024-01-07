package com.cloudsheeptech.shoppinglist.create.list

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.data.User
import com.cloudsheeptech.shoppinglist.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.databinding.FragmentCreateShoppinglistBinding
import com.cloudsheeptech.shoppinglist.list.ShoppinglistViewModel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class CreateShoppinglistFragment : Fragment() {

    private lateinit var binding : FragmentCreateShoppinglistBinding
    private val viewModel : CreateShoppinglistViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_create_shoppinglist, container, false)

//        val database = ShoppingListDatabase.getInstance(requireContext())
//        val viewModelFactory = CreateShoppinglistViewModelFactory(database)
        // Don't keep track of the state in the creation fragment, therefore destroy everything when we navigate away
//        viewModel = ViewModelProvider(this, viewModelFactory)[CreateShoppinglistViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel.navigateToCreatedList.observe(viewLifecycleOwner, Observer { navigate ->
            if (navigate > 0) {
                findNavController().navigate(CreateShoppinglistFragmentDirections.actionCreateShoppinglistFragmentToShoppinglist())
                viewModel.onCreatedListNavigated()
            }
        })

        viewModel.navigateBack.observe(viewLifecycleOwner, Observer { navigate ->
            if (navigate) {
                viewModel.onBackNavigated()
                findNavController().navigateUp()
            }
        })

        return binding.root
    }

}