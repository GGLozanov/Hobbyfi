package com.example.hobbyfi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.DefaultRefreshListHeaderBinding

// TODO: Use this adapter with the withLoadStateFooter() method of the PagedListAdapter
class DefaultLoadStateAdapter(
    private inline val retry: () -> Unit,
    private val createChatroomButtonCallback: OnCreateChatroomButtonPressed? = null) :
    LoadStateAdapter<DefaultLoadStateAdapter.DefaultLoaderViewHolder>() {

    interface OnCreateChatroomButtonPressed {
        fun onCreateChatroomButtonPress(view: View)
    }

    class DefaultLoaderViewHolder(val binding: DefaultRefreshListHeaderBinding, view: View, private val retry: () -> Unit) : RecyclerView.ViewHolder(view) {

        companion object {
            fun getInstance(parent: ViewGroup, retry: () -> Unit): DefaultLoaderViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding = DefaultRefreshListHeaderBinding.inflate(inflater, parent, false)

                return DefaultLoaderViewHolder(binding, binding.root, retry)
            }
        }

        init {
//            view.findViewById<Button>(R.id.btnRetry).setOnClickListener {
//                retry()
//            }
            // TODO: Update callback once UI is setup
        }

        fun bind(loadState: LoadState) {
            //errorText.visibility = toVisibility(loadState is LoadState.Error)
            //button.visibilit = toVisibility(loadState is LoadState.Error)
        }

        private fun toVisibility(constraint: Boolean): Int = if (constraint) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun onBindViewHolder(holder: DefaultLoaderViewHolder, loadState: LoadState) {
        if(createChatroomButtonCallback == null) {
            holder.binding.chatroomCreateButton.visibility = View.GONE
        }
        holder.bind(loadState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): DefaultLoaderViewHolder {
        return DefaultLoaderViewHolder.getInstance(parent, retry)
    }
}