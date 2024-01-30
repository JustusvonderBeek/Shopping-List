package com.cloudsheeptech.shoppinglist.fragments.share

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.databinding.FragmentListOverviewBinding
import com.cloudsheeptech.shoppinglist.databinding.FragmentShareBinding

class ShareFragment : Fragment() {

    private lateinit var binding : FragmentShareBinding

    companion object {
        fun newInstance() = ShareFragment()
    }

    private lateinit var viewModel: ShareViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_share, container, false)

        val database = ShoppingListDatabase.getInstance(requireContext())
        val viewModel = ShareViewModel(database)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        return binding.root
    }

}