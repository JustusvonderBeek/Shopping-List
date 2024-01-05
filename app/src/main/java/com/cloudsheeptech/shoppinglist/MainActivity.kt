package com.cloudsheeptech.shoppinglist

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.cloudsheeptech.shoppinglist.databinding.ActivityMainBinding
import com.cloudsheeptech.shoppinglist.list_overview.ListOverviewViewModel
import com.cloudsheeptech.shoppinglist.list_overview.ListOverviewViewModelFactory
import com.cloudsheeptech.shoppinglist.create.user.StartViewModel
import com.cloudsheeptech.shoppinglist.create.user.StartViewModelFactory
import com.cloudsheeptech.shoppinglist.data.User
import com.cloudsheeptech.shoppinglist.network.Networking
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    // Create the vocabulary here and pass the data around the app so that every fragment share the same data
//    private lateinit var learningViewModel : LearningViewModel
    private lateinit var recapViewModel : StartViewModel
    private lateinit var overviewViewModel: ListOverviewViewModel

    private var notificationId = 0

//    private suspend fun loadUser() {
//        withContext(Dispatchers.IO) {
//            try {
//                val userfile = File(application.filesDir, "user.json")
//                if (!userfile.exists()) {
//                    Log.d("ListOverviewViewModel", "Found no user at ${userfile.absolutePath}")
//                    return@withContext false
//                }
//                val reader = userfile.reader(Charsets.UTF_8)
//                val content = reader.readText()
//                val jsonSerializer = Json {
//                    encodeDefaults = true
//                    ignoreUnknownKeys = false
//                }
//                val user = jsonSerializer.decodeFromString<User>(content)
//                if (user.ID == 0L) {
//                    Log.w("ListOverviewViewModel", "Found user with ID == 0! Incorrect state! Deleting and setting up new user")
//                    userfile.delete()
//                    return@withContext false
//                }
//                reader.close()
//                Log.i("ListOverviewViewModel", "Load user $user from disk")
//                withContext(Dispatchers.Main) {
//                    this@ListOverviewViewModel.user = user
//                }
//            } catch (ex : Exception) {
//                Log.w("ListOverviewViewModel", "Failed to write username to file: $ex")
//            }
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val botNav : BottomNavigationView = binding.bottomNavigation
        val navController = findNavController(R.id.navHostFragment)
        val navIds = navController.graph
        val appBarConfig = AppBarConfiguration.Builder(navIds).build()
        setupActionBarWithNavController(navController, appBarConfig)
//        NavigationUI.setupActionBarWithNavController(this, navController)
        botNav.setupWithNavController(navController)

//        val vocabularyFile = File(applicationContext.filesDir, "vocabulary.json")
//        val vocabulary = Vocabulary.getInstance(vocabularyFile)
        val startViewModel by viewModels<StartViewModel> { StartViewModelFactory(application) }
        val overviewViewModel by viewModels<ListOverviewViewModel> { ListOverviewViewModelFactory(application) }
//        val activityViewModel by viewModels<LearningViewModel> { LearningViewModelFactory(vocabulary) }
//        learningViewModel = activityViewModel
//        val actViewModel by viewModels<RecapViewModel> { RecapViewModelFactory(vocabulary) }
//        recapViewModel = actViewModel

        // Check if we are coming from a notification
        val redirect = intent.extras?.getString("redirect")
        if (redirect != null && redirect == "recapFragment") {
            Log.i("MainActivity", "Coming from a notification!")
            // Navigate to the fragment
            recapViewModel.navigateToApp()
            navController.navigate(R.id.overview)
        }

        // Dirty hack to avoid storing application context in this object class
        Networking.registerApplicationDir(application.filesDir.absolutePath)

        // Create notifications
        createNotificationChannel()
//        RemindersManager.startReminder(applicationContext)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.navHostFragment)
        return navController.navigateUp()
    }

    private fun createNotificationChannel() {
        // Check unnecessary since SDK >= 29 and therefore always in
        val name = getString(R.string.notification_channel_name)
        val descriptionText = getString(R.string.notification_description)
        val importance = NotificationManager.IMPORTANCE_LOW
        val messageChannel = NotificationChannel(getString(R.string.CHANNEL_ID), name, importance)
        messageChannel.description = descriptionText
        // Register channel. Changes are impossible afterwards
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(messageChannel)
    }

}