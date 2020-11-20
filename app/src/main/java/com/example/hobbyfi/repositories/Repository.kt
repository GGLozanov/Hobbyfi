package com.example.hobbyfi.repositories

import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.shared.PrefConfig

abstract class Repository(protected val prefConfig: PrefConfig, protected val hobbyfiAPI: HobbyfiAPI) {
}