package com.example.hobbyfi.adapters.base

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.example.hobbyfi.models.data.ExpandedModel
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.asFirebaseStorageReference
import com.example.hobbyfi.shared.asFirebaseStorageReferenceEx
import com.example.hobbyfi.shared.loadReferenceWithMetadataSignature
import com.google.firebase.storage.FirebaseStorage

abstract class ImageLoaderViewHolder<in T: ExpandedModel>(
    itemView: View,
    protected val prefConfig: PrefConfig
) : BaseViewHolder<T>(itemView) {
    override fun bind(model: T?, position: Int) {
        bindImage(model, position)
    }

    protected fun bindImage(model: ExpandedModel?, position: Int) {
        try {
            if(model?.photoUrl == null) {
                throw IllegalArgumentException()
            }

            model.photoUrl!!.asFirebaseStorageReferenceEx().apply {
                metadata.addOnSuccessListener { metadata ->
                    Glide.with(itemView.context)
                        .loadReferenceWithMetadataSignature(this, metadata)
                        .placeholder(defaultPicDrawable)
                        .into(mainImageView)
                }
            }
        } catch(ex: Exception) {
            ex.printStackTrace()
            Glide.with(itemView.context)
                .load(defaultPicDrawable)
                .into(mainImageView)
        }
    }

    abstract val mainImageView: ImageView
    abstract val signatureGenerator: (position: Int) -> ObjectKey
    abstract val defaultPicDrawable: Drawable
}