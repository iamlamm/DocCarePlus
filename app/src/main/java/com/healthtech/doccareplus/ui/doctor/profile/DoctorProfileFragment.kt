package com.healthtech.doccareplus.ui.doctor.profile

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.FragmentDoctorProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Rect
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.healthtech.doccareplus.ui.doctor.adapter.CalendarAdapter
import java.util.*
import kotlinx.coroutines.launch
import com.google.android.material.snackbar.Snackbar
import com.healthtech.doccareplus.domain.model.TimeSlot
import com.healthtech.doccareplus.domain.model.TimePeriod
import android.util.Log
import androidx.navigation.fragment.findNavController
import com.healthtech.doccareplus.utils.SnackbarUtils
import com.healthtech.doccareplus.utils.showErrorDialog
import com.healthtech.doccareplus.utils.showInfoDialog
import androidx.activity.OnBackPressedCallback
import com.zegocloud.zimkit.common.ZIMKitRouter
import com.zegocloud.zimkit.common.enums.ZIMKitConversationType

@AndroidEntryPoint
class DoctorProfileFragment : Fragment() {
    private var _binding: FragmentDoctorProfileBinding? = null
    private val binding get() = _binding!!
    private val args: DoctorProfileFragmentArgs by navArgs()
    private val viewModel: DoctorProfileViewModel by viewModels()
    private lateinit var calendarAdapter: CalendarAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Xử lý nút back
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateBack()
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvCalendar.isNestedScrollingEnabled = false
        setupViews()
        setupTimeSlotChips()
        observeState()
        observeTimeSlots()
        setupBookingButton()
    }

    private fun setupViews() {
        displayDoctorInfo()
        setupCalendarRecyclerView()
        setupToolbar()
        setupTimePeriodsChips()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                handleState(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.months.collect { months ->
                setupMonthSpinner(months)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedTimeSlot.collect { timeSlot ->
                timeSlot?.let {
                    Log.d("SelectedTimeSlot", "Selected: ${it.startTime}-${it.endTime}")
                    binding.btnBookAppointment.isEnabled = true
                } ?: run {
                    binding.btnBookAppointment.isEnabled = false
                }
            }
        }
    }

    private fun observeTimeSlots() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.timeSlots.collect { timeSlots ->
                Log.d("TimeSlots", "Received ${timeSlots.size} time slots")
                updateTimeSlots(timeSlots)
            }
        }
    }

    private fun handleState(state: DoctorProfileState) {
        when (state) {
            is DoctorProfileState.Idle -> hideLoading()
            is DoctorProfileState.Loading -> showLoading()
            is DoctorProfileState.BookingLoading -> {
                showLoading()
                binding.btnBookAppointment.isEnabled = false
            }

            is DoctorProfileState.Success -> {
                hideLoading()
                calendarAdapter.setDates(state.datesInMonth)
            }

            is DoctorProfileState.BookingSuccess -> {
                hideLoading()
                binding.btnBookAppointment.isEnabled = true
                showBookingSuccess(state.appointmentId)
                // Navigate to appointment detail or confirmation screen
                findNavController().navigate(
                    DoctorProfileFragmentDirections.actionDoctorProfileToSuccess(state.appointmentId)
                )
            }

            is DoctorProfileState.Error -> {
                hideLoading()
                binding.btnBookAppointment.isEnabled = true

                when (state.message) {
                    "Bạn đã đặt lịch khám vào khung giờ này" -> {
                        showInfoDialog(
                            title = "Đã đặt lịch",
                            message = "Bạn đã đặt lịch khám vào khung giờ này. Vui lòng chọn khung giờ khác hoặc kiểm tra lịch hẹn của bạn."
                        )
                    }

                    "Khung giờ này đã có người đặt lịch" -> {
                        showInfoDialog(
                            title = "Không khả dụng",
                            message = "Khung giờ này đã có người đặt lịch. Vui lòng chọn khung giờ khác."
                        )
                    }

                    "Bác sĩ không thể khám vào khung giờ này" -> {
                        showInfoDialog(
                            title = "Không khả dụng",
                            message = "Bác sĩ không thể khám vào khung giờ này. Vui lòng chọn khung giờ khác."
                        )
                    }

                    else -> showErrorDialog(message = state.message)
                }
            }
            
            is DoctorProfileState.InitiateChat -> {
                // Xử lý chuyển đến màn hình chat
                val action = DoctorProfileFragmentDirections.actionDoctorProfileToChat(
                    conversationId = state.doctorId,
                    title = state.doctorName
                )
                findNavController().navigate(action)
            }
        }
    }

    private fun showLoading() {
        binding.progressBarDoctorProfile.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.progressBarDoctorProfile.visibility = View.GONE
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showBookingSuccess(appointmentId: String) {
        Snackbar.make(
            binding.root,
            "Đặt lịch thành công! Mã cuộc hẹn: $appointmentId",
            Snackbar.LENGTH_LONG
        ).show()
    }

    /**
     * Chuyển đổi định dạng thời gian từ "HH:mm" sang "HH" và thêm hậu tố AM/PM
     */
    private fun formatTimeSlot(timeSlot: TimeSlot): String {
        val startHour = timeSlot.startTime.substringBefore(":")
        val endHour = timeSlot.endTime.substringBefore(":")
        val suffix = when (timeSlot.period) {
            TimePeriod.MORNING -> "AM"
            TimePeriod.AFTERNOON, TimePeriod.EVENING -> "PM"
        }
        return "$startHour-$endHour $suffix"
    }

    private fun updateTimeSlots(timeSlots: List<TimeSlot>) {
        binding.apply {
            val currentPeriod = getSelectedPeriod()

            Log.d("TimeSlots All: ", "$timeSlots")

            val filteredSlots = timeSlots.filter { it.period == currentPeriod }

            Log.d("TimeSlots", "Current period: $currentPeriod")
            Log.d("TimeSlots", "All slots: ${timeSlots.size}, Filtered: ${filteredSlots.size}")
            filteredSlots.forEach { slot ->
                Log.d("TimeSlots", "Filtered slot: ${slot.startTime}-${slot.endTime}")
            }

            val chips = listOf(chipSlot1, chipSlot2, chipSlot3, chipSlot4)
            // Reset all chips
            chips.forEach { chip ->
                chip.isChecked = false
                chip.visibility = View.GONE
            }

            // Update visible chips
            chips.forEachIndexed { index, chip ->
                if (index < filteredSlots.size) {
                    val slot = filteredSlots[index]
                    chip.apply {
                        // Sử dụng hàm formatTimeSlot để hiển thị text trên chip
                        text = formatTimeSlot(slot)
                        isEnabled = true
                        visibility = View.VISIBLE
                        tag = slot
                        Log.d("TimeSlots", "Setting chip $index: ${formatTimeSlot(slot)}")
                    }
                }
            }
        }
    }

    private fun setupMonthSpinner(months: List<String>) {
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, months)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.tvMonth.adapter = adapter

        binding.tvMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.updateCalendarForMonth(months[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupCalendarRecyclerView() {
        calendarAdapter = CalendarAdapter().apply {
            setOnDateClickListener { date ->
                viewModel.onDateSelected(date)
            }
        }

        binding.rvCalendar.apply {
            adapter = calendarAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.right = resources.getDimensionPixelSize(R.dimen.calendar_item_spacing)
                }
            })
        }
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener {
                navigateBack()
            }
            inflateMenu(R.menu.doctor_profile_menu)
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_chat -> {
                        startChatWithDoctor()
                        true
                    }
                    R.id.action_voice_call -> {
                        startVoiceCallWithDoctor()
                        true
                    }
                    R.id.action_video_call -> {
                        startVideoCallWithDoctor()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun navigateBack() {
        findNavController().navigateUp()
    }

    private fun startChatWithDoctor() {
        try {
            // Chuyển đổi ID bác sĩ sang chuỗi
            val doctorIdString = args.doctor.id.toString()
            
            // Gọi trực tiếp vào ZIMKit như trong ví dụ
            ZIMKitRouter.toMessageActivity(
                requireContext(),
                doctorIdString,
                ZIMKitConversationType.ZIMKitConversationTypePeer
            )
        } catch (e: Exception) {
            Log.e("DoctorProfileFragment", "Error starting chat: ${e.message}", e)
            SnackbarUtils.showErrorSnackbar(binding.root, "Lỗi khi bắt đầu trò chuyện: ${e.message}")
        }
    }

    private fun startVoiceCallWithDoctor() {
        // Tạm thời thông báo tính năng sắp có
        SnackbarUtils.showInfoSnackbar(
            binding.root,
            "Tính năng gọi điện sẽ sớm được cập nhật"
        )
    }

    private fun startVideoCallWithDoctor() {
        // Tạm thời thông báo tính năng sắp có
        SnackbarUtils.showInfoSnackbar(
            binding.root,
            "Tính năng gọi video sẽ sớm được cập nhật"
        )
    }

    @SuppressLint("SetTextI18n")
    private fun displayDoctorInfo() {
        binding.apply {
            tvDoctorUid.text = args.doctor.id.toString()
            tvDoctorName.text = args.doctor.name
            tvDoctorProfileSpecialty.text = args.doctor.specialty
            tvDoctorRate.text = args.doctor.rating.toString()
            tvDoctorReviewCount.text = "(${args.doctor.reviews})"

            Glide.with(requireContext())
                .load(args.doctor.image)
                .placeholder(R.drawable.doctor)
                .error(R.drawable.doctor)
                .into(ivDoctorAvatar)
        }
        setupCopyUidButton()
    }

    private fun setupCopyUidButton() {
        binding.btnCopyDoctorUid.setOnClickListener {
            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Doctor UID", binding.tvDoctorUid.text)
            clipboard.setPrimaryClip(clip)

            binding.btnCopyDoctorUid.animate()
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(100)
                .withEndAction {
                    binding.btnCopyDoctorUid.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()

            SnackbarUtils.showSuccessSnackbar(binding.root, getString(R.string.uid_copied))
        }
    }

    private fun setupTimePeriodsChips() {
        binding.apply {
            val chips = listOf(chipMorning, chipAfternoon, chipEvening)

            // Set initial period
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)

            val selectedChip = when {
                hour in 0..12 -> chipMorning
                hour in 13..17 -> chipAfternoon
                else -> chipEvening
            }
            selectedChip.isChecked = true
            viewModel.timeSlots.value.let { slots ->
                updateTimeSlots(slots)
            }

            chips.forEach { chip ->
                chip.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        Log.d("TimeSlots", "Period chip changed: ${getSelectedPeriod()}")
                        chips.forEach { otherChip ->
                            if (otherChip != buttonView) otherChip.isChecked = false
                        }
                        viewModel.timeSlots.value.let { slots ->
                            updateTimeSlots(slots)
                        }
                    } else if (chips.none { it.isChecked }) {
                        buttonView.isChecked = true
                    }
                }
            }
        }
    }

    private fun getSelectedPeriod(): TimePeriod {
        return binding.run {
            when {
                chipMorning.isChecked -> TimePeriod.MORNING
                chipAfternoon.isChecked -> TimePeriod.AFTERNOON
                else -> TimePeriod.EVENING
            }
        }
    }

    private fun setupTimeSlotChips() {
        val chips = listOf(
            binding.chipSlot1,
            binding.chipSlot2,
            binding.chipSlot3,
            binding.chipSlot4
        )

        chips.forEach { chip ->
            chip.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    // Uncheck other chips
                    chips.forEach { otherChip ->
                        if (otherChip != buttonView) {
                            otherChip.isChecked = false
                        }
                    }

                    // Get TimeSlot from chip's tag and update ViewModel
                    (buttonView.tag as? TimeSlot)?.let { slot ->
                        viewModel.setSelectedTimeSlot(slot)
                    }
                } else if (chips.none { it.isChecked }) {
                    viewModel.resetSelectedTimeSlot()
                }
            }
        }
    }

    private fun setupBookingButton() {
        binding.btnBookAppointment.setOnClickListener {
            viewModel.bookAppointment(args.doctor.id)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}