package com.cloudsheeptech.shoppinglist.fragments.config

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.databinding.FragmentConfigBinding
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfigFragment : Fragment() {
    private lateinit var binding : FragmentConfigBinding
    private val configViewModel : ConfigViewModel by viewModels()

    companion object {
        fun newInstance() = ConfigFragment()
    }

    private val viewModel: ConfigViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_config, container, false)
        binding.viewModel = configViewModel
        binding.lifecycleOwner = this

        viewModel.buttonStatus.observe(viewLifecycleOwner, Observer { enable ->
            if (enable) {
                binding.toggleUserButton.background = requireContext().getDrawable(R.color.shopping_green)
            } else {
                binding.toggleUserButton.background = requireContext().getDrawable(R.color.shopping_red)
            }
        })

        return binding.root
    }
}