package com.example.hobbyfi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.R

// TODO: Use this adapter with the withLoadStateFooter() method of the PagedListAdapter
class DefaultLoadStateAdapter(
    private inline val retry: () -> Unit,
    private val createChatroomButtonCallback: OnCreateChatroomButtonPressed? = null) :
    LoadStateAdapter<DefaultLoadStateAdapter.DefaultLoaderViewHolder>() {

    interface OnCreateChatroomButtonPressed {
        fun onCreateChatroomButtonPress(view: View)
    }

    class DefaultLoaderViewHolder(view: View, private val retry: () -> Unit) : RecyclerView.ViewHolder(view) {
        // TODO: Init button & error text

        companion object {
            fun getInstance(parent: ViewGroup, retry: () -> Unit): DefaultLoaderViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val view = inflater.inflate(R.layout.default_refresh_list_header, parent, false)
                return DefaultLoaderViewHolder(view, retry)
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
        holder.bind(loadState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): DefaultLoaderViewHolder {
        return DefaultLoaderViewHolder.getInstance(parent, retry)
    }
}