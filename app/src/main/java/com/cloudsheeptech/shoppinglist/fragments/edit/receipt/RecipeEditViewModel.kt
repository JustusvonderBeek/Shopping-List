package com.cloudsheeptech.shoppinglist.fragments.edit.receipt

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.recipe.ApiDescription
import com.cloudsheeptech.shoppinglist.data.recipe.ApiIngredient
import com.cloudsheeptech.shoppinglist.data.recipe.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem
import javax.inject.Inject
import kotlin.math.max

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
@HiltViewModel
class RecipeEditViewModel
    @Inject
    constructor(
        private val recipeRepository: RecipeRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val job = Job()
        private val vmScope = CoroutineScope(Dispatchers.Main + job)

        private val receiptId: Long = savedStateHandle["receiptId"]!!
        private val createdBy: Long = savedStateHandle["createdBy"]!!

        private val _takeImage = MutableLiveData(false)
        val takeImage: LiveData<Boolean> get() = _takeImage
        private val _navigateUp = MutableLiveData(false)
        val navigateUp: LiveData<Boolean> get() = _navigateUp

        private val _store = MutableLiveData(false)
        val store: LiveData<Boolean> get() = _store

        @OptIn(InternalSerializationApi::class)
        private val recipe = recipeRepository.readLive(receiptId, createdBy)

        val title = MutableLiveData("")
        private val _images = MutableLiveData<List<CarouselItem>>(emptyList())
        val images: LiveData<List<CarouselItem>> get() = _images

        val receiptDescription = MutableLiveData<List<ApiDescription>>(emptyList())

        @Suppress("ktlint:standard:backing-property-naming")
        private val _receiptIngredientList = MutableLiveData<List<ApiIngredient>>(emptyList())
        val receiptIngredients: LiveData<List<ApiIngredient>> get() = _receiptIngredientList

        init {
            // The user should be able to modify the existing receipt, therefore load ingredients
            // and descriptions at the start
            if (receiptId != -1L && createdBy != -1L) {
                vmScope.launch {
                    val recipeAndImages = recipeRepository.read(receiptId, createdBy) ?: return@launch
                    val storedRecipe = recipeAndImages.first
                    Log.d("ReceiptEditViewModel", "Loaded: $storedRecipe")
                    withContext(Dispatchers.Main) {
                        receiptDescription.value = storedRecipe.description
                        _receiptIngredientList.value = storedRecipe.ingredients
                        title.value = storedRecipe.name
                    }
                }
            } else {
                Log.d("ReceiptEditViewModel", "Creating new receipt")
            }
            setupNewRecipe()
        }

        private fun setupNewRecipe() {
            if (_receiptIngredientList.value!!.isEmpty()) {
                addItem()
            }
            if (receiptDescription.value!!.isEmpty()) {
                addDescription()
            }
        }

        fun setImages(uris: List<Uri>) {
            Log.d("Got images", "$uris")
            val newImageList = mutableListOf<CarouselItem>()
            uris.forEach { uri ->
                newImageList.add(
                    CarouselItem(imageUrl = uri.toString()),
                )
            }
            _images.value = newImageList
        }

        fun addItem() {
            val newIngredient =
                ApiIngredient(
                    id = 0L,
                    name = "",
                    icon = "",
                    quantity = 1,
                    quantityType = "",
                )
            _receiptIngredientList.value = _receiptIngredientList.value!! + newIngredient
        }

        fun addDescription() {
            val emptyDescription =
                ApiDescription(
                    order = receiptDescription.value!!.size,
                    step = "",
                )
            receiptDescription.value = receiptDescription.value!! + emptyDescription
        }

        fun deleteDescription(order: Int) {
            Log.d("ReceiptEditViewModel", "Filtering $order description")
            receiptDescription.value = receiptDescription.value?.filter { x -> x.order != order }
        }

        fun changeIngredientQuantity(
            ingredient: Long,
            quantity: Int,
        ) {
            Log.d("ReceiptEditViewModel", "Changing quantity of ingredient $ingredient by $quantity")
            val changedIngredients =
                _receiptIngredientList.value?.map { ing ->
                    if (ing.id == ingredient) {
                        ing.quantity = max(1, ing.quantity.plus(quantity))
                    }
                    ing
                } ?: return
            _receiptIngredientList.value = changedIngredients
        }

        fun deleteIngredient(itemId: Long) {
            Log.d("ReceiptEditViewModel", "Filtering $itemId ingredient")
            _receiptIngredientList.value = receiptIngredients.value?.filter { x -> x.id != itemId }
        }

        private fun checkAddNewIngredientNecessary(): Boolean =
            _receiptIngredientList.value!!
                .last()
                .name
                .isNotEmpty()

        private fun checkAddNewDescriptionNecessary(): Boolean =
            receiptDescription.value!!
                .last()
                .step
                .isNotEmpty()

        fun storeUpdate() {
            if (title.value == null || title.value!!.isEmpty()) {
                Log.e("ReceiptEditViewModel", "Title cannot be empty!")
                // TODO: Make toast
                return
            }
            // Differentiate between completely new receipt and existing one
            val imageLocations = images.value?.map { image -> image.imageUrl ?: "" } ?: emptyList()
            if (receiptId == -1L && createdBy == -1L) {
                vmScope.launch {
                    val newReceipt = recipeRepository.create(title.value!!, 2, imageLocations)
                    newReceipt.ingredients =
                        _receiptIngredientList.value?.filter { x -> x.name.isNotEmpty() } ?: emptyList()
                    newReceipt.description =
                        receiptDescription.value?.filter { x -> x.step.isNotEmpty() } ?: emptyList()
                    recipeRepository.update(newReceipt, emptyList())
                    withContext(Dispatchers.Main) {
                        navigateUp()
                    }
                }
            } else {
                vmScope.launch {
                    val storedRecipeAndImages = recipeRepository.read(receiptId, createdBy) ?: return@launch
                    val updatedRecipe = storedRecipeAndImages.first
                    updatedRecipe.name = title.value ?: "Title"
                    updatedRecipe.ingredients =
                        _receiptIngredientList.value?.filter { x -> x.name.isNotEmpty() } ?: emptyList()
                    updatedRecipe.description =
                        receiptDescription.value?.filter { x -> x.step.isNotEmpty() } ?: emptyList()

                    recipeRepository.update(updatedRecipe, imageLocations)
                    withContext(Dispatchers.Main) {
                        navigateUp()
                    }
                }
            }
        }

        fun selectImages() {
            _takeImage.value = true
        }

        fun onImageSelected() {
            _takeImage.value = false
        }

        fun navigateUp() {
            _navigateUp.value = true
        }

        fun onUpNavigated() {
            _navigateUp.value = false
        }
    }
