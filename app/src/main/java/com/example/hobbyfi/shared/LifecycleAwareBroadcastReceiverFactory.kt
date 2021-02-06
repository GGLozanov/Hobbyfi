package com.example.hobbyfi.shared

import androidx.lifecycle.LifecycleOwner

abstract class LifecycleAwareBroadcastReceiverFactory(
    protected val lifecycleOwner: LifecycleOwner
) : BroadcastReceiverFactory()