package com.healthtech.doccareplus.ui.doctor.profile

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.FragmentDoctorProfileBinding
import com.healthtech.doccareplus.domain.model.TimePeriod
import com.healthtech.doccareplus.domain.model.TimeSlot
import com.healthtech.doccareplus.ui.doctor.adapter.CalendarAdapter
import com.healthtech.doccareplus.utils.Constants
import com.healthtech.doccareplus.utils.SnackbarUtils
import com.healthtech.doccareplus.utils.showErrorDialog
import com.healthtech.doccareplus.utils.showInfoDialog
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.zegocloud.zimkit.common.ZIMKitRouter
import com.zegocloud.zimkit.common.enums.ZIMKitConversationType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar

@AndroidEntryPoint
class DoctorProfileFragment : Fragment() {
    private var _binding: FragmentDoctorProfileBinding? = null
    private val binding get() = _binding!!
    private val args: DoctorProfileFragmentArgs by navArgs()
    private val viewModel: DoctorProfileViewModel by viewModels()
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var paymentSheet: PaymentSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PaymentConfiguration.init(requireContext(), Constants.STRIPE_PUBLISHABLE_KEY)
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

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

        val doctor = args.doctor
        viewModel.setDoctorId(doctor.id)

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
                val action = DoctorProfileFragmentDirections.actionDoctorProfileToChat(
                    conversationId = state.doctorId,
                    title = state.doctorName
                )
                findNavController().navigate(action)
            }

            is DoctorProfileState.PaymentLoading -> {
                showLoading()
                binding.btnBookAppointment.isEnabled = false
            }

            is DoctorProfileState.PaymentReady -> {
                hideLoading()
                presentPaymentSheet(state.paymentIntentClientSecret, state.customerConfig)
            }

            is DoctorProfileState.PaymentComplete -> {
                hideLoading()
                binding.btnBookAppointment.isEnabled = true
                showBookingSuccess(state.appointmentId)

                findNavController().navigate(
                    DoctorProfileFragmentDirections.actionDoctorProfileToSuccess(state.appointmentId)
                )
            }

            is DoctorProfileState.PaymentFailed -> {
                hideLoading()
                binding.btnBookAppointment.isEnabled = true
                showErrorDialog(message = state.error)
            }

            is DoctorProfileState.PaymentCancelled -> {
                hideLoading()
                binding.btnBookAppointment.isEnabled = true
                SnackbarUtils.showErrorSnackbar(
                    binding.root,
                    getString(R.string.payment_cancelled)
                )
            }
        }
    }

    private fun showLoading() {
        binding.progressBarDoctorProfile.setLoading(true)
    }

    private fun hideLoading() {
        binding.progressBarDoctorProfile.setLoading(false)
    }


    private fun showBookingSuccess(appointmentId: String) {
        SnackbarUtils.showSuccessSnackbar(
            binding.root,
            "Đặt lịch thành công! Mã cuộc hẹn: $appointmentId"
        )
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

            Timber.tag("TimeSlots All: ").d(timeSlots.toString())

            val filteredSlots = timeSlots.filter { it.period == currentPeriod }

            Timber.d("Current period: %s", currentPeriod)
            Timber.d("All slots: " + timeSlots.size + ", Filtered: " + filteredSlots.size)
            filteredSlots.forEach { slot ->
                Timber.d("Filtered slot: " + slot.startTime + "-" + slot.endTime)
            }

            val chips = listOf(chipSlot1, chipSlot2, chipSlot3, chipSlot4)
            chips.forEach { chip ->
                chip.isChecked = false
                chip.visibility = View.GONE
            }

            chips.forEachIndexed { index, chip ->
                if (index < filteredSlots.size) {
                    val slot = filteredSlots[index]
                    chip.apply {
                        text = formatTimeSlot(slot)
                        isEnabled = true
                        visibility = View.VISIBLE
                        tag = slot
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
            val doctorId = args.doctor.id

            ZIMKitRouter.toMessageActivity(
                requireContext(),
                doctorId,
                ZIMKitConversationType.ZIMKitConversationTypePeer
            )
        } catch (e: Exception) {
            Timber.e(e, "Error starting chat: " + e.message)
            SnackbarUtils.showErrorSnackbar(
                binding.root,
                "Lỗi khi bắt đầu trò chuyện: ${e.message}"
            )
        }
    }

    private fun startVoiceCallWithDoctor() {
        SnackbarUtils.showInfoSnackbar(
            binding.root,
            "Tính năng gọi điện sẽ sớm được cập nhật"
        )
    }

    private fun startVideoCallWithDoctor() {
        SnackbarUtils.showInfoSnackbar(
            binding.root,
            "Tính năng gọi video sẽ sớm được cập nhật"
        )
    }

    @SuppressLint("SetTextI18n")
    private fun displayDoctorInfo() {
        binding.apply {
            tvDoctorUid.text = args.doctor.id
            tvDoctorName.text = args.doctor.name
            tvDoctorProfileSpecialty.text = args.doctor.specialty
            tvDoctorRate.text = args.doctor.rating.toString()
            tvDoctorReviewCount.text = "(${args.doctor.reviews})"

            Glide.with(requireContext())
                .load(args.doctor.avatar)
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

            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)

            val selectedChip = when (hour) {
                in 0..12 -> chipMorning
                in 13..17 -> chipAfternoon
                else -> chipEvening
            }
            selectedChip.isChecked = true
            viewModel.timeSlots.value.let { slots ->
                updateTimeSlots(slots)
            }

            chips.forEach { chip ->
                chip.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
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
                    chips.forEach { otherChip ->
                        if (otherChip != buttonView) {
                            otherChip.isChecked = false
                        }
                    }

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
            val doctor = args.doctor
            viewModel.setupBooking(doctor.id, doctor.fee)
        }
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        viewModel.handlePaymentResult(paymentSheetResult)
    }

    private fun presentPaymentSheet(
        clientSecret: String,
        customerConfig: PaymentSheet.CustomerConfiguration
    ) {
        paymentSheet.presentWithPaymentIntent(
            clientSecret,
            PaymentSheet.Configuration(
                merchantDisplayName = "DocCarePlus",
                customer = customerConfig,
                allowsDelayedPaymentMethods = true
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}