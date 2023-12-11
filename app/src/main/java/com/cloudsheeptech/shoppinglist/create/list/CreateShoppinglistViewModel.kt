package com.cloudsheeptech.shoppinglist.create.list

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.datastructures.ItemListWithName

class CreateShoppinglistViewModel : ViewModel() {

    val title = MutableLiveData<String>("")
    val description = MutableLiveData<String>("")
    val creator = MutableLiveData<Long>()
    val image = MutableLiveData<String>()
    val items = ItemListWithName<Item>()
    val list_id = MutableLiveData<Long>()

    private val _navigateBack = MutableLiveData<Boolean>(false)
    val navigateBack : LiveData<Boolean> get() = _navigateBack

    private val _navigateToCreatedList = MutableLiveData<Long>(-1)
    val navigateToCreatedList : LiveData<Long> get() = _navigateToCreatedList

    fun create() {
        Log.d("CreateShoppinglistViewModel", "Creating list pressed")

    }

    fun navigateBack() {
        _navigateBack.value = true
    }

    fun onBackNavigated() {
        _navigateBack.value = false
    }

    fun navigateCreatedList() {
        _navigateToCreatedList.value = 1
    }

    fun onCreatedListNavigated() {
        _navigateToCreatedList.value = -1
    }
}