package com.example.smartlock.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent.KEYCODE_DEL
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.smartlock.R
import com.example.smartlock.databinding.FragmentVerifyOtpBinding
import com.example.smartlock.viewmodel.AuthUiState
import com.example.smartlock.viewmodel.AuthViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VerifyOtpFragment : Fragment() {
    private var _binding: FragmentVerifyOtpBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private val args: VerifyOtpFragmentArgs by navArgs()

    private val otpEditTexts: Array<EditText> by lazy {
        arrayOf(
            binding.etOtp1, binding.etOtp2, binding.etOtp3,
            binding.etOtp4, binding.etOtp5, binding.etOtp6
        )
    }

    private var countdownJob: Job? = null
    private val RESEND_DELAY = 60

    private val email: String by lazy { args.email }
    private val newPassword: String by lazy { args.newPassword.orEmpty() }
    private val isForgotPasswordFlow: Boolean by lazy { newPassword.isNotEmpty() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerifyOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isForgotPasswordFlow) {
            binding.btnVerify.text = "Đặt lại mật khẩu"
        } else {
            binding.btnVerify.text = "Xác nhận đăng ký"
        }

        setupOtpInput()
        setupClickListeners()
        setupObservers()
        startResendCountdown()
    }

    private fun setupOtpInput(){
        otpEditTexts.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher{
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                }

                override fun afterTextChanged(s: Editable?) {
                    val text = s.toString()
                    if (text.length == 1) {
                        if (index < 5) otpEditTexts[index + 1].requestFocus()
                    } else if (text.isEmpty() && index > 0) {
                        otpEditTexts[index - 1].requestFocus()
                    }

                    if (getOtpCode().length == 6) {
                        verifyOtp(getOtpCode())
                    }
                }
            })

            editText.setOnKeyListener { _, keyCode, event ->
                if(keyCode == KEYCODE_DEL && editText.text.isEmpty() && index > 0){
                    otpEditTexts[index - 1].requestFocus()
                }
                false
            }
        }
    }

    private fun getOtpCode(): String {
        return otpEditTexts.joinToString("") { it.text.toString() }
    }

    private fun verifyOtp(otp: String) {
        viewModel.apply {
            if (isForgotPasswordFlow) {
                viewModel.verifyForgotPasswordOtp(email, otp, newPassword)
            } else {
                viewModel.verifyRegisterOtp(email, otp)
            }
        }
    }

    private fun setupClickListeners(){
        binding.btnVerify.setOnClickListener {
            val otp = getOtpCode()
            if(otp.length == 6){
                verifyOtp(otp)
            }else{
                Snackbar.make(binding.root, "Vui lòng nhập mã OTP gồm 6 chữ số", Snackbar.LENGTH_LONG).show()
            }
        }

        binding.tvResendOtp.setOnClickListener {
            if (binding.tvResendOtp.isEnabled) {
                resendOtp()
            }
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun resendOtp(){
        viewLifecycleOwner.lifecycleScope.launch {
            if(isForgotPasswordFlow){
                viewModel.sendForgotPasswordOtp(email)
            } else {
                viewModel.resendRegisterOtp(email)
            }
            Snackbar.make(binding.root, "Mã OTP đã được gửi lại", Snackbar.LENGTH_LONG).show()
            startResendCountdown()
        }
    }

    private fun startResendCountdown() {
        binding.tvResendOtp.isEnabled = false
        var timeLeft = RESEND_DELAY

        countdownJob?.cancel()
        countdownJob = viewLifecycleOwner.lifecycleScope.launch {
            while (timeLeft > 0) {
                binding.tvResendOtp.text = "Gửi lại mã sau ${timeLeft}s"
                delay(1000)
                timeLeft--
            }
            binding.tvResendOtp.text = "Gửi lại mã"
            binding.tvResendOtp.isEnabled = true
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AuthUiState.Loading -> showLoading()
                    is AuthUiState.RegisterSuccess -> {
                        val token = args.googleToken
                        val email = args.email
                        val password = args.password

                        if (!token.isNullOrEmpty() && !password.isNullOrEmpty()) {
                            viewModel.login(email, password)
                        } else {
                            hideLoading()
                            Snackbar.make(binding.root, "Đăng ký thành công! Hãy đăng nhập.", Snackbar.LENGTH_LONG).show()
                            findNavController().popBackStack(R.id.loginFragment, false)
                        }
                    }

                    is AuthUiState.LoginSuccess -> {
                        val token = args.googleToken
                        if (!token.isNullOrEmpty()) {
                            viewModel.linkOAuth("google", token)
                        } else {
                            findNavController().navigate(R.id.action_verifyOtpFragment_to_doorListFragment)
                        }
                    }

                    is AuthUiState.OAuthLinked -> {
                        hideLoading()
                        Snackbar.make(binding.root, "Đăng ký và liên kết Google thành công!", Snackbar.LENGTH_LONG).show()
                        // Sau khi liên kết xong, về màn hình đăng nhập
                        findNavController().popBackStack(R.id.loginFragment, false)
                    }
                    is AuthUiState.ForgotPasswordVerified -> {
                        hideLoading()
                        Snackbar.make(binding.root, "Đặt lại mật khẩu thành công!", Snackbar.LENGTH_LONG).show()
                        findNavController().popBackStack(R.id.loginFragment, false)
                    }
                    is AuthUiState.Error -> {
                        hideLoading()
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        clearOtpFields()
                    }
                    else -> hideLoading()
                }
            }
        }
    }

    private fun clearOtpFields() {
        otpEditTexts.forEach { it.text?.clear() }
        otpEditTexts[0].requestFocus()
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnVerify.isEnabled = false
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.btnVerify.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownJob?.cancel()
        viewModel.resetState()
        _binding = null
    }
}