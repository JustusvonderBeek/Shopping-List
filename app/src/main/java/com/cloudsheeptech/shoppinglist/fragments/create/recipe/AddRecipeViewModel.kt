package com.cloudsheeptech.shoppinglist.fragments.create.recipe

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.SingleEvent
import com.cloudsheeptech.shoppinglist.data.Item
import com.cloudsheeptech.shoppinglist.datastructures.ItemListWithName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AddRecipeViewModel() : ViewModel() {

    private val job = Job()
    private val addVmScope = CoroutineScope(Dispatchers.IO + job)

    val itemListWithName = MutableLiveData<ItemListWithName<Item>>()

    val recipeName = MutableLiveData<String>()
    val recipeDescription = MutableLiveData<String>()

    private val handledToast = MutableLiveData<SingleEvent<String>>()
    val toast : LiveData<SingleEvent<String>>
        get() = handledToast

    fun addNewRecipe() {
        addVmScope.launch {
//            if (word.value != null && translation.value != null) {
//                vocabulary.postVocabulary(word.value!!, translation.value!!)
//                resetValues()
//            }
        }
    }
}