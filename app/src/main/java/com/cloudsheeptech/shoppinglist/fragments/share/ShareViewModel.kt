package com.cloudsheeptech.shoppinglist.fragments.share

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import com.cloudsheeptech.shoppinglist.data.onlineUser.OnlineUserRepository
import com.cloudsheeptech.shoppinglist.data.sharing.ListShareRepository
import com.cloudsheeptech.shoppinglist.data.sharing.ShareUserPreview
import com.cloudsheeptech.shoppinglist.data.sharing.recipe.RecipeShareRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ShareViewModel
    @Inject
    constructor(
        private val onlineUserRepo: OnlineUserRepository,
        private val sharingRepository: ListShareRepository,
        private val recipeShareRepository: RecipeShareRepository,
        private val appUserRepository: AppUserRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val job = Job()
        private val localCoroutine = CoroutineScope(Dispatchers.Main + job)

        // Both of these are -1L by default and this fragment can handle both
        // Using liveData so that the ids can change after the fragment is created
        private val listId = savedStateHandle.getLiveData("listId", -1L)
        private val recipeId = savedStateHandle.getLiveData("recipeId", -1L)

        // Not yet used, since only the owner can share for now
        private val createdBy = savedStateHandle.getLiveData("createdBy", -1L)

        // Load list or shared recipe data based on live data from store
        private val offlineUsers: LiveData<List<ShareUserPreview>> =
            listId
                .asFlow()
                .flatMapLatest { id ->
                    if (id > 0L) {
                        val currentUser = appUserRepository.read()
                        if (currentUser == null) {
                            return@flatMapLatest flow { emit(emptyList<ShareUserPreview>()) }
                        }
                        val listOfSharedIds = sharingRepository.readLive(id, currentUser.OnlineID)
                        return@flatMapLatest listOfSharedIds.asFlow()
                    }
                    flow {
                        emit(emptyList<ShareUserPreview>())
                    }
                }.asLiveData()

        private val _searchedUsers = MutableLiveData<List<ShareUserPreview>>()
        val searchedUsers: LiveData<List<ShareUserPreview>> get() = _searchedUsers

        val searchString = MutableLiveData<String>("")

        private val _sharedUsers = MediatorLiveData<List<ShareUserPreview>>()
        val sharedUsers: LiveData<List<ShareUserPreview>> get() = _sharedUsers

        // --- Navigation / UI States ---

        private val _navigateUp = MutableLiveData<Boolean>(false)
        val navigateUp: LiveData<Boolean> get() = _navigateUp

        init {
            initPreview()
        }

        private fun initPreview() {
            _sharedUsers.addSource(_searchedUsers) { onlineUsers ->
                Log.d("ShareViewModel", "Online changed...")
                localCoroutine.launch {
                    combineUserLists(onlineUsers, offlineUsers.value)
                }
            }
            _sharedUsers.addSource(offlineUsers) { offlineUsers ->
                Log.d("ShareViewModel", "Offline changed")
                localCoroutine.launch {
                    combineUserLists(_searchedUsers.value, offlineUsers)
                }
            }
        }

        private suspend fun combineUserLists(
            onlinePreview: List<ShareUserPreview>?,
            offlinePreview: List<ShareUserPreview>?,
        ) {
            Log.d("ShareViewModel", "Combine called")
            withContext(Dispatchers.IO) {
                val combinedUsers = mutableListOf<ShareUserPreview>()
                Log.d(
                    "ShareViewModel",
                    "Combine: Step before - length C:${combinedUsers.size}; On:${onlinePreview?.size}; Off:${offlinePreview?.size}",
                )
                offlinePreview?.let { combinedUsers.addAll(it) }
                // If the offline user is already in the list, we know he was already shared
                // Therefore don't add the online user anymore. Differentiate on UserID
                Log.d(
                    "ShareViewModel",
                    "Combine: Step offline - length C:${combinedUsers.size}; On:${onlinePreview?.size}; Off:${offlinePreview?.size}",
                )
                onlinePreview?.let {
                    it.forEach {
                        // Compare on the UserID (overwritten in the class equals operator itself)
                        if (!combinedUsers.contains(it)) {
                            combinedUsers.add(it)
                        }
                    }
                }
                Log.d(
                    "ShareViewModel",
                    "Combine: Step online - length C:${combinedUsers.size}; On:${onlinePreview?.size}; Off:${offlinePreview?.size}",
                )
                withContext(Dispatchers.Main) {
                    _sharedUsers.value = combinedUsers
                }
            }
        }

        private suspend fun searchUsersFromOnlineAndDatabase(name: String): List<ShareUserPreview> {
            var users = emptyList<ShareUserPreview>()
            withContext(Dispatchers.IO) {
//            val onlineUsers = listHandler.SearchUsersOnline(name)
//            val onlinePreview = onlineUsers.map { x -> ShareUserPreview(x.ID, x.Name, false) }
//            users = onlinePreview
                val onlineUser = onlineUserRepo.readOnline(name)
                val onlinePreview =
                    onlineUser.map { x -> ShareUserPreview(x.onlineId, x.username, false) }
                users = onlinePreview
            }
            return users
        }

        private suspend fun updateListCreators(list: List<ShareUserPreview>) {
            withContext(Dispatchers.Main) {
                _searchedUsers.value = list
            }
        }

        fun searchUser() {
            if (searchString.value!!.isEmpty()) {
                Log.d("ShareViewModel", "Empty string, clear preview")
                _searchedUsers.value = emptyList()
                return
            }
            localCoroutine.launch {
                val creators = searchUsersFromOnlineAndDatabase(searchString.value!!)
                updateListCreators(creators)
            }
        }

        fun share(sharedWithId: Long) {
            if (this.listId.value!! > 0L) {
                shareList(sharedWithId)
            } else {
                shareRecipe(sharedWithId)
            }
        }

        private fun shareList(sharedWithId: Long) {
            localCoroutine.launch {
                val user = appUserRepository.read() ?: return@launch
                val success = sharingRepository.create(listId.value!!, user.OnlineID, sharedWithId)
                if (success) {
                    _sharedUsers.value?.map { user ->
                        if (user.UserId == sharedWithId) {
                            user.Shared = true
                        }
                    }
                }
            }
        }

        private fun shareRecipe(sharedWithId: Long) {
            localCoroutine.launch {
                val user = appUserRepository.read() ?: return@launch
                recipeShareRepository.create(recipeId.value!!, user.OnlineID, sharedWithId)
            }
        }

        fun unshare(sharedWithId: Long) {
            if (this.listId.value!! > 0L) {
                unshareList(sharedWithId)
            } else {
                unshareRecipe(sharedWithId)
            }
        }

        private fun unshareList(sharedWithId: Long) {
            if (sharedWithId > 0L) {
                unshareListForUser(sharedWithId)
            } else {
                localCoroutine.launch {
                    val user = appUserRepository.read() ?: return@launch
                    sharingRepository.deleteAll(listId.value!!, user.OnlineID)
                }
            }
        }

        private fun unshareRecipe(sharedWithId: Long) {
            if (sharedWithId > 0L) {
                unshareRecipeForUser(sharedWithId)
            } else {
                localCoroutine.launch {
                    val user = appUserRepository.read() ?: return@launch
                    recipeShareRepository.deleteAll(recipeId.value!!, user.OnlineID)
                }
            }
        }

        private fun unshareListForUser(sharedWithId: Long) {
            localCoroutine.launch {
                val user = appUserRepository.read() ?: return@launch
                sharingRepository.delete(listId.value!!, user.OnlineID, sharedWithId)
            }
        }

        private fun unshareRecipeForUser(sharedWithId: Long) {
            localCoroutine.launch {
                val user = appUserRepository.read() ?: return@launch
                recipeShareRepository.delete(recipeId.value!!, user.OnlineID, sharedWithId)
            }
        }

        fun navigateUp() {
            _navigateUp.value = true
        }

        fun onUpNavigated() {
            _navigateUp.value = false
        }
    }
