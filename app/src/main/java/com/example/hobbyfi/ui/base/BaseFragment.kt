package com.example.hobbyfi.ui.base

import androidx.fragment.app.Fragment
import com.example.hobbyfi.shared.PrefConfig

abstract class BaseFragment : Fragment() {
    private val prefConfig: PrefConfig // no need for weakreference this time because PrefConfig will use appContext!
        get() {
            TODO()
        }
}