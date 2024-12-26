package com.cloudsheeptech.shoppinglist.fragments.create.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.databinding.FragmentCreateShoppinglistBinding
import com.cloudsheeptech.shoppinglist.fragments.list.ShoppinglistFragmentArgs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateShoppinglistFragment : Fragment() {
    private lateinit var binding: FragmentCreateShoppinglistBinding
    private val viewModel: CreateShoppinglistViewModel by viewModels()

    val args: ShoppinglistFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_create_shoppinglist, container, false)

//        val database = ShoppingListDatabase.getInstance(requireContext())
//        val viewModelFactory = CreateShoppinglistViewModelFactory(database)
        // Don't keep track of the state in the creation fragment, therefore destroy everything when we navigate away
//        viewModel = ViewModelProvider(this, viewModelFactory)[CreateShoppinglistViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel.editTitle.observe(
            viewLifecycleOwner,
            Observer { editTitle ->
                if (editTitle) {
                    binding.createListButton.text = "Update List Title"
                }
            },
        )

        viewModel.navigateToCreatedList.observe(
            viewLifecycleOwner,
            Observer { navigate ->
                if (navigate > 0) {
                    findNavController().navigate(CreateShoppinglistFragmentDirections.actionCreateShoppinglistFragmentToShoppinglist())
                    viewModel.onCreatedListNavigated()
                }
            },
        )

        viewModel.navigateBack.observe(
            viewLifecycleOwner,
            Observer { navigate ->
                if (navigate) {
                    viewModel.onBackNavigated()
                    findNavController().navigateUp()
                }
            },
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (viewModel.editTitle.value != null && viewModel.editTitle.value == true) {
            (activity as AppCompatActivity).supportActionBar?.title = "Update List Title"
        }
    }
}
