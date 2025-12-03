package com.example.realtimechatapplicaition.ui.login

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.realtimechatapplicaition.ChatApplication
import com.example.realtimechatapplicaition.R
import com.example.realtimechatapplicaition.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    companion object {
        private const val TAG = "LoginFragment"
    }

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnSubmit.setOnClickListener {
            submitUsername()
        }

        binding.etUsername.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitUsername()
                true
            } else {
                false
            }
        }
    }

    private fun submitUsername() {
        val username = binding.etUsername.text?.toString()?.trim() ?: ""

        if (username.isEmpty()) {
            binding.tilUsername.error = "Please enter your username"
            return
        }

        binding.tilUsername.error = null
        showLoading(true)
        hideError()

        val app = requireActivity().application as ChatApplication

        Log.d(TAG, "Attempting login with username: $username")

        viewLifecycleOwner.lifecycleScope.launch {
            val result = app.apiService.getUser(username)

            result.onSuccess { user ->
                Log.d(TAG, "API Success - User ID: ${user.id}, Username: ${user.username}")

                // Save user info
                app.currentUser = user

                // Navigate to chat list
                findNavController().navigate(R.id.action_loginFragment_to_chatListFragment)
            }.onFailure { exception ->
                Log.e(TAG, "API Failure - Error: ${exception.message}", exception)

                showLoading(false)
                showError(exception.message ?: "Something went wrong")
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSubmit.isEnabled = !show
        binding.etUsername.isEnabled = !show
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
