package com.cloudsheeptech.shoppinglist.datastructures

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cloudsheeptech.shoppinglist.data.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ItemListWithName<T : Comparable<T>> {

    @Serializable
    private data class ItemList<T> (
        var title : String,
        var list : List<T>,
    )

    companion object {
        fun <T : Comparable<T>> createFromFile(file : File) : ItemListWithName<T>? {
            val reader = file.reader(Charsets.UTF_8)
            val content = reader.readText()
            if (content == "")
                return null
            val itemList = ItemListWithName<T>()
            val jsonHandler = Json {
                ignoreUnknownKeys = false
                encodeDefaults = true
            }
            val t = jsonHandler.decodeFromString<ItemList<T>>(content)
            itemList._title.value = t.title
            itemList._list.value = t.list.toMutableList()
            return itemList
        }
    }

    private var jsonHandler = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    private var _title = MutableLiveData<String>("Title")
    val title : LiveData<String>
        get() = _title

    private var _list = MutableLiveData<MutableList<T>>()
    val list : LiveData<MutableList<T>> get() = _list

    fun addItem(item : T) {
        Log.d("Shoppinglist", "Adding new item")
        if (_list.value == null) {
            Log.d("Shoppinglist", "List initialized")
            _list.value = mutableListOf()
        }
        _list.value!!.add(item)
        _list.value = _list.value!!
        Log.d("ItemListWithName", "Added element. Length now: ${size()}")
    }

    fun removeItem(item : T) {
        if (_list.value == null) {
            return
        }
        _list.value!!.remove(item)
    }

    fun remoteAt(index : Int) {
        if (_list.value == null)
            return
        _list.value!!.removeAt(index)
        _list.value = _list.value
    }

    suspend fun storeToDisk(file : String) {
        if (_list.value == null && (_title.value == null || _title.value!! == "")) {
            Log.w("ItemListWithName", "Does not store empty list")
            return
        }
        withContext(Dispatchers.IO) {
            try {
                val listFile = File(file)
                val writer = listFile.writer(Charsets.UTF_8)
                val info = ItemList<T>(_title.value!!, _list.value!!)
                val encoded = jsonHandler.encodeToString(info)
                writer.write(encoded)
            } catch (ex : Exception) {
                Log.w("ItemListWithName", "Cannot store list to disk: $ex")
            }
        }
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