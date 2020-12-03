package com.example.hobbyfi.ui.chatroom

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.NavDestination
import androidx.navigation.Navigation.findNavController
import androidx.navigation.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.ActivityChatroomBinding
import com.example.hobbyfi.ui.base.BaseActivity
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.factories.ChatroomActivityViewModelFactory
import kotlinx.android.synthetic.main.activity_chatroom.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class ChatroomActivity : BaseActivity() {
    // TODO: Handle navdrawer, rendering of fragments, activity intent calls, button listeners, etc. . .
    // TODO: Have user & chatroom info passed in (or fetched from cache) & send request for messages, users, & event
    // TODO: Disable event create button if event is already created

    // TODO: research into integrating nav drawer different icons with navcomponent
    // TODO: need to integrate action bar menu and have drawable from there trigger right navigationview

    // TODO: Have this be the deeplink activity. Register it and handle intent extras and facebook/default user

    // TODO: If user leaves chatroom (not exits), delete messages saved in local db + users

    private val viewModel: ChatroomActivityViewModel by viewModels(factoryProducer = {
        ChatroomActivityViewModelFactory(application, args.user)
    })
    private val args: ChatroomActivityArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityChatroomBinding.inflate(layoutInflater)
        with(binding) {
            setContentView(root)
            val appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)

            // FIXME: Bad Kotlin synthetics
            nav_view_admin.setupWithNavController(navController)
            // TODO: if(user_is_admin) - viewmodel & databinding
            nav_view_chatroom.setupWithNavController(navController)
        }


        // if not deeplink: fire off 3 requests/load from db here -> event, messages, user
        // if deeplink: also fire off request/load from db here -> chatroom info

        // TODO: Ask for permission upon event card press for access to location
    }

    override fun onResume() {
        super.onResume()
        // TODO: Google Services availability https://firebase.google.com/docs/cloud-messaging/android/client#sample-play
    }
}