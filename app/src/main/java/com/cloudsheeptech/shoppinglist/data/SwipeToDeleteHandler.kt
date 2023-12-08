package com.cloudsheeptech.shoppinglist.data

import android.util.Log
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.shoppinglist.list.ShoppingListItemAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SwipeToDeleteHandler(private val adapter: ShoppingListItemAdapter) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val job = Job()
    private val swipeHandlerScope = CoroutineScope(Dispatchers.Main + job)

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
//        adapter.deleteItemAt(viewHolder.adapterPosition)
        Log.i("SwipeToDeleteHandler", "Position is ${viewHolder.adapterPosition}")
        val position = viewHolder.adapterPosition
        swipeHandlerScope.launch {
            adapter.deleteItemAt(position)
            adapter.notifyItemRemoved(position)
        }
    }

}