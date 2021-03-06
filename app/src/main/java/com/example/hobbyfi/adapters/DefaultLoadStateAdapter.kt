package com.example.hobbyfi.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.DefaultRefreshListHeaderBinding

// FIXME: ever-so slightly coupled here
class DefaultLoadStateAdapter(
    private inline val retry: () -> Unit,
    private inline val onCreateChatroomButton: ((view: View) -> Unit)?,
    private var userOwnedChatroomIds: List<Long> = emptyList(),
    private val showOnlyProgessBar: Boolean = false
) : LoadStateAdapter<DefaultLoadStateAdapter.DefaultLoaderViewHolder>() {

    class DefaultLoaderViewHolder(val binding: DefaultRefreshListHeaderBinding, private inline val retry: () -> Unit) : RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun getInstance(parent: ViewGroup, retry: () -> Unit): DefaultLoaderViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.default_refresh_list_header, parent, false)
                val binding = DefaultRefreshListHeaderBinding.bind(view)

                return DefaultLoaderViewHolder(binding, retry)
            }
        }

        init {
            binding.refreshPageButton.setOnClickListener {
                retry()
            }
        }

        fun bind(loadState: LoadState, userOwnedChatroomIds: List<Long>, showOnlyProgessBar: Boolean) {
            Log.i("DefaultLoadStateA", "Binding views by loadState: $loadState")
            with(binding) {
                if(!showOnlyProgessBar) {
                    listErrorHeader.isVisible = loadState !is LoadState.Loading
                    listErrorHeader.text = itemView.context.resources.getString(if(loadState is LoadState.Error || userOwnedChatroomIds.size > 100)
                        R.string.list_error_text else R.string.list_suggest_text)

                    refreshPageButton.isVisible = loadState is LoadState.Error
                    chatroomCreateButton.isVisible = userOwnedChatroomIds.size < 100
                } else {
                    // TODO: Maybe add error header and refresh page button on error?
                    listErrorHeader.isVisible = false
                    refreshPageButton.isVisible = false
                    chatroomCreateButton.isVisible = false
                }

                progressBar.isVisible = loadState is LoadState.Loading
            }
        }
    }

    override fun onBindViewHolder(holder: DefaultLoaderViewHolder, loadState: LoadState) {
        holder.bind(loadState, userOwnedChatroomIds, showOnlyProgessBar)
        holder.binding.chatroomCreateButton.setOnClickListener {
            onCreateChatroomButton?.invoke(it)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): DefaultLoaderViewHolder {
        return DefaultLoaderViewHolder.getInstance(parent, retry)
    }

    fun setUserChatroomOwnershipIds(ownershipIds: List<Long>) {
        userOwnedChatroomIds = ownershipIds
        notifyDataSetChanged()
    }

    override fun displayLoadStateAsItem(loadState: LoadState): Boolean {
        return loadState is LoadState.Loading || loadState is LoadState.Error || loadState is LoadState.NotLoading
    }
}