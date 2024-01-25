package com.cloudsheeptech.shoppinglist.fragments.edit

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditViewModel() : ViewModel() {

    private val job = Job()
    private val editVmScope = CoroutineScope(Dispatchers.IO + job)

    val word = MutableLiveData<String>()
    val translation = MutableLiveData<String>()
    private var wordId = -1

    private val _navigateUp = MutableLiveData<Boolean>(false)
    val navigateUp : LiveData<Boolean> get() = _navigateUp

    fun loadWord(selectedId : Int) {
//        if (selectedId > -1) {
//            val selected = vocabulary.wordList[selectedId]
//            word.value = selected.Vocabulary
//            translation.value = selected.Translation
//            wordId = selected.ID
//        }
    }

    fun editItem() {
        if (word.value != null && translation.value != null) {
//            val updatedWord = Word(wordId, word.value!!, translation.value!!)
            editVmScope.launch {
//                vocabulary.modifyVocabularyItem(updatedWord)
                withContext(Dispatchers.Main) {
                    _navigateUp.value = true
                }
            }
        }
    }

    fun wordEdited() {
        _navigateUp.value = false
    }

}