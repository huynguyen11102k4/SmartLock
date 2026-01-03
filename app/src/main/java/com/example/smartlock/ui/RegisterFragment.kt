package com.example.smartlock.ui

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.smartlock.R
import com.example.smartlock.databinding.FragmentRegisterBinding
import com.example.smartlock.viewmodel.AuthUiState
import com.example.smartlock.viewmodel.AuthViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    private var googleToken: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            val args = RegisterFragmentArgs.fromBundle(it)
            googleToken = args.googleToken
            if (!args.googleEmail.isNullOrEmpty()) {
                binding.etEmail.setText(args.googleEmail)
            }
            if (!args.googleName.isNullOrEmpty()) {
                binding.etName.setText(args.googleName)
            }
        }

        setupClickListeners()
        setupObservers()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            performRegister()
        }

        binding.tvLoginPrompt.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun performRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (!validateInput(name, email, password)) return

        viewModel.register(email, password, name)
    }

    private fun validateInput(name: String, email: String, password: String): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.tilName.error = "Vui lòng nhập họ và tên"
            isValid = false
        } else {
            binding.tilName.error = null
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = "Vui lòng nhập email"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Email không hợp lệ"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$"
        val passwordMatcher = Regex(passwordPattern)

        if (password.isEmpty()) {
            binding.tilPassword.error = "Vui lòng nhập mật khẩu"
            isValid = false
        } else if (!passwordMatcher.matches(password)) {
            binding.tilPassword.error = "Mật khẩu tối thiểu 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        return isValid
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AuthUiState.Loading -> showLoading(true)

                    is AuthUiState.OtpSent -> {
                        showLoading(false)
                        val email = binding.etEmail.text.toString().trim()
                        val password = binding.etPassword.text.toString().trim() // Lấy password từ UI

                        val action = RegisterFragmentDirections
                            .actionRegisterFragmentToVerifyOtpFragment(
                                email = email,
                                googleToken = googleToken,
                                password = password
                            )
                        findNavController().navigate(action)
                        viewModel.resetState()
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
        binding.btnRegister.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.resetState()
        _binding = null
    }
}