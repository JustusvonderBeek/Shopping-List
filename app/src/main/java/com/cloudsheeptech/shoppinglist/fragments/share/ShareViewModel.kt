package com.cloudsheeptech.shoppinglist.fragments.share

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.cloudsheeptech.shoppinglist.data.sharing.ListShareDatabase
import com.cloudsheeptech.shoppinglist.data.sharing.ShareUserPreview
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.onlineUser.OnlineUserRepository
import com.cloudsheeptech.shoppinglist.data.sharing.ListShare
import com.cloudsheeptech.shoppinglist.data.sharing.ListShareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val database : ShoppingListDatabase,
    private val onlineUserRepo: OnlineUserRepository,
    private val sharingRepository: ListShareRepository,
    private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {

    private val job = Job()
    private val localCoroutine = CoroutineScope(Dispatchers.Main + job)
    private val sharedDao = database.sharedDao()
    private val onlineUserDao = database.onlineUserDao()

    private val listId : Long = savedStateHandle["listId"]!!

//    private val listHandler = ShoppingListRepository(database)

    val searchName = MutableLiveData<String>("")
    private val _searchedUsers = MutableLiveData<List<ShareUserPreview>>()
    val searchedUsers : LiveData<List<ShareUserPreview>> get() = _searchedUsers

    private var _offlineShared = sharedDao.getListSharedWithLive(listId)

    private val _shared = MediatorLiveData<List<ShareUserPreview>>()
    val sharedPreview : LiveData<List<ShareUserPreview>> get() = _shared

    // --- Navigation / UI States ---

    private val _navigateUp = MutableLiveData<Boolean>(false)
    val navigateUp : LiveData<Boolean> get() = _navigateUp

    init {
        _searchedUsers.value = emptyList()
        initPreview()
    }

    private fun initPreview() {
        _shared.addSource(_searchedUsers) { onlineUsers ->
            Log.d("ShareViewModel", "Online changed...")
            localCoroutine.launch {
                combineUserLists(onlineUsers, _offlineShared.value)
            }
        }
        _shared.addSource(_offlineShared) { offlineUsers ->
            Log.d("ShareViewModel", "Offline changed")
            localCoroutine.launch {
                combineUserLists(_searchedUsers.value, offlineUsers)
            }
        }
    }

    private suspend fun convertListShareToPreviewUser(share : List<ListShareDatabase>) : List<ShareUserPreview> {
        val previewUsers = mutableListOf<ShareUserPreview>()
        withContext(Dispatchers.IO) {
            share.forEach { s ->
                val user = onlineUserDao.getUser(s.SharedWith)
                if (user != null) {
                    previewUsers.add(ShareUserPreview(user.onlineId, user.username, true))
                }
            }
        }
        return previewUsers
    }

    private suspend fun combineUserLists(onlinePreview : List<ShareUserPreview>?, offlinePreview : List<ListShareDatabase>?) {
        Log.d("ShareViewModel", "Combine called")
        withContext(Dispatchers.IO) {
            val combinedUsers = mutableListOf<ShareUserPreview>()
            Log.d("ShareViewModel", "Combine: Step before - length C:${combinedUsers.size}; On:${onlinePreview?.size}; Off:${offlinePreview?.size}")
            offlinePreview?.let { combinedUsers.addAll(convertListShareToPreviewUser(it)) }
            // If the offline user is already in the list, we know he was already shared
            // Therefore don't add the online user anymore. Differentiate on UserID
            Log.d("ShareViewModel", "Combine: Step offline - length C:${combinedUsers.size}; On:${onlinePreview?.size}; Off:${offlinePreview?.size}")
            onlinePreview?.let {
                it.forEach {
                    // Compare on the UserID (overwritten in the class equals operator itself)
                    if (!combinedUsers.contains(it)) {
                        combinedUsers.add(it)
                    }
                }
            }
            Log.d("ShareViewModel", "Combine: Step online - length C:${combinedUsers.size}; On:${onlinePreview?.size}; Off:${offlinePreview?.size}")
            withContext(Dispatchers.Main) {
                _shared.value = combinedUsers
            }
        }
    }

    private suspend fun searchUsersFromOnlineAndDatabase(name : String) : List<ShareUserPreview> {
        var users = emptyList<ShareUserPreview>()
        withContext(Dispatchers.IO) {
//            val onlineUsers = listHandler.SearchUsersOnline(name)
//            val onlinePreview = onlineUsers.map { x -> ShareUserPreview(x.ID, x.Name, false) }
//            users = onlinePreview
            val onlineUser = onlineUserRepo.readOnline(name)
            val onlinePreview = onlineUser.map { x -> ShareUserPreview(x.onlineId, x.username, false) }
            users = onlinePreview
        }
        return users
    }

    private suspend fun updateListCreators(list : List<ShareUserPreview>) {
        withContext(Dispatchers.Main) {
            _searchedUsers.value = list
        }
    }

    fun searchUser() {
        if (searchName.value!!.isEmpty()) {
            Log.d("ShareViewModel", "Empty string, clear preview")
            _searchedUsers.value = emptyList()
            return
        }
        localCoroutine.launch {
            val creators = searchUsersFromOnlineAndDatabase(searchName.value!!)
            updateListCreators(creators)
        }
    }

    fun shareList(sharedWithId : Long) {
//        listHandler.ShareShoppingListOnline(listId, sharedWithId)
        localCoroutine.launch {
            sharingRepository.create(listId, sharedWithId)
        }
//        navigateUp()
    }

    fun unshareList() {
//        listHandler.UnshareShoppingListOnline(listId)
        localCoroutine.launch {
            sharingRepository.delete(listId)
        }
    }

    fun unshareListForUser(userId : Long) {
//        listHandler.UnshareShoppingListForUserOnline(userId, listId)
        localCoroutine.launch {
            val sharings = sharingRepository.read(listId)
            val filteredSharing = sharings.filter { share -> share != userId }
            sharingRepository.update(listId, filteredSharing)
        }
    }

    fun navigateUp() {
        _navigateUp.value = true
    }

    fun onUpNavigated() {
        _navigateUp.value = false
    }
}