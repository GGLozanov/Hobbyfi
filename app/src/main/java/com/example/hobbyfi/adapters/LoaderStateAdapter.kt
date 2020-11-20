package com.example.hobbyfi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.R

// TODO: Use this adapter with the withLoadStateFooter() method of the PagedListAdapter
class LoaderStateAdapter(private inline val retry: () -> Unit) :
    LoadStateAdapter<LoaderStateAdapter.LoaderViewHolder>() {

    class LoaderViewHolder(view: View, retry: () -> Unit) : RecyclerView.ViewHolder(view) {
        // TODO: Init button & error text

        companion object {
            fun getInstance(parent: ViewGroup, retry: () -> Unit): LoaderViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val view = inflater.inflate(R.layout.refresh_list_header, parent, false)
                return LoaderViewHolder(view, retry)
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

    override fun onBindViewHolder(holder: LoaderViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoaderViewHolder {
        return LoaderViewHolder.getInstance(parent, retry)
    }
}