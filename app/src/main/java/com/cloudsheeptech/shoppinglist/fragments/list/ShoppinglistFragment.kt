package com.cloudsheeptech.shoppinglist.fragments.list

import android.app.AlertDialog
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.data.SwipeToDeleteHandler
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.databinding.FragmentListBinding
import com.cloudsheeptech.shoppinglist.fragments.recipe.RecipeViewModel

class ShoppinglistFragment : Fragment(), MenuProvider {

    private lateinit var binding : FragmentListBinding
    private lateinit var viewModel : ShoppinglistViewModel
    private val learningViewModel : RecipeViewModel by activityViewModels()

    val args : ShoppinglistFragmentArgs by navArgs()

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.list_drop_down_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.share_list -> {
                viewModel.shareThisList()
                return true
            }
            R.id.delete_list -> {
                viewModel.deleteThisList()
                return true
            }
            R.id.clear_items_list -> {
                viewModel.clearAllCheckedItems()
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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_list, container, false)

        // Adding the dropdown menu in the toolbar
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        var shoppingListId = args.ListID
        Log.d("ShoppinglistFragment", "Navigated to list with ID $shoppingListId")
        if (shoppingListId < 0)
            findNavController().navigateUp()

        val database = ShoppingListDatabase.getInstance(requireContext())
        val viewModelFactory = ShoppingListViewModelFactory(database, shoppingListId)

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
            viewModel.toggleItem(itemId.toLong())
        }, resources, database.mappingDao())
        // The adapter for the preview items
        val previewAdapter = ItemPreviewAdapter(ItemPreviewAdapter.ItemPreviewClickListener { itemId ->
            Log.d("ShoppinglistFragment", "Got preview ID $itemId")
            viewModel.AddTappedItem(itemId)
            viewModel.clearItemPreview()
        })
        binding.itemList.adapter = adapter
        binding.shoppingItemSelectView.adapter = previewAdapter

        // Allow removing item with swipe
        val deleteHelper = ItemTouchHelper(SwipeToDeleteHandler(adapter))
        deleteHelper.attachToRecyclerView(binding.itemList)

        viewModel.itemsInList.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it)
            }
        })

        viewModel.itemName.observe(viewLifecycleOwner, Observer { name ->
            name?.let {
                viewModel.showItemPreview(name)
            }
        })

        viewModel.previewItems.observe(viewLifecycleOwner, Observer { list ->
            list?.let {
                Log.d("ShoppinglistFragment", "New list (${list.size}) observed")
                if (list.isNotEmpty()) {
                    binding.shoppingItemSelectView.visibility = View.VISIBLE
                }
                if (list.isEmpty()) {
                    binding.shoppingItemSelectView.visibility = View.GONE
                }
                previewAdapter.submitList(list)
//                previewAdapter.notifyDataSetChanged()
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
//                adapter.notifyDataSetChanged()
                binding.refreshLayout.isRefreshing = false
            }
        })

        viewModel.navigateUp.observe(viewLifecycleOwner, Observer { navigate ->
            if (navigate) {
                findNavController().navigateUp()
                viewModel.onUpNavigated()
            }
        })

        viewModel.hideKeyboard.observe(viewLifecycleOwner, Observer { hide ->
            if (hide) {
                val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(requireView().windowToken, 0)
                viewModel.keyboardHidden()
            }
        })

        viewModel.title.observe(viewLifecycleOwner, Observer { title ->
            title?.let {
                requireActivity().actionBar?.title = "Bla"
            }
        })

        viewModel.navigateShare.observe(viewLifecycleOwner, Observer { listId ->
            if (listId > 0) {
                findNavController().navigate(ShoppinglistFragmentDirections.actionShoppinglistToShareFragment(listId))
                viewModel.onShareNavigated()
            }
        })

        val confirmClearDialog = AlertDialog.Builder(context)
            .setMessage("Do you really want to clear all checked items?")
            .setTitle("Confirm Clear All!")
            .setPositiveButton("Yes, clear") { dialog, which ->
                viewModel.onClearAllItemsPositiv()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                viewModel.onClearAllItemsNegative()
            }
            .create()

        val confirmDeleteDialog = AlertDialog.Builder(context)
            .setMessage("Do you really want to delete this list?")
            .setTitle("Delete List!")
            .setPositiveButton("Yes, delete") { dialog, which ->
                viewModel.onDeleteConfirmed()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                viewModel.onDeleteCanceled()
            }
            .create()

        viewModel.confirmClear.observe(viewLifecycleOwner, Observer {
            if (it) {
                confirmClearDialog.show()
            }
        })

        viewModel.confirmDelete.observe(viewLifecycleOwner, Observer {
            if (it) {
                confirmDeleteDialog.show()
            }
        })

        return binding.root
    }
}