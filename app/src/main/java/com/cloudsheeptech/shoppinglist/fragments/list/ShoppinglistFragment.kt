package com.cloudsheeptech.shoppinglist.fragments.list

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import com.cloudsheeptech.shoppinglist.R
import com.cloudsheeptech.shoppinglist.data.SwipeToDeleteHandler
import com.cloudsheeptech.shoppinglist.databinding.FragmentListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShoppinglistFragment :
    Fragment(),
    MenuProvider,
    AdapterView.OnItemSelectedListener {
    private lateinit var binding: FragmentListBinding
    private val viewModel: ShoppinglistViewModel by viewModels()
//    private val learningViewModel : RecipeViewModel by activityViewModels()

    private var startTouchY = 0f
    private var startTranslationY = 0f

    val args: ShoppinglistFragmentArgs by navArgs()

    override fun onCreateMenu(
        menu: Menu,
        menuInflater: MenuInflater,
    ) {
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

            R.id.rename_list -> {
                viewModel.renameThisList()
                return true
            }

            R.id.clear_items_list -> {
                viewModel.clearAllCheckedItems()
                return true
            }
        }
        return false
    }

    // Below is for the spinner selection
    override fun onItemSelected(
        parent: AdapterView<*>,
        view: View?,
        pos: Int,
        id: Long,
    ) {
        val selected = parent.getItemAtPosition(pos)
        Log.d("ShoppingListFragment", "Got: $selected")
        viewModel.setOrderingInDatabase(selected as String, requireContext())
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Reset the order to default
        viewModel.resetOrdering()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_list, container, false)

        // Adding the dropdown menu in the toolbar
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val shoppingListId = args.ListID
        val createdBy = args.CreatedBy

        Log.d("ShoppinglistFragment", "Navigated to list with ID $shoppingListId from $createdBy")
        if (shoppingListId < 0) {
            findNavController().navigateUp()
        }
        if (createdBy < 0) {
            findNavController().navigateUp()
        }

        binding.viewModel = viewModel
        binding.lifecycleOwner = requireActivity()

        // TODO: Clean up this mess...
        val amountName = getString(R.string.list_item_amount_name)
        val adapter =
            ShoppingListItemAdapter(
                ShoppingListItemAdapter.ShoppingItemClickListener { itemId, count ->
                    Log.i("ShoppinglistFragment", "Tapped on item $itemId to increase count")
                    if (count > 0) {
                        viewModel.increaseItemCount(itemId)
                    } else if (count < 0) {
                        viewModel.decreaseItemCount(itemId)
                    }
                },
                ShoppingListItemAdapter.ShoppingItemCheckboxClickListener { itemId ->
                    Log.d("ShoppinglistFragment", "Tapped on item $itemId to toggle checkbox")
                    viewModel.toggleItem(itemId.toLong())
                },
                amountName,
                Pair(shoppingListId, createdBy),
                viewModel.shoppingListRepository,
            )
        // The adapter for the preview items
        val previewAdapter =
            ItemPreviewAdapter(
                ItemPreviewAdapter.ItemPreviewClickListener { itemId ->
                    Log.d("ShoppinglistFragment", "Got preview ID $itemId")
                    viewModel.addTappedItem(itemId)
                    viewModel.clearItemPreview()
                },
            )
        binding.itemList.adapter = adapter
        binding.shoppingItemSelectView.adapter = previewAdapter

        // Allow removing item with swipe
        val deleteHelper = ItemTouchHelper(SwipeToDeleteHandler(adapter))
        deleteHelper.attachToRecyclerView(binding.itemList)

        // Populate the ordering spinner with the pre-defined orderings
        val spinner = binding.orderSelectionSpinner
        ArrayAdapter
            .createFromResource(
                requireContext(),
                R.array.list_ordering_array,
                R.layout.ordering_spinner_item,
            ).also {
                it.setDropDownViewResource(R.layout.ordering_spinner_dropdown_item)
                spinner.adapter = it
            }
        spinner.onItemSelectedListener = this

        viewModel.orderedItemsInList.observe(
            viewLifecycleOwner,
            Observer {
                Log.d("ShoppingListFragment", "List changed")
                it?.let {
                    adapter.submitList(it)
                    // Necessary to apply the ordering directly
//                    adapter.notifyDataSetChanged()
                }
            },
        )

        viewModel.itemName.observe(
            viewLifecycleOwner,
            Observer { name ->
                name?.let {
                    viewModel.showItemPreview(name)
                }
            },
        )

        viewModel.previewItems.observe(
            viewLifecycleOwner,
            Observer { list ->
                list?.let {
//                Log.d("ShoppinglistFragment", "New list (${list.size}) observed")
                    if (list.isNotEmpty()) {
                        binding.shoppingItemSelectView.visibility = View.VISIBLE
                    }
                    if (list.isEmpty()) {
                        binding.shoppingItemSelectView.visibility = View.GONE
                    }
                    previewAdapter.submitList(list)
//                previewAdapter.notifyDataSetChanged()
                }
            },
        )

        viewModel.listInformation.observe(
            viewLifecycleOwner,
            Observer { info ->
                if (info != null) {
                    requireActivity().title = info.title
                }
            },
        )

        viewModel.preferences.observe(
            viewLifecycleOwner,
            Observer { p ->
                p?.let {
                    Log.d("ShoppingListFragment", "Found ordering: ${it.Ordering}")
                    viewModel.setOrdering(it.Ordering)
                    binding.orderSelectionSpinner.setSelection(it.Ordering.position)
                    Log.d("ShoppingListFragment", "${it.Ordering.position}")
                }
            },
        )

        binding.refreshLayout.setOnRefreshListener {
            Log.i("EditFragment", "On refresh called")
            viewModel.updateShoppinglist()
        }

        viewModel.refreshing.observe(
            viewLifecycleOwner,
            Observer {
                if (!it) {
                    Log.i("EditFragment", "Refreshing finished")
//                adapter.notifyDataSetChanged()
                    binding.refreshLayout.isRefreshing = false
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

        viewModel.hideKeyboard.observe(
            viewLifecycleOwner,
            Observer { hide ->
                if (hide) {
                    val imm =
                        requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(requireView().windowToken, 0)
                    viewModel.keyboardHidden()
                }
            },
        )

        viewModel.navigateShare.observe(
            viewLifecycleOwner,
            Observer { (listId, createdBy) ->
                if (listId > 0 && createdBy > 0) {
                    findNavController().navigate(
                        ShoppinglistFragmentDirections.actionShoppinglistToShareFragment(
                            listId = listId,
                            createdBy = createdBy,
                            title = viewModel.title.value!!,
                        ),
                    )
                    viewModel.onShareNavigated()
                }
            },
        )

        viewModel.allItemsChecked.observe(
            viewLifecycleOwner,
            Observer { checked ->
                Log.d("ShoppingListFragment", "Checked: $checked")
                // Expecting 1 if all are the same "COUNT" otherwise 0 or 2 because of boolean
                if (checked == 1 && !viewModel.finished.value!!) {
//                Toast.makeText(context, "All Items are checked", Toast.LENGTH_LONG).show()
                    binding.blurLayout.visibility = View.VISIBLE
                } else {
                    binding.blurLayout.visibility = View.GONE
                }
            },
        )

        viewModel.emptyList.observe(
            viewLifecycleOwner,
            Observer { empty ->
                if (empty) {
                    binding.refreshLayout.visibility = View.GONE
                    binding.emptyListPlaceholder.visibility = View.VISIBLE
                } else {
                    binding.refreshLayout.visibility = View.VISIBLE
                    binding.emptyListPlaceholder.visibility = View.GONE
                }
            },
        )

        viewModel.finished.observe(
            viewLifecycleOwner,
            Observer { clicked ->
                if (clicked) {
                    binding.blurLayout.visibility = View.GONE
                }
            },
        )

        viewModel.scrollDown.observe(
            viewLifecycleOwner,
            Observer { position ->
                if (position > 0) {
                    binding.itemList.scrollToPosition(position)
                    viewModel.onViewScrolledDown()
                }
            },
        )

        val confirmClearDialog =
            AlertDialog
                .Builder(context)
                .setMessage(getString(R.string.clear_check_item_dialog))
                .setTitle(getString(R.string.clear_item_dialog_title))
                .setPositiveButton(getString(R.string.clear_item_dialog_yes)) { dialog, which ->
                    viewModel.onClearAllItemsPositiv()
                }.setNegativeButton(getString(R.string.clear_item_dialog_no)) { dialog, which ->
                    viewModel.onClearAllItemsNegative()
                }.create()

        val confirmDeleteDialog =
            AlertDialog
                .Builder(context)
                .setMessage(getString(R.string.delete_list_dialog))
                .setTitle(getString(R.string.delete_list_dialog_title))
                .setPositiveButton(getString(R.string.delete_list_dialog_yes)) { dialog, which ->
                    viewModel.onDeleteConfirmed()
                }.setNegativeButton(getString(R.string.delete_list_dialog_no)) { dialog, which ->
                    viewModel.onDeleteCanceled()
                }.create()

        viewModel.confirmClear.observe(
            viewLifecycleOwner,
            Observer {
                if (it) {
                    confirmClearDialog.show()
                }
            },
        )

        viewModel.confirmDelete.observe(
            viewLifecycleOwner,
            Observer {
                if (it) {
                    confirmDeleteDialog.show()
                }
            },
        )

        viewModel.renameList.observe(
            viewLifecycleOwner,
            Observer { (rename, listId) ->
                if (rename.isNotEmpty() && listId > 0L) {
                    findNavController().navigate(
                        ShoppinglistFragmentDirections.actionShoppinglistToCreateShoppinglistFragment(
                            rename,
                            listId,
                        ),
                    )
                    viewModel.onListRenamed()
                }
            },
        )

        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.title = viewModel.title.value

        val maxDragUp = binding.bottomSheetLayout.height.toFloat() // Drag up max by height
        val maxTranslationY = maxDragUp - 30f
        binding.visualBoxDrawer.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startTouchY = event.rawY
                    startTranslationY = binding.bottomSheetLayout.translationY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dy = event.rawY - startTouchY
                    val newTranslationY = (startTranslationY + dy).coerceIn(0f, 2000f)

                    binding.bottomSheetLayout.translationY = newTranslationY
                    true
                }

                else -> false
            }
        }
    }

    private fun dpToPx(dp: Float): Float {
        val scale = resources.displayMetrics.density
        return dp * scale
    }
}
