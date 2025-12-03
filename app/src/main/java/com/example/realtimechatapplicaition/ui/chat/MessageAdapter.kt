package com.example.realtimechatapplicaition.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.realtimechatapplicaition.R
import com.example.realtimechatapplicaition.data.model.Message
import com.example.realtimechatapplicaition.data.model.MessageStatus
import com.example.realtimechatapplicaition.databinding.ItemMessageReceivedBinding
import com.example.realtimechatapplicaition.databinding.ItemMessageSentBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val onRetryClick: (Message) -> Unit
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isFromUser) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SentMessageViewHolder(binding, onRetryClick)
            }
            else -> {
                val binding = ItemMessageReceivedBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ReceivedMessageViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SentMessageViewHolder -> holder.bind(getItem(position))
            is ReceivedMessageViewHolder -> holder.bind(getItem(position))
        }
    }

    class SentMessageViewHolder(
        private val binding: ItemMessageSentBinding,
        private val onRetryClick: (Message) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(message: Message) {
            binding.apply {
                tvMessage.text = message.content
                tvTimestamp.text = timeFormat.format(Date(message.timestamp))

                // Status indicator
                when (message.status) {
                    MessageStatus.SENDING -> {
                        ivStatus.setImageResource(R.drawable.ic_clock)
                        ivStatus.setColorFilter(ContextCompat.getColor(root.context, R.color.status_sending))
                        ivRetry.visibility = View.GONE
                    }
                    MessageStatus.SENT -> {
                        ivStatus.setImageResource(R.drawable.ic_check)
                        ivStatus.setColorFilter(ContextCompat.getColor(root.context, R.color.status_sent))
                        ivRetry.visibility = View.GONE
                    }
                    MessageStatus.DELIVERED -> {
                        ivStatus.setImageResource(R.drawable.ic_double_check)
                        ivStatus.setColorFilter(ContextCompat.getColor(root.context, R.color.status_delivered))
                        ivRetry.visibility = View.GONE
                    }
                    MessageStatus.FAILED -> {
                        ivStatus.setImageResource(R.drawable.ic_error)
                        ivStatus.setColorFilter(ContextCompat.getColor(root.context, R.color.status_failed))
                        ivRetry.visibility = View.VISIBLE
                    }
                    MessageStatus.QUEUED -> {
                        ivStatus.setImageResource(R.drawable.ic_queue)
                        ivStatus.setColorFilter(ContextCompat.getColor(root.context, R.color.status_queued))
                        ivRetry.visibility = View.GONE
                    }
                }

                ivRetry.setOnClickListener { onRetryClick(message) }
            }
        }
    }

    class ReceivedMessageViewHolder(
        private val binding: ItemMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(message: Message) {
            binding.apply {
                tvMessage.text = message.content
                tvTimestamp.text = timeFormat.format(Date(message.timestamp))

                // Show sender name if available
                if (message.senderUsername.isNotEmpty()) {
                    tvSenderName.text = message.senderUsername
                    tvSenderName.visibility = View.VISIBLE
                } else {
                    tvSenderName.visibility = View.GONE
                }
            }
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}
