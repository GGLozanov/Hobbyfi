package com.example.hobbyfi.adapters.tag

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.ChatroomTagCardBinding
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.utils.ColourUtils


class ChatroomTagListAdapter(private var chatroomTags: List<Tag>, context: Context, resource: Int) :
        ArrayAdapter<Tag>(context, resource) {
    override fun getCount(): Int {
        return chatroomTags.size
    }

    override fun getItemId(position: Int): Long {
        return chatroomTags[position].id
    }

    override fun getItem(position: Int): Tag? {
        return if (chatroomTags.isNotEmpty() && position < chatroomTags.size)
            return chatroomTags[position]
        else null
    }

    private class ChatroomTagHolder(var chatroomCardBinding: ChatroomTagCardBinding? = null)

    /**
     * Credit to @sergi from https://stackoverflow.com/questions/33943717/android-data-binding-with-custom-adapter
     * for the implementation of data-binding within this method using convertView's tag to assign data binding instance
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val chatroomTagHolder: ChatroomTagHolder
        var tagCard = convertView
        val tag: Tag? = getItem(position)
        if (tagCard == null) {
            chatroomTagHolder = ChatroomTagHolder(
                DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.chatroom_tag_card, parent, false)
            )

            tagCard = chatroomTagHolder.chatroomCardBinding!!.root
            tagCard.tag = chatroomTagHolder
        } else {
            chatroomTagHolder = tagCard.tag as ChatroomTagHolder
        }

        chatroomTagHolder.chatroomCardBinding!!.tagName.setBackgroundColor(
           ColourUtils.getColourOrGreen(tag?.colour)
        )
        chatroomTagHolder.chatroomCardBinding!!.tag = tag

        return tagCard  // should be init'ed in any valid case
    }


    fun setTags(chatroomTags: List<Tag>) {
        this.chatroomTags = chatroomTags
        notifyDataSetChanged()
    }
}