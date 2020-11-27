package com.example.hobbyfi.adapters.tag

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.TagCardBinding
import com.example.hobbyfi.models.Tag

class TagListAdapter(
    private var tags: MutableList<Tag>,
    private var selectedTags: MutableList<Tag>
) : RecyclerView.Adapter<TagListAdapter.TagViewHolder>() {
    

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        return TagViewHolder.getInstance(parent)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = tags[position]
        holder.bind(tag)

        Log.i("SelectedTags", selectedTags.toString())
        Log.i("Tags", tags.toString())

        var color: Int
        try {
            color = Color.parseColor(tag.colour)
        } catch(ex: IllegalArgumentException) {
            Log.w("TagListAdapter" , "Invalid color for tag! Reverting to default colour")
            color = Color.GREEN // default colour, idk
        }

        val isSelected = selectedTags.contains(tag)

        with(holder.binding) {
            tagCard.setCardBackgroundColor(
                if(isSelected) color
                else ContextCompat.getColor(root.context, R.color.colorGrey)
            )

            tagCard.setOnClickListener {
                if(!isSelected) {
                    selectedTags.add(tag)
                    tagCard.setCardBackgroundColor(
                        color
                    )
                } else {
                    selectedTags.remove(tag)
                    tagCard.setCardBackgroundColor(
                        ContextCompat.getColor(root.context, R.color.colorGrey)
                    )
                }
                Log.i("TagListAdapter", "Tags: ${selectedTags}")
            }
        }
    }

    class TagViewHolder(val binding: TagCardBinding, itemView: View) : RecyclerView.ViewHolder(itemView) {
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

    fun setTags(tags: MutableList<Tag>) {
        this.tags = tags
        notifyDataSetChanged()
    }

    fun addTag(tag: Tag) {
        this.tags.add(tag)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = tags.size

    fun getSelectedTags(): MutableList<Tag> = selectedTags
}