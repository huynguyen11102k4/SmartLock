package com.example.smartlock.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.smartlock.MainActivity
import com.example.smartlock.R
import com.example.smartlock.databinding.FragmentLoginBinding
import com.example.smartlock.viewmodel.AuthUiState
import com.example.smartlock.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGoogleSignIn()
        setupClickListeners()
        setupObservers()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                viewModel.login(email, password)
            }
        }

        binding.btnGoogleLogin.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AuthUiState.Loading -> showLoading()

                    is AuthUiState.LoginSuccess -> {
                        hideLoading()
                        handleLoginSuccess()
                    }

                    is AuthUiState.Error -> {
                        hideLoading()
                        if (state.message.contains("404")) {
                            handleGoogleLoginNotFound()
                        } else {
                            showError(state.message)
                        }
                    }

                    else -> hideLoading()
                }
            }
        }
    }

    private var pendingGoogleToken: String? = null
    private var pendingGoogleAccount: GoogleSignInAccount? = null

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val token = account.idToken
                if (token != null) {
                    pendingGoogleToken = token
                    pendingGoogleAccount = account
                    viewModel.loginWithSocial("google", token)
                }
            } catch (e: ApiException) {
                Log.e("LoginFragment", "Google sign in failed", e)
                showError("Đăng nhập Google thất bại")
            }
        }
    }

    private fun handleGoogleLoginNotFound() {
        val account = pendingGoogleAccount ?: return
        val token = pendingGoogleToken ?: return

        val email = account.email ?: ""
        val name = account.displayName ?: ""

        val action = LoginFragmentDirections
            .actionLoginFragmentToRegisterFragment(googleToken = token, googleEmail = email, googleName = name)
        findNavController().navigate(action)

        Snackbar.make(binding.root, "Tài khoản Google chưa liên kết. Hãy đăng ký để tiếp tục!", Snackbar.LENGTH_LONG).show()

        // Reset pending
        pendingGoogleToken = null
        pendingGoogleAccount = null
    }

    private fun handleLoginSuccess() {
        Toast.makeText(requireContext(), "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
        findNavController().navigate(R.id.action_loginFragment_to_doorListFragment)
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.tilEmail.error = "Vui lòng nhập Email"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Định dạng Email không hợp lệ"
            return false
        }
        binding.tilEmail.error = null

        if (password.isEmpty()) {
            binding.tilPassword.error = "Vui lòng nhập mật khẩu"
            return false
        }
        binding.tilPassword.error = null
        return true
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
        binding.btnGoogleLogin.isEnabled = false
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.btnLogin.isEnabled = true
        binding.btnGoogleLogin.isEnabled = true
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.resetState()
        _binding = null
    }
}