package com.example.hobbyfi.adapters.base

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.models.data.Model

abstract class BaseViewHolder<in T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(model: T?, position: Int)
}