package com.example.hobbyfi.repositories

import com.example.hobbyfi.persistence.BaseDao
import com.example.hobbyfi.persistence.HobbyfiDatabase

abstract class CacheRepository(protected val database: HobbyfiDatabase) : Repository() {
}