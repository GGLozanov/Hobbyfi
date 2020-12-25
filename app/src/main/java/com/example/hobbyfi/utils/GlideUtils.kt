package com.example.hobbyfi.utils

import com.bumptech.glide.signature.ObjectKey
import com.example.hobbyfi.R
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig

object GlideUtils {
    fun getPagingObjectKey(prefConfig: PrefConfig, position: Int, prefId: Int, pageSize: Int) = ObjectKey(
        prefConfig.readLastPrefFetchTime(prefId) +
                if(position % pageSize == 0)
                    position / pageSize else (position / pageSize) + 1)
}