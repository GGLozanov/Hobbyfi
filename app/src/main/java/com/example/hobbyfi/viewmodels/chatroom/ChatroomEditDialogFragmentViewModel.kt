package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.models.TagBundle
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.NameDescriptionBindable
import com.example.hobbyfi.viewmodels.base.NameDescriptionBindableViewModel

class ChatroomEditDialogFragmentViewModel(application: Application) : BaseViewModel(application),
    NameDescriptionBindable by NameDescriptionBindableViewModel() {
        var tagBundle = TagBundle()
}