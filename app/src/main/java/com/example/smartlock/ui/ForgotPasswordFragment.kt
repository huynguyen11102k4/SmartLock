package com.example.smartlock.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.smartlock.R
import com.example.smartlock.databinding.FragmentForgotPasswordBinding
import com.example.smartlock.viewmodel.AuthUiState
import com.example.smartlock.viewmodel.AuthViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment() {
    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupObservers()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.btnSendOtp.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val newPass = binding.etNewPassword.text.toString().trim()
            val confirmPass = binding.etConfirmPassword.text.toString().trim()

            if (validateInput(email, newPass, confirmPass)) {
                viewModel.sendForgotPasswordOtp(email)
            }
        }

        binding.tvBackToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_forgotPasswordFragment_to_loginFragment)
        }
    }

    private fun validateInput(email: String, newPass: String, confirmPass: String): Boolean {
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Email không hợp lệ"
            return false
        }
        binding.tilEmail.error = null

        val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$"
        val passwordMatcher = Regex(passwordPattern)

        if (newPass.isEmpty()) {
            binding.tilNewPassword.error = "Vui lòng nhập mật khẩu"
            return false
        }

        if (!passwordMatcher.matches(newPass)) {
            binding.tilNewPassword.error = "Mật khẩu tối thiểu 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt"
            return false
        }
        binding.tilNewPassword.error = null

        if (confirmPass != newPass) {
            binding.tilConfirmPassword.error = "Mật khẩu xác nhận không khớp"
            return false
        }
        binding.tilConfirmPassword.error = null

        return true
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AuthUiState.Loading -> showLoading(true)
                    is AuthUiState.OtpSent -> {
                        showLoading(false)
                        val email = binding.etEmail.text.toString().trim()
                        val newPassword = binding.etNewPassword.text.toString().trim()

                        val action = ForgotPasswordFragmentDirections
                            .actionForgotPasswordFragmentToVerifyOtpFragment(email = email, newPassword = newPassword)
                        findNavController().navigate(action)
                    }
                    is AuthUiState.Error -> {
                        showLoading(false)
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    }
                    else -> showLoading(false)
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSendOtp.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.resetState()
        _binding = null
    }
}