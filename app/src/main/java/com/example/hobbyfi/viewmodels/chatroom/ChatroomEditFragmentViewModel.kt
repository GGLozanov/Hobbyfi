package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.models.Base64Image
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.models.TagBundle
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.NameDescriptionBindable
import com.example.hobbyfi.viewmodels.base.NameDescriptionBindableViewModel

class ChatroomEditFragmentViewModel(application: Application, initialTags: List<Tag>) : BaseViewModel(application),
    NameDescriptionBindable by NameDescriptionBindableViewModel() {
    var base64Image: Base64Image = Base64Image()

    var tagBundle = TagBundle(initialTags)
}