package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.models.data.TagBundle
import com.example.hobbyfi.viewmodels.base.*

class ChatroomEditFragmentViewModel(application: Application, initialTags: List<Tag>) : BaseViewModel(application),
        NameDescriptionBindable by NameDescriptionBindableViewModel(),
        Base64ImageHolder by Base64ImageHolderViewModel(), TagBundleHolder by TagBundleHolderViewModel(initialTags) {
}