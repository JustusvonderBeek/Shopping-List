package com.cloudsheeptech.shoppinglist.fragments.edit.receipt

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.databinding.FragmentReceiptBinding
import com.cloudsheeptech.shoppinglist.databinding.FragmentReceiptEditBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReceiptEditFragment : Fragment() {

    private val viewModel: ReceiptEditViewModel by viewModels()
    private lateinit var binding : FragmentReceiptEditBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_receipt_edit, container, false)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val receiptDescAdapter = ReceiptDescriptionEditAdapter(ReceiptDescriptionEditAdapter.ReceiptDescriptionEditClickListener { order ->
            if (order >= 0) {
                viewModel.deleteDescription(order)
            }
        })
        binding.descriptionRecyclerView.adapter = receiptDescAdapter

        val receiptIngredientAdapter = ReceiptIngredientEditAdapter(ReceiptIngredientEditAdapter.ReceiptIngredientEditClickListener { id ->
            if (id > 0) {
                viewModel.deleteIngredient(id)
            }
        })
        binding.itemRecyclerView.adapter = receiptIngredientAdapter

        viewModel.receiptIngredients.observe(viewLifecycleOwner, Observer { items ->
            receiptIngredientAdapter.submitList(items)
        })

        viewModel.receiptDescription.observe(viewLifecycleOwner, Observer { descriptions ->
            receiptDescAdapter.submitList(descriptions)
        })

        viewModel.store.observe(viewLifecycleOwner, Observer { store ->
            if (store) {
                viewModel.storeUpdate()
            }
        })

        viewModel.navigateUp.observe(viewLifecycleOwner, Observer { navigate ->
            if (navigate) {
                findNavController().navigateUp()
                viewModel.onUpNavigated()
            }
        })

        return binding.root
    }
}