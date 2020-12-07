package com.example.hobbyfi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.DefaultRefreshListHeaderBinding

class DefaultLoadStateAdapter(
    private inline val retry: () -> Unit,
    private inline val onCreateChatroomButton: (view: View) -> Unit) :
    LoadStateAdapter<DefaultLoadStateAdapter.DefaultLoaderViewHolder>() {

    class DefaultLoaderViewHolder(val binding: DefaultRefreshListHeaderBinding, view: View, private inline val retry: () -> Unit) : RecyclerView.ViewHolder(view) {

        companion object {
            fun getInstance(parent: ViewGroup, retry: () -> Unit): DefaultLoaderViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding = DefaultRefreshListHeaderBinding.inflate(inflater, parent, false)

                return DefaultLoaderViewHolder(binding, binding.root, retry)
            }
        }

        init {
            binding.refreshPageButton.setOnClickListener {
                retry()
            }
        }

        fun bind(loadState: LoadState) {
            with(binding) {
                listErrorHeader.text = itemView.context.resources.getString(if(loadState is LoadState.Error)
                    R.string.list_error_text else R.string.list_suggest_text)

                refreshPageButton.isVisible = loadState !is LoadState.Loading
                progressBar.isVisible = loadState is LoadState.Loading
            }
        }
    }

    override fun onBindViewHolder(holder: DefaultLoaderViewHolder, loadState: LoadState) {
        holder.bind(loadState)
        holder.binding.chatroomCreateButton.setOnClickListener {
            onCreateChatroomButton.invoke(it)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): DefaultLoaderViewHolder {
        return DefaultLoaderViewHolder.getInstance(parent, retry)
    }
}