package com.cloudsheeptech.shoppinglist

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
import com.cloudsheeptech.shoppinglist.fragments.create.list.CreateShoppinglistViewModel
import com.cloudsheeptech.shoppinglist.fragments.create.list.CreateShoppinglistViewModelFactory
import com.cloudsheeptech.shoppinglist.databinding.ActivityMainBinding
import com.cloudsheeptech.shoppinglist.fragments.list_overview.ListOverviewViewModel
import com.cloudsheeptech.shoppinglist.fragments.list_overview.ListOverviewViewModelFactory
import com.cloudsheeptech.shoppinglist.fragments.create.user.StartViewModel
import com.cloudsheeptech.shoppinglist.data.database.ShoppingListDatabase
import com.cloudsheeptech.shoppinglist.data.list.ShoppingListRepository
import com.cloudsheeptech.shoppinglist.data.user.AppUserLocalDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRemoteDataSource
import com.cloudsheeptech.shoppinglist.data.user.AppUserRepository
import com.cloudsheeptech.shoppinglist.network.Networking
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    private var notificationId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val botNav : BottomNavigationView = binding.bottomNavigation
        val navController = findNavController(R.id.navHostFragment)
        val navIds = navController.graph
        val appBarConfig = AppBarConfiguration.Builder(navIds).build()
        setupActionBarWithNavController(navController, appBarConfig)
        botNav.setupWithNavController(navController)

        // TODO: Check if we are coming from a notification; NOT yet necessary
        val redirect = intent.extras?.getString("redirect")
        if (redirect != null && redirect == "recapFragment") {
            Log.i("MainActivity", "Coming from a notification!")
            // Navigate to the fragment
//            recapViewModel.navigateToApp()
            navController.navigate(R.id.overview)
        }

        // Dirty hack to avoid storing application context in this object class
//        Networking.registerApplicationDir(application.filesDir.absolutePath, database)

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