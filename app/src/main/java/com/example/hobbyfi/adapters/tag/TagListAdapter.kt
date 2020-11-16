package com.example.hobbyfi.adapters.tag

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.TagCardBinding
import com.example.hobbyfi.models.Tag

class TagListAdapter(
    private var tags: List<Tag>,
) : RecyclerView.Adapter<TagListAdapter.TagViewHolder>() {
    
    interface OnItemPressed {
        fun onItemPress(tag: Tag, wasSelected: Boolean)
    }

    private lateinit var onItemPressed: OnItemPressed
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        return TagViewHolder.getInstance(parent)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(tags[position])
        holder.binding.tagCard.setOnClickListener {
            holder.isSelected = !holder.isSelected
            if(holder.isSelected) {
                holder.binding.tagCard.setCardBackgroundColor(
                    Color.parseColor(tags[position].colour)
                )
            } else {
                holder.binding.tagCard.setCardBackgroundColor(
                   0x9a9a9a
                )
            }

            onItemPressed.onItemPress(tags[position], holder.isSelected)
        }
    }

    class TagViewHolder(val binding: TagCardBinding, itemView: View) : RecyclerView.ViewHolder(itemView) {
        var isSelected: Boolean = false

        companion object {
            //get instance of the ViewHolder
            fun getInstance(parent: ViewGroup): TagViewHolder {
                val tagCardBinding: TagCardBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.tag_card,
                    parent, false)
                return TagViewHolder(tagCardBinding, tagCardBinding.root)
            }
        }


        fun bind(tag: Tag?) {
            binding.tag = tag
        }
    }

    fun setItems(tags: List<Tag>) {
        this.tags = tags
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return tags.size
    }

    fun setOnItemPressed(onItemPressed: OnItemPressed) {
        this.onItemPressed = onItemPressed
    }
}