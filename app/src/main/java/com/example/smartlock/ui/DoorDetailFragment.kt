package com.example.smartlock.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.smartlock.R
import com.example.smartlock.databinding.DoorDetailFragmentBinding
import com.example.smartlock.model.entity.Door
import com.example.smartlock.viewmodel.DoorViewModel
import com.example.smartlock.viewmodel.DoorUiState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class DoorDetailFragment : Fragment() {
    private var _binding: DoorDetailFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DoorViewModel by viewModels()
    private val args: DoorDetailFragmentArgs by navArgs()

    private var isLocked: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DoorDetailFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val doorId = args.doorId
        viewModel.setCurrentDoor(doorId)

        viewModel.refreshDoorDetails(doorId)

        setupAnimations()
        observeDoorData()
        setupSliderLogic()
        setupNavigationClickListeners()

        binding.btnBackIcon.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupAnimations() {
        binding.cardStatus.apply {
            translationY = 100f
            alpha = 0f
            animate().translationY(0f).alpha(1f).setDuration(400).start()
        }
        view?.post { animateToLocked() }
    }

    private fun observeDoorData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentDoor.collect { door ->
                    door?.let {
                        binding.tvDoorName.text = it.name
                        binding.tvBattery.text = "${it.battery}%"

                        when (it.state) {
                            "Lock" -> if (!isLocked) animateToLocked()
                            "Unlock" -> if (isLocked) animateToUnlocked()
                            "Unknown" -> {
                                animateToLocked()
                            }
                        }

                        applyPermissions(it)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentDoor.collect { door ->
                    door?.let {
                        binding.tvDoorName.text = it.name
                        binding.tvBattery.text = "${it.battery}%"
                        applyPermissions(it)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is DoorUiState.CommandSuccess -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                            viewModel.resetState()
                        }
                        is DoorUiState.Error -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                            // Nếu lỗi, có thể cần reset lại trạng thái Slider về đúng thực tế
                            viewModel.resetState()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun applyPermissions(door: Door) {
        val p = door.permission
        val currentTime = System.currentTimeMillis()

        if (p == 3) {
            val parseIsoToLong = { isoString: String? ->
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    sdf.parse(isoString ?: "")?.time
                } catch (e: Exception) {
                    null
                }
            }

            val validFromLong = parseIsoToLong(door.validFrom)
            val validToLong = parseIsoToLong(door.validTo)

            val isExpired = validToLong != null && currentTime > validToLong
            val isNotStarted = validFromLong != null && currentTime < validFromLong

            if (isExpired || isNotStarted) {
                disableControlSlider("Truy cập hết hạn hoặc chưa tới giờ")
                hideAllManagementCards()
                return
            }
        }

        val isManager = (p == 0 || p == 1)

        binding.cardSendEKey.visibility = if (isManager) View.VISIBLE else View.GONE
        binding.cardEKeys.visibility = if (isManager) View.VISIBLE else View.GONE
        binding.cardICCard.visibility = if (isManager) View.VISIBLE else View.GONE
        binding.cardPasscodes.visibility = if (isManager) View.VISIBLE else View.GONE
        binding.cardGeneratePasscode.visibility = if (isManager) View.VISIBLE else View.GONE

        binding.cardRecords.visibility = View.VISIBLE

        binding.btnSettingsIcon.visibility = if (p == 0) View.VISIBLE else View.GONE
    }

    private fun disableControlSlider(message: String) {
        binding.lockSlider.alpha = 0.5f
        binding.thumb.isEnabled = false
        binding.tvArrow.text = message
    }

    private fun hideAllManagementCards() {
        val cards = listOf(
            binding.cardSendEKey, binding.cardICCard, binding.cardEKeys,
            binding.cardPasscodes, binding.cardGeneratePasscode,
        )
        cards.forEach { it.visibility = View.GONE }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSliderLogic() {
        binding.thumb.setOnTouchListener { v, event ->
            val maxTranslationX = binding.lockSlider.width - binding.thumb.width -
                    (binding.lockSlider.paddingStart + binding.lockSlider.paddingEnd)

            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    var newX = event.rawX - binding.lockSlider.x - v.width / 2
                    newX = newX.coerceIn(0f, maxTranslationX.toFloat())
                    v.translationX = newX

                    updateIconAlpha(newX, maxTranslationX.toFloat())

                    if (isLocked && newX > maxTranslationX * 0.9f) {
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (isLocked) {
                        if (v.translationX > maxTranslationX * 0.75f) {
                            showUnlockDialog()
                        } else {
                            animateToLocked()
                        }
                    } else {
                        if (v.translationX < maxTranslationX * 0.25f) {
                            performLockAction()
                        } else {
                            animateToUnlocked()
                        }
                    }
                }
            }
            true
        }
    }

    private fun showUnlockDialog() {
        val door = viewModel.currentDoor.value

        if (door?.doorCode.isNullOrBlank()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Yêu cầu thiết lập")
                .setMessage("Cửa này chưa được thiết lập mã (Door Code). Bạn cần tạo mã cửa trong phần cài đặt trước khi mở khóa.")
                .setNegativeButton("Đóng", null)
                .show()

            animateToLocked()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_unlock, null)
        val etPasscode = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPasscode)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnConfirm = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmUnlock)

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogTheme)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
            animateToLocked()
        }

        btnConfirm.setOnClickListener {
            val enteredPass = etPasscode.text.toString()

            if (enteredPass == door.doorCode) {
                viewModel.unlockDoor(args.doorId)

                animateToUnlocked()
                Toast.makeText(requireContext(), "Đang gửi lệnh mở khóa...", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                etPasscode.error = "Mã cửa không chính xác"
            }
        }

        dialog.show()
    }

    private fun performLockAction() {
        viewModel.lockDoor(args.doorId)

        animateToLocked()
        Toast.makeText(requireContext(), "Đang gửi lệnh khóa...", Toast.LENGTH_SHORT).show()
    }


    private fun setupNavigationClickListeners() {
        binding.cardPasscodes.setOnClickListener {
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToPasscodesFragment(args.doorId)
            findNavController().navigate(action)
        }

        binding.cardICCard.setOnClickListener {
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToIcCardFragment(args.doorId)
            findNavController().navigate(action)
        }

        binding.cardRecords.setOnClickListener {
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToRecordsFragment(args.doorId)
            findNavController().navigate(action)
        }

        binding.cardSendEKey.setOnClickListener {
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToSendEKeyFragment(args.doorId)
            findNavController().navigate(action)
        }

        binding.cardEKeys.setOnClickListener {
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToEKeysFragment(args.doorId)
            findNavController().navigate(action)
        }

        binding.cardGeneratePasscode.setOnClickListener {
            val action = DoorDetailFragmentDirections.actionDoorDetailFragmentToGenerateEKeyFragment(args.doorId)
            findNavController().navigate(action)
        }
    }


    private fun animateToLocked() {
        binding.thumb.animate().translationX(0f).setDuration(300).setInterpolator(DecelerateInterpolator()).start()
        binding.icThumb.setImageResource(R.drawable.ic_lock)
        binding.tvArrow.text = "› › ›"
        isLocked = true
    }

    private fun animateToUnlocked() {
        val max = binding.lockSlider.width - binding.thumb.width - (binding.lockSlider.paddingStart * 2)
        binding.thumb.animate().translationX(max.toFloat()).setDuration(300).setInterpolator(DecelerateInterpolator()).start()
        binding.icThumb.setImageResource(R.drawable.ic_unlock)
        binding.tvArrow.text = "‹ ‹ ‹"
        isLocked = false
    }

    private fun updateIconAlpha(translationX: Float, maxX: Float) {
        val progress = (translationX / maxX).coerceIn(0f, 1f)
        binding.icLockInactive.alpha = 0.3f * (1f - progress)
        binding.icUnlockInactive.alpha = 0.3f * progress
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}