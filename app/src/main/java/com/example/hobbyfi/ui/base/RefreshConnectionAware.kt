package com.example.hobbyfi.ui.base

import android.os.Bundle
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.RefreshConnectivityMonitor

interface RefreshConnectionAware {
    fun observeConnectionRefresh(savedState: Bundle?, refreshConnectivityMonitor: RefreshConnectivityMonitor) {
        refreshConnectivityMonitor.postLastConnection(savedState?.getBoolean(Constants.LAST_CONNECTIVITY) == true)
    }

    fun refreshDataOnConnectionRefresh()
}