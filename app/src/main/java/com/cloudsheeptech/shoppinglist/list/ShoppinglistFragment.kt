package com.cloudsheeptech.shoppinglist.list

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.data.SwipeToDeleteHandler
import com.cloudsheeptech.shoppinglist.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.databinding.FragmentShoppinglistBinding
import com.cloudsheeptech.shoppinglist.datastructures.ItemListWithName
import com.cloudsheeptech.shoppinglist.recipe.RecipeViewModel

class ShoppinglistFragment : Fragment(), MenuProvider {

    private lateinit var binding : FragmentShoppinglistBinding
    private lateinit var viewModel : ShoppinglistViewModel
    private val learningViewModel : RecipeViewModel by activityViewModels()

    val args : ShoppinglistFragmentArgs by navArgs()

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.list_dropdown, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.dd_edit_btn -> {
//                viewModel.navigateToAddWord()
                return true
            }
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_shoppinglist, container, false)

        // Adding the dropdown menu in the toolbar
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val shoppingListId = args.ListID

        val database = ShoppingListDatabase.getInstance(requireContext())
        val itemListWithName = ItemListWithName<Item>()
        val viewModelFactory = ShoppingListViewModelFactory(itemListWithName, database, shoppingListId)

        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)[ShoppinglistViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        val adapter = ShoppingListItemAdapter(ShoppingListItemAdapter.ShoppingItemClickListener { itemId ->
            Log.i("EditFragment", "Tapped on item with ID $itemId")
            viewModel.editWord(itemId)
        }, resources)
        binding.vocabList.adapter = adapter
        // Allow removing item with swipe
        val deleteHelper = ItemTouchHelper(SwipeToDeleteHandler(adapter))
        deleteHelper.attachToRecyclerView(binding.vocabList)

        viewModel.shoppinglist.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it)
                adapter.notifyDataSetChanged()
            }
        })

        binding.refreshLayout.setOnRefreshListener {
            Log.i("EditFragment", "On refresh called")
            viewModel.updateVocabulary()
        }

        viewModel.refreshing.observe(viewLifecycleOwner, Observer {
            if (!it) {
                Log.i("EditFragment", "Refreshing finished")
                adapter.notifyDataSetChanged()
                binding.refreshLayout.isRefreshing = false
            }
        })

//        viewModel.navigateToAdd.observe(viewLifecycleOwner, Observer { navigate ->
//            if (navigate) {
//                findNavController().navigate(EditlistFragmentDirections.actionEditToAdd())
//                viewModel.onAddWordNavigated()
//            }
//        })
//
//        viewModel.navigateToEdit.observe(viewLifecycleOwner, Observer {selected ->
//            if (selected > -1) {
//                findNavController().navigate(EditlistFragmentDirections.actionEditToEditFragment(selected))
//                viewModel.onEditWordNavigated()
//            }
//        })

        return binding.root
    }
}