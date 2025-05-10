package com.cloudsheeptech.shoppinglist.fragments.share

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.cloudsheeptech.shoppinglist.data.onlineUser.OnlineUserRepository
import com.cloudsheeptech.shoppinglist.data.sharing.ListShareRepository
import com.cloudsheeptech.shoppinglist.data.sharing.ShareUserPreview
import com.cloudsheeptech.shoppinglist.data.sharing.recipe.RecipeShareRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val job = Job()
        private val localCoroutine = CoroutineScope(Dispatchers.Main + job)

        // Both of these are -1L by default and this fragment can handle both
        // Using liveData so that the ids can change after the fragment is created
        private val listId = savedStateHandle.getLiveData("listId", -1L)
        private val recipeId = savedStateHandle.getLiveData("recipeId", -1L)

        // Not yet used, since only the owner can share for now
        private val createdBy = savedStateHandle.getLiveData("createdBy", -1L)

        // Combine the latest listId,createdBy into a LiveData pair
        private val listIdentifier =
            MediatorLiveData<Pair<Long, Long>>().apply {
                var currentListId: Long = -1L
                var currentCreatedBy: Long = -1L

                addSource(listId) { newId ->
                    if (newId > 0L) {
                        currentListId = newId
                        if (currentCreatedBy > 0L) {
                            value = currentListId to currentCreatedBy
                        }
                    }
                }

                addSource(createdBy) { newId ->
                    if (newId > 0L) {
                        currentCreatedBy = newId
                        if (currentListId > 0L) {
                            value = currentListId to currentCreatedBy
                        }
                    }
                }
            }

        private val recipeIdentifier =
            MediatorLiveData<Pair<Long, Long>>().apply {
                var currentRecipeId: Long = -1L
                var currentCreatedBy: Long = -1L

                addSource(recipeId) { newId ->
                    if (newId > 0L) {
                        currentRecipeId = newId
                        if (currentCreatedBy > 0L) {
                            value = currentRecipeId to currentCreatedBy
                        }
                    }
                }

                addSource(createdBy) { newId ->
                    if (newId > 0L) {
                        currentCreatedBy = newId
                        if (currentRecipeId > 0L) {
                            value = currentRecipeId to currentCreatedBy
                        }
                    }
                }
            }

        // The full list of all users which this list is currently shared with
        private val sharedWithUsers: LiveData<List<ShareUserPreview>> =
            listIdentifier.switchMap { (listId, createdBy) ->
                sharingRepository.readLive(listId, createdBy)
            }

        private val recipeSharedWithUsers: LiveData<List<ShareUserPreview>> =
            recipeIdentifier.switchMap { (recipeId, createdBy) ->
                recipeShareRepository.readLive(recipeId, createdBy)
            }

        // The list of users which is returned from the online search
        private val _searchedUsers = MutableLiveData<List<ShareUserPreview>>(emptyList<ShareUserPreview>())
        val searchedUsers: LiveData<List<ShareUserPreview>> get() = _searchedUsers

        val searchString = MutableLiveData<String>("")

        private val _combinedUsers = MediatorLiveData<List<ShareUserPreview>>()
        val combinedUsers: LiveData<List<ShareUserPreview>> get() = _combinedUsers

        // --- Navigation / UI States ---

        private val _navigateUp = MutableLiveData<Boolean>(false)
        val navigateUp: LiveData<Boolean> get() = _navigateUp

        init {
            setupCombinedUserList()
        }

        private fun setupCombinedUserList() {
            _combinedUsers.addSource(sharedWithUsers) { sharedUsers ->
                Log.d("ShareViewModel", "Shared changed...")
                _combinedUsers.value = combineUserLists(_searchedUsers.value, sharedUsers)
            }

            _combinedUsers.addSource(_searchedUsers) { onlineUsers ->
                Log.d("ShareViewModel", "Online changed...")
                _combinedUsers.value = combineUserLists(onlineUsers, sharedWithUsers.value)
            }

            _combinedUsers.addSource(recipeSharedWithUsers) { sharedUsers ->
                Log.d("ShareViewModel", "Shared changed...")
                _combinedUsers.value = combineUserLists(_searchedUsers.value, sharedUsers)
            }
        }

        private fun combineUserLists(
            onlinePreview: List<ShareUserPreview>?,
            offlinePreview: List<ShareUserPreview>?,
        ): List<ShareUserPreview> {
            Log.d("ShareViewModel", "Combine called")
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
            return combinedUsers
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
                    _combinedUsers.value?.map { user ->
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
                val success = sharingRepository.delete(listId.value!!, user.OnlineID, sharedWithId)
                if (!success) {
                    // TODO: Show toast
                }
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
