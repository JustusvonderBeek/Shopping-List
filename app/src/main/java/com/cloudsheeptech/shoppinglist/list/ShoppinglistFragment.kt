package com.cloudsheeptech.shoppinglist.list

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
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
            R.id.dd_delete_btn -> {
                viewModel.clearAll()
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
//        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        var shoppingListId = args.ListID
        Log.d("ShoppinglistFragment", "Navigated to list with ID $shoppingListId")
        if (shoppingListId < 0)
            findNavController().navigateUp()

        val database = ShoppingListDatabase.getInstance(requireContext())
        val itemListWithName = ItemListWithName<Item>()
        val viewModelFactory = ShoppingListViewModelFactory(itemListWithName, database, shoppingListId)

        viewModel = ViewModelProvider(this, viewModelFactory)[ShoppinglistViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = requireActivity()

        val adapter = ShoppingListItemAdapter(ShoppingListItemAdapter.ShoppingItemClickListener { itemId, count ->
            Log.i("EditFragment", "Tapped on item $itemId to increase count")
            if (count > 0)
                viewModel.increaseItemCount(itemId)
            else if (count < 0)
                viewModel.decreaseItemCount(itemId)
        }, ShoppingListItemAdapter.ShoppingItemCheckboxClickListener { itemId ->
            Log.d("ShoppinglistFragment", "Tapped on item $itemId to toggle checkbox")
            viewModel.checkItem(itemId)
        }, resources, database.mappingDao())
        // The adapter for the preview items
        val previewAdapter = ItemPreviewAdapter(ItemPreviewAdapter.ItemPreviewClickListener { itemId ->
            Log.d("ShoppinglistFragment", "Got preview ID $itemId")
            viewModel.addTappedItem(itemId)
            viewModel.clearItemPreview()
        })
        binding.itemList.adapter = adapter
        binding.shoppingItemSelectView.adapter = previewAdapter

        // Allow removing item with swipe
        val deleteHelper = ItemTouchHelper(SwipeToDeleteHandler(adapter))
        deleteHelper.attachToRecyclerView(binding.itemList)


        viewModel.mappedItemIds.observe(viewLifecycleOwner, Observer {
            it?.let {
                // The received list is not empty
                // Use the updated mapping to select the items that are in the list
                viewModel.reloadItemsInList(it)
            }
        })

        viewModel.shoppinglist.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it)
                adapter.notifyDataSetChanged()
            }
        })

        viewModel.itemName.observe(viewLifecycleOwner, Observer {  name ->
            name?.let {
                viewModel.showItemPreview(name)
            }
        })

        viewModel.previewItems.observe(viewLifecycleOwner, Observer { list ->
            list?.let {
                Log.d("ShoppinglistFragment", "New list (${list.size}) observed")
                previewAdapter.submitList(list)
                previewAdapter.notifyDataSetChanged()
            }
        })

        viewModel.listInformation.observe(viewLifecycleOwner, Observer { info ->
            if (info != null) {
                requireActivity().title = info.Name
            }
        })

        binding.refreshLayout.setOnRefreshListener {
            Log.i("EditFragment", "On refresh called")
            viewModel.updateShoppinglist()
        }

        viewModel.refreshing.observe(viewLifecycleOwner, Observer {
            if (!it) {
                Log.i("EditFragment", "Refreshing finished")
                adapter.notifyDataSetChanged()
                binding.refreshLayout.isRefreshing = false
            }
        })

        viewModel.navigateToAdd.observe(viewLifecycleOwner, Observer { navigate ->
            if (navigate) {
                findNavController().navigate(ShoppinglistFragmentDirections.actionEditToEditFragment(-1))
                viewModel.onAddWordNavigated()
            }
        })

        viewModel.hideKeyboard.observe(viewLifecycleOwner, Observer { hide ->
            if (hide) {
                val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(requireView().windowToken, 0)
                viewModel.keyboardHidden()
            }
        })

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