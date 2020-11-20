package com.example.hobbyfi.utils

import com.example.hobbyfi.persistence.UserDao
import com.example.hobbyfi.shared.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


object CacheUtils {
    fun updateCacheUserFromRequestMap(dao: UserDao, authId: Int, userMap: Map<String?, String?>) {
        /* dao.getUser(authId).observeForever { userEntity ->
            if (userMap.containsKey(Constants.USERNAME)) {
                userEntity.setUsername(userMap[Constants.USERNAME])
            }
            if (userMap.containsKey(Constants.DESCRIPTION)) {
                userEntity.setDescription(userMap[Constants.DESCRIPTION])
            }
            if (userMap.containsKey(Constants.EMAIL)) {
                userEntity.setEmail(userMap[Constants.EMAIL])
            }

            // we don't care about the password for the cache (because there is no such field)
            CoroutineScope(Dispatchers.IO).launch {
                dao.updateFields(
                    userEntity.getEmail(),
                    userEntity.getUsername(),
                    userEntity.getDescription(), authId
                )
            } // TODO: Fix small overhead with resetting already set information
        } // get user to receive already cached information and use it if necessary */
    }
}