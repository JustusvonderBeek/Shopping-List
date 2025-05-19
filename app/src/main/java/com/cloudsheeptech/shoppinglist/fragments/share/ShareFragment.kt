package com.cloudsheeptech.shoppinglist.fragments.share

import android.os.Bundle
import android.util.Log
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
import com.cloudsheeptech.shoppinglist.databinding.FragmentShareBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareFragment : Fragment() {
    private lateinit var binding: FragmentShareBinding
    private val viewModel: ShareViewModel by viewModels()

    companion object {
        fun newInstance() = ShareFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_share, container, false)

        val navArgs by navArgs<ShareFragmentArgs>()
        savedInstanceState?.putLong("listId", navArgs.listId)
        savedInstanceState?.putLong("createdBy", navArgs.createdBy)
        savedInstanceState?.putLong("recipeId", navArgs.recipeId)

        viewModel.setTitle(navArgs.title)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        val adapter =
            UserShareAdapter(
                UserShareAdapter.UserShareClickListener { userId ->
                    Log.d("ShareFragment", "Clicked on user $userId")
                    viewModel.share(userId)
                },
                UserShareAdapter.UserShareClickListener { userId ->
                    Log.d("ShareFragment", "Clicked on unshare user $userId")
                    viewModel.unshare(userId)
                },
            )
        binding.userPreviewList.adapter = adapter

        viewModel.combinedUsers.observe(
            viewLifecycleOwner,
            Observer { users ->
                users.let {
                    Log.d("ShareFragment", "Got list with ${users.size} users")
                    adapter.submitList(users)
                    adapter.notifyDataSetChanged()
                }
            },
        )

//        viewModel.searchedUsers.observe(viewLifecycleOwner, Observer { users ->
//            users.let {
//                Log.d("ShareFragment", "Got list with ${users.size} users")
//                adapter.submitList(users)
//            }
//        })

        viewModel.searchString.observe(
            viewLifecycleOwner,
            Observer { name ->
                Log.d("ShareFragment", "Got search query $name")
                viewModel.searchUser()
            },
        )

        viewModel.unshareable.observe(
            viewLifecycleOwner,
            Observer { unshareable ->
                when (unshareable) {
                    SharePlaceholderEnum.SHAREABLE -> {
                        binding.unshareableListPlaceholder.visibility = ViewGroup.GONE
                        binding.unshareableRecipePlaceholder.visibility = ViewGroup.GONE
                        binding.shareSearchUserText.visibility = ViewGroup.VISIBLE
                        binding.userPreviewList.visibility = ViewGroup.VISIBLE
                    }

                    SharePlaceholderEnum.RECIPE_UNSHAREABLE -> {
                        binding.unshareableRecipePlaceholder.visibility = ViewGroup.VISIBLE
                        binding.unshareableListPlaceholder.visibility = ViewGroup.GONE
                        binding.shareSearchUserText.visibility = ViewGroup.GONE
                        binding.userPreviewList.visibility = ViewGroup.GONE
                    }

                    SharePlaceholderEnum.LIST_UNSHAREABLE -> {
                        binding.unshareableRecipePlaceholder.visibility = ViewGroup.GONE
                        binding.unshareableListPlaceholder.visibility = ViewGroup.VISIBLE
                        binding.shareSearchUserText.visibility = ViewGroup.GONE
                        binding.userPreviewList.visibility = ViewGroup.GONE
                    }
                }
            },
        )

        viewModel.navigateUp.observe(
            viewLifecycleOwner,
            Observer { navigate ->
                if (navigate) {
                    findNavController().navigateUp()
                    viewModel.onUpNavigated()
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
        val title = getString(R.string.share_fragment_name, viewModel.title.value)
        (activity as AppCompatActivity).supportActionBar?.title = title
    }
}
