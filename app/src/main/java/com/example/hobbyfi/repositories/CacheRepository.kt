package com.example.hobbyfi.repositories

import com.example.hobbyfi.persistence.BaseDao

abstract class CacheRepository<T: BaseDao<*>> : Repository() {
}