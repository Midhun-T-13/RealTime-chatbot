package com.example.realtimechatapplicaition.ui.chatlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.realtimechatapplicaition.data.model.Chat
import com.example.realtimechatapplicaition.databinding.ItemChatBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatListAdapter(
    private val onChatClick: (Chat) -> Unit
) : ListAdapter<Chat, ChatListAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding, onChatClick)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChatViewHolder(
        private val binding: ItemChatBinding,
        private val onChatClick: (Chat) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

        fun bind(chat: Chat) {
            binding.apply {
                tvChatTitle.text = chat.title
                tvLastMessage.text = chat.lastMessage
                tvTimestamp.text = formatTimestamp(chat.lastMessageTimestamp)

                // Unread badge
                if (chat.unreadCount > 0) {
                    tvUnreadCount.visibility = View.VISIBLE
                    tvUnreadCount.text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString()
                } else {
                    tvUnreadCount.visibility = View.GONE
                }

                // Avatar initial
                tvAvatarInitial.text = chat.title.firstOrNull()?.uppercase() ?: "C"

                root.setOnClickListener { onChatClick(chat) }
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 24 * 60 * 60 * 1000 -> timeFormat.format(Date(timestamp))
                diff < 7 * 24 * 60 * 60 * 1000 -> dateFormat.format(Date(timestamp))
                else -> dateFormat.format(Date(timestamp))
            }
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<Chat>() {
        override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem == newItem
        }
    }
}
