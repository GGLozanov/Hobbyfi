package com.example.hobbyfi.adapters.base

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.example.hobbyfi.models.ExpandedModel
import com.example.hobbyfi.shared.PrefConfig

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
                .placeholder(defaultPicDrawable)
                .signature(
                    signatureGenerator(position)
                ) // calculate current page based on item position
                .into(mainImageView)
        } else {
            Glide.with(itemView.context)
                .load(defaultPicDrawable)
                .into(mainImageView)
        }
    }

    abstract val mainImageView: ImageView
    abstract val signatureGenerator: (position: Int) -> ObjectKey
    abstract val defaultPicDrawable: Drawable
}