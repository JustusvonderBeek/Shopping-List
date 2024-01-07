package com.cloudsheeptech.shoppinglist.create.user

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.databinding.FragmentStartBinding

class StartFragment : Fragment() {

    private lateinit var binding : FragmentStartBinding
    private val viewModel : StartViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_start, container, false)

        requireActivity().actionBar?.setDisplayHomeAsUpEnabled(false)

        val viewModelFactory = StartViewModelFactory(requireActivity().application)
        val viewModel = ViewModelProvider(this, viewModelFactory)[StartViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.hideKeyboard.observe(viewLifecycleOwner, Observer { hide ->
            if (hide) {
                val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(requireView().windowToken, 0)
                viewModel.keyboardHidden()
            }
        })

        viewModel.user.observe(viewLifecycleOwner, Observer { user ->
            user?.let {
                findNavController().navigateUp()
            }
        })

        return binding.root
    }
}