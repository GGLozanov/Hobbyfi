package com.example.hobbyfi.adapters.tag

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.models.Tag

class TagListAdapter(
    private val tags: List<Tag>
) : RecyclerView.Adapter<TagListAdapter.TagViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    inner class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }

    override fun getItemCount(): Int {
        return tags.size
    }
}