package com.cloudsheeptech.shoppinglist.datastructures

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cloudsheeptech.shoppinglist.data.Item

class Shoppinglist {

    private var _title = MutableLiveData<String>("Title")
    val title : LiveData<String>
        get() = _title

    private var _list = MutableLiveData<MutableList<Item>>()
    val list : LiveData<MutableList<Item>> get() = _list

    fun addItem(item : Item) {
        Log.d("Shoppinglist", "Adding new item {${item.ID}, ${item.Name}}")
        if (_list.value == null) {
            Log.d("Shoppinglist", "List initialized")
            _list.value = mutableListOf()
        }
        _list.value!!.add(item)
    }

    fun size() : Int {
        if (_list.value == null)
            return 0
        return _list.value!!.size
    }

    fun isEmpty() : Boolean {
        if (_list.value == null)
            return true
        return _list.value!!.isEmpty()
    }

}