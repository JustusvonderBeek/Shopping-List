package com.cloudsheeptech.shoppinglist.ui.list.create

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
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.databinding.FragmentCreateShoppingListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateShoppingListFragment : Fragment() {
    private lateinit var binding: FragmentCreateShoppingListBinding
    private val viewModel: CreateShoppingListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_create_shopping_list,
                container,
                false,
            )

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel.navigateToEditTitle.observe(
            viewLifecycleOwner,
            Observer { editTitle ->
                if (editTitle) {
                    binding.createListButton.text = getString(R.string.create_list_fragment_title)
                }
            },
        )

        viewModel.navigateToCreatedList.observe(
            viewLifecycleOwner,
            Observer { navigate ->
                if (navigate > 0) {
                    findNavController().navigate(CreateShoppingListFragmentDirections.actionCreateShoppinglistFragmentToShoppinglist())
                    viewModel.onCreatedListNavigated()
                }
            },
        )

        viewModel.navigateBack.observe(
            viewLifecycleOwner,
            Observer { navigate ->
                if (navigate != BackNavigation.NONE) {
                    viewModel.onBackNavigated()
                    if (navigate == BackNavigation.TO_OVERVIEW) {
                        findNavController().popBackStack(R.id.fragment_overview, false)
                    } else {
                        findNavController().navigateUp()
                    }
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
        if (viewModel.navigateToEditTitle.value != null && viewModel.navigateToEditTitle.value == true) {
            (activity as AppCompatActivity).supportActionBar?.title =
                getString(R.string.create_list_fragment_title)
        }
    }
}
