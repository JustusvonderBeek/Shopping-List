package com.cloudsheeptech.shoppinglist.fragments.edit

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.databinding.FragmentEditBinding

class EditFragment : Fragment() {

    private lateinit var binding : FragmentEditBinding
    private lateinit var viewModel : EditViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_edit, container, false)

//        val vocabFile = File(requireActivity().applicationContext.filesDir, "vocabulary.json")
//        val vocabulary = Vocabulary.getInstance(vocabFile)
        val viewModelFactory = EditViewModelFactory()
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)[EditViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

//        try {
//            val navigationArgs = EditFragmentArgs.fromBundle(requireArguments())
//            viewModel.loadWord(navigationArgs.selectedId)
//        } catch (ex : IllegalArgumentException) {
//            Log.i("EditFragment", "Failed to load navargs!")
//            viewModel.loadWord(0)
//        }
//
//        viewModel.navigateUp.observe(viewLifecycleOwner, Observer {
//            if (it) {
//                viewModel.wordEdited()
//                findNavController().navigateUp()
//            }
//        })

        return binding.root
    }
}