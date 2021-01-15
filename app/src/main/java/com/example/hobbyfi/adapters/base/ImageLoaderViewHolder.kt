package com.example.hobbyfi.adapters.base

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.example.hobbyfi.R
import com.example.hobbyfi.models.ExpandedModel
import com.example.hobbyfi.models.Model
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.utils.GlideUtils

abstract class ImageLoaderViewHolder<in T: ExpandedModel>(
    itemView: View,
    protected val prefConfig: PrefConfig
) : BaseViewHolder<T>(itemView) {
    override fun bind(model: T?, position: Int) {
        bindImage(model, position)
    }

    protected fun bindImage(model: ExpandedModel?, position: Int) {
        if(model?.photoUrl != null) {
            Glide.with(itemView.context)
                .load(model.photoUrl)
                .placeholder(defaultPicResId)
                .signature(
                    signatureGenerator(position)
                ) // calculate current page based on item position
                .into(mainImageView)
        } else {
            Glide.with(itemView.context)
                .load(
                    defaultPicResId
                )
                .into(mainImageView)
        }
    }

    abstract val mainImageView: ImageView
    abstract val signatureGenerator: (position: Int) -> ObjectKey
    abstract val defaultPicResId: Int
}