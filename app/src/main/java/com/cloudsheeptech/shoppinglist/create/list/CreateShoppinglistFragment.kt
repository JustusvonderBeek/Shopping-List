package com.cloudsheeptech.shoppinglist.create.list

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.databinding.FragmentCreateShoppinglistBinding
import com.cloudsheeptech.shoppinglist.list.ShoppinglistViewModel

class CreateShoppinglistFragment : Fragment() {

    private lateinit var binding : FragmentCreateShoppinglistBinding
    private lateinit var viewModel : CreateShoppinglistViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_create_shoppinglist, container, false)

        val viewModelFactory = CreateShoppinglistViewModelFactory()
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)[CreateShoppinglistViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner


        return binding.root
    }

}