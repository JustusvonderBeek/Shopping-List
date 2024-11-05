package com.cloudsheeptech.shoppinglist.fragments.create.user

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.databinding.FragmentStartBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StartFragment : Fragment() {

    private lateinit var binding: FragmentStartBinding
    private val viewModel: StartViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as AppCompatActivity).supportActionBar?.show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_start, container, false)

        requireActivity().actionBar?.setDisplayHomeAsUpEnabled(false)

//        val viewModelFactory = StartViewModelFactory(requireActivity().application)
//        val viewModel = ViewModelProvider(this, viewModelFactory)[StartViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.hideKeyboard.observe(viewLifecycleOwner, Observer { hide ->
            if (hide) {
                val imm =
                    requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(requireView().windowToken, 0)
                viewModel.keyboardHidden()
            }
        })

        viewModel.disableButton.observe(viewLifecycleOwner) { disable ->
            binding.startButton.isClickable = !disable
        }

        viewModel.user.observe(viewLifecycleOwner, Observer { user ->
            user?.let {
                Log.d("StartFragment", "Navigating up...")
                findNavController().navigateUp()
            }
        })

        return binding.root
    }
}