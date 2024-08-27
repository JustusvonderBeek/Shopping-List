package com.cloudsheeptech.shoppinglist.fragments.share

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.databinding.FragmentShareBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareFragment : Fragment() {

    private lateinit var binding : FragmentShareBinding
    private val viewModel : ShareViewModel by viewModels()
    private val args: ShareFragmentArgs by navArgs()

    companion object {
        fun newInstance() = ShareFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_share, container, false)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        val adapter = UserShareAdapter(UserShareAdapter.UserShareClickListener { userId ->
            Log.d("ShareFragment", "Clicked on user $userId")
            viewModel.shareList(userId)
        }, UserShareAdapter.UserShareClickListener { userId ->
            Log.d("ShareFragment", "Clicked on unshare user $userId")
            viewModel.unshareListForUser(userId)
        })
        binding.userPreviewList.adapter = adapter

        viewModel.sharedPreview.observe(viewLifecycleOwner, Observer { users ->
            users.let {
                Log.d("ShareFragment", "Got list with ${users.size} users")
                adapter.submitList(users)
//                adapter.notifyDataSetChanged()
            }
        })

//        viewModel.searchedUsers.observe(viewLifecycleOwner, Observer { users ->
//            users.let {
//                Log.d("ShareFragment", "Got list with ${users.size} users")
//                adapter.submitList(users)
//            }
//        })

        viewModel.searchName.observe(viewLifecycleOwner, Observer { name ->
            Log.d("ShareFragment", "Got search query $name")
            viewModel.searchUser()
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