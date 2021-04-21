package com.example.hobbyfi.ui.base

import com.example.hobbyfi.utils.WorkerUtils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

interface ImageUploadContinuation {
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onImageUploadEvent(event: WorkerUtils.ImageUploadEvent)
}