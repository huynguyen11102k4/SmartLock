package com.example.smartlock.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.bumptech.glide.Glide
import com.example.smartlock.MainActivity
import com.example.smartlock.R
import com.example.smartlock.databinding.FragmentAccountSettingsBinding
import com.example.smartlock.viewmodel.UserUiState
import com.example.smartlock.viewmodel.UserViewModel
import com.example.smartlock.viewmodel.AuthViewModel
import com.example.smartlock.viewmodel.AuthUiState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.*

@AndroidEntryPoint
class AccountSettingsFragment : Fragment() {
    private var _binding: FragmentAccountSettingsBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadAvatar(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAccountSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        userViewModel.getUserProfile()

        binding.btnBackIcon.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupUI() {
        binding.ivUserAvatar.setOnClickListener { pickImage.launch("image/*") }

        binding.etBirthday.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                binding.etBirthday.setText("$d/${m + 1}/$y")
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val dobDisplay = binding.etBirthday.text.toString().trim()
            val newPass = binding.etNewPassword.text.toString().trim()

            binding.tilName.error = null
            binding.tilBirthday.error = null

            if (name.isEmpty()) {
                binding.tilName.error = "Không được để trống tên"
                return@setOnClickListener
            }

            userViewModel.updateName(name)

            if (phone.isNotEmpty()) userViewModel.updatePhoneNumber(phone)

            if (dobDisplay.isNotEmpty()) {
                val isoDate = formatToIsoDate(dobDisplay)
                if (isoDate != null) {
                    userViewModel.updateDateOfBirth(isoDate)
                } else {
                    binding.tilBirthday.error = "Định dạng ngày phải là dd/MM/yyyy"
                    return@setOnClickListener
                }
            }

            if (newPass.isNotEmpty()) {
                val oldPass = binding.etVerifyPassword.text.toString()
                if (oldPass.isEmpty()) {
                    binding.tilVerifyPassword.error = "Nhập mật khẩu hiện tại"
                } else {
                    authViewModel.changePassword(oldPass, newPass)
                }
            }
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.currentUser.collect { user ->
                user?.let {
                    binding.etName.setText(it.name)
                    binding.etPhone.setText(it.phoneNumber)
                    if (!it.dateOfBirth.isNullOrEmpty()) {
                        // Chuyển "2014-01-09T00:00:00" -> "09/01/2014" rồi gán vào EditText
                        binding.etBirthday.setText(formatIsoToDisplay(it.dateOfBirth))
                    }
                    binding.ivUserAvatar.load(it.avatarUrl) {
                        placeholder(com.example.smartlock.R.drawable.ic_person)
                        error(com.example.smartlock.R.drawable.ic_person)
                        transformations(coil.transform.CircleCropTransformation())
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.uiState.collect { state ->
                if (state is UserUiState.ProfileUpdated) {
                    Toast.makeText(context, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else if (state is UserUiState.Error) {
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.uiState.collect { state ->
                when (state) {
                    is AuthUiState.LoggedOut -> {
                        Toast.makeText(context, "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show()

                        val intent = Intent(requireContext(), MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)

                        requireActivity().finish()
                    }
                    is AuthUiState.Error -> {
                        if (state.message.contains("Logout", ignoreCase = true) || state.message.contains("415")) {
                            val intent = Intent(requireContext(), MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            requireActivity().finish()
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun uploadAvatar(uri: Uri) {
        val file = uriToFile(uri)
        val mimeType = requireContext().contentResolver.getType(uri) ?: "image/jpeg"
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())

        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        userViewModel.updateAvatar(body, isRandom = false)
    }

    private fun uriToFile(uri: Uri): File {
        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(requireContext().contentResolver.getType(uri)) ?: "jpg"

        val file = File(requireContext().cacheDir, "avatar_upload.$extension")

        requireContext().contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun formatToIsoDate(displayDate: String): String? {
        return try {
            val inputFormat = java.text.SimpleDateFormat("dd/M/yyyy", java.util.Locale.getDefault())
            val date = inputFormat.parse(displayDate)

            val outputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
            outputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            outputFormat.format(date!!)
        } catch (e: Exception) {
            null
        }
    }

    private fun formatIsoToDisplay(isoDate: String): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            val date = inputFormat.parse(isoDate)

            val outputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            outputFormat.format(date!!)
        } catch (e: Exception) {
            isoDate
        }
    }

    private fun showLogoutConfirmation() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_logout, null)
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogTheme) // Sử dụng theme trong suốt cho Dialog
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnConfirm = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmLogout)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            authViewModel.logout()
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}