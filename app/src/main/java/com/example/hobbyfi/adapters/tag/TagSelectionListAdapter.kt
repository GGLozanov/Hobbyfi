package com.example.hobbyfi.adapters.tag

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.base.BaseViewHolder
import com.example.hobbyfi.databinding.TagCardBinding
import com.example.hobbyfi.models.data.Tag

class TagSelectionListAdapter(
    private var tags: MutableList<Tag>,
    private var selectedTags: MutableList<Tag>
) : RecyclerView.Adapter<TagSelectionListAdapter.TagViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        return TagViewHolder.getInstance(parent)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = tags[position]
        holder.bind(tag, position)

        Log.i("SelectedTags", selectedTags.toString())
        Log.i("Tags", tags.toString())

        val color: Int = try {
            Color.parseColor(tag.colour)
        } catch(ex: IllegalArgumentException) {
            Log.w("TagListAdapter" , "Invalid color for tag! Reverting to default colour")
            Color.GREEN // default colour, idk
        }

        val wasSelected = selectedTags.contains(tag)
        with(holder.binding) {
            tagCard.setCardBackgroundColor(
                if(wasSelected) color
                else ContextCompat.getColor(root.context, R.color.colorGrey)
            )

            tagCard.setOnClickListener {
                val isSelected = !selectedTags.contains(tag)

                if(isSelected) {
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

    class TagViewHolder(val binding: TagCardBinding) : BaseViewHolder<Tag>(binding.root) {
        companion object {
            //get instance of the ViewHolder
            fun getInstance(parent: ViewGroup): TagViewHolder {
                val tagCardBinding: TagCardBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.tag_card,
                    parent, false)
                return TagViewHolder(tagCardBinding)
            }
        }


        override fun bind(tag: Tag?, position: Int) {
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