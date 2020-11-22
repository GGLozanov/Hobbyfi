package com.example.hobbyfi.repositories

import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.persistence.BaseDao
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.shared.PrefConfig

abstract class CacheRepository(prefConfig: PrefConfig, hobbyfiAPI: HobbyfiAPI, protected val database: HobbyfiDatabase)
    : Repository(prefConfig, hobbyfiAPI) {
}