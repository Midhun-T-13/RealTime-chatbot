package com.example.realtimechatapplicaition.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.realtimechatapplicaition.ChatApplication
import com.example.realtimechatapplicaition.databinding.FragmentChatDetailBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ChatDetailFragment : Fragment() {

    private var _binding: FragmentChatDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChatDetailViewModel
    private lateinit var messageAdapter: MessageAdapter

    private val chatId: String by lazy {
        arguments?.getString("chatId")
            ?: throw IllegalArgumentException("Chat ID is required")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupHeader()
        setupRecyclerView()
        setupWindowInsets()
        setupInput()
        observeState()
        observeEvents()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply bottom margin to inputContainer so it moves up with the keyboard
            val bottomMargin = if (imeInsets.bottom > 0) imeInsets.bottom else systemBarsInsets.bottom
            val layoutParams = binding.inputContainer.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.bottomMargin = bottomMargin
            binding.inputContainer.layoutParams = layoutParams

            // Scroll to bottom when keyboard appears
            if (imeInsets.bottom > 0) {
                binding.rvMessages.post {
                    val count = messageAdapter.itemCount
                    if (count > 0) {
                        binding.rvMessages.scrollToPosition(count - 1)
                    }
                }
            }

            insets
        }
    }

    private fun setupViewModel() {
        val app = requireActivity().application as ChatApplication
        val username = app.currentUser?.username ?: ""
        viewModel = ViewModelProvider(
            this,
            ChatDetailViewModel.Factory(
                chatId,
                app.chatRepository,
                app.socketService,
                app.apiService,
                username,
                app.networkMonitor
            )
        )[ChatDetailViewModel::class.java]
    }

    private fun setupHeader() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter { message ->
            // Retry sending failed message
            viewModel.retryMessage(message)
        }

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupInput() {
        // The @AI prefix is now a separate TextView in the layout, not editable

        binding.fabSend.setOnClickListener {
            sendMessage()
        }

        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun sendMessage() {
        val messageContent = binding.etMessage.text?.toString()?.trim() ?: return
        if (messageContent.isEmpty()) return

        // Prepend @AI to the message since it's now a fixed prefix
        val fullMessage = "@AI $messageContent"
        viewModel.sendMessage(fullMessage)

        // Clear only the user input, the @AI prefix stays in its separate view
        binding.etMessage.text?.clear()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update title
                    state.chat?.let { chat ->
                        binding.tvTitle.text = chat.title
                    }

                    // Update messages
                    messageAdapter.submitList(state.messages) {
                        if (state.messages.isNotEmpty()) {
                            binding.rvMessages.scrollToPosition(state.messages.size - 1)
                        }
                    }

                    // Empty state
                    binding.emptyMessagesContainer.visibility =
                        if (state.messages.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvMessages.visibility =
                        if (state.messages.isEmpty()) View.GONE else View.VISIBLE

                    // Offline banner
                    binding.tvOfflineBanner.visibility =
                        if (state.isOffline) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is ChatDetailEvent.MessageSent -> {
                            // Scroll to bottom when message sent
                            val count = messageAdapter.itemCount
                            if (count > 0) {
                                binding.rvMessages.smoothScrollToPosition(count - 1)
                            }
                        }
                        is ChatDetailEvent.ShowOfflineToast -> {
                            showOfflineToast()
                        }
                        is ChatDetailEvent.ShowError -> {
                            showError(event.message)
                        }
                    }
                }
            }
        }
    }

    private fun showOfflineToast() {
        Toast.makeText(
            requireContext(),
            "You're offline. Messages will be sent once you're back online.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.main, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
