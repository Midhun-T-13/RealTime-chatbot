package com.example.realtimechatapplicaition.ui.chatlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.realtimechatapplicaition.ChatApplication
import com.example.realtimechatapplicaition.R
import com.example.realtimechatapplicaition.databinding.FragmentChatListBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ChatListFragment : Fragment() {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChatListViewModel
    private lateinit var chatAdapter: ChatListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        setupFab()
        setupWindowInsets()
        observeState()
        observeEvents()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply bottom margin to FAB for navigation bar
            val fabLayoutParams = binding.fabNewChat.layoutParams as ViewGroup.MarginLayoutParams
            fabLayoutParams.bottomMargin = systemBarsInsets.bottom + 16.dpToPx()
            binding.fabNewChat.layoutParams = fabLayoutParams

            // Apply bottom padding to RecyclerView
            binding.rvChats.setPadding(
                binding.rvChats.paddingLeft,
                binding.rvChats.paddingTop,
                binding.rvChats.paddingRight,
                systemBarsInsets.bottom + 72.dpToPx()
            )

            insets
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun setupViewModel() {
        val app = requireActivity().application as ChatApplication
        val username = app.currentUser?.username ?: ""
        viewModel = ViewModelProvider(
            this,
            ChatListViewModel.Factory(app.chatRepository, app.apiService, username)
        )[ChatListViewModel::class.java]
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatListAdapter { chat ->
            viewModel.selectChat(chat.id)
        }

        binding.rvChats.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatAdapter
        }

        // Setup swipe to delete
        val swipeToDeleteCallback = SwipeToDeleteCallback(requireContext()) { position ->
            viewModel.deleteChat(position)
        }
        ItemTouchHelper(swipeToDeleteCallback).attachToRecyclerView(binding.rvChats)
    }

    private fun setupFab() {
        binding.fabNewChat.setOnClickListener {
            viewModel.createNewChat()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update chat list
                    chatAdapter.submitList(state.chats)

                    // Empty state
                    binding.emptyStateContainer.visibility =
                        if (state.isEmpty) View.VISIBLE else View.GONE
                    binding.rvChats.visibility =
                        if (state.isEmpty) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is ChatListEvent.NavigateToChat -> {
                            navigateToChat(event.chatId)
                        }
                        is ChatListEvent.ShowError -> {
                            showError(event.message)
                        }
                        is ChatListEvent.ShowSuccess -> {
                            showSuccess(event.message)
                        }
                    }
                }
            }
        }
    }

    private fun navigateToChat(chatId: String) {
        val bundle = Bundle().apply {
            putString("chatId", chatId)
        }
        findNavController().navigate(R.id.action_chatListFragment_to_chatDetailFragment, bundle)
    }

    private fun showError(message: String) {
        Snackbar.make(binding.main, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.main, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
