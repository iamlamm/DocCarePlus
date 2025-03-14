package com.healthtech.doccareplus.ui.appointment.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.ItemAppointmentBinding
import com.healthtech.doccareplus.domain.model.Appointment
import com.healthtech.doccareplus.utils.DateTimeUtils.formatServerDate
import com.healthtech.doccareplus.utils.DateTimeUtils.getTimeRangeForSlot
import java.text.SimpleDateFormat
import java.util.*

class AppointmentsAdapter :
    ListAdapter<Appointment, AppointmentsAdapter.AppointmentViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val binding =
            ItemAppointmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppointmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppointmentViewHolder(private val binding: ItemAppointmentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(appointment: Appointment) {
            // Format ngày tháng
            binding.tvAppointmentDate.text = formatServerDate(appointment.date)

            // Hiển thị thời gian dựa trên slotId
            binding.tvAppointmentTime.text = getTimeRangeForSlot(appointment.slotId)

            // Thiết lập thông tin bác sĩ
            binding.tvDoctorName.text = appointment.doctorName
            binding.tvAppointmentId.text = "Mã cuộc hẹn: #${appointment.id.take(8)}"

            // Tải avatar bác sĩ nếu có
            if (appointment.doctorAvatar.isNotEmpty()) {
                Glide.with(binding.root)
                    .load(appointment.doctorAvatar)
                    .placeholder(R.drawable.doctor)
                    .into(binding.ivDoctorAvatar)
            } else {
                // Hiển thị placeholder nếu không có avatar
                binding.ivDoctorAvatar.setImageResource(R.drawable.doctor)
            }

            // Hiển thị trạng thái cuộc hẹn
            when (calculateAppointmentStatus(appointment)) {
                "upcoming" -> {
                    binding.tvStatus.text = "Sắp tới"
                    binding.tvStatus.background = ContextCompat.getDrawable(
                        binding.root.context,
                        R.drawable.bg_status_upcoming
                    )
                }

                "completed" -> {
                    binding.tvStatus.text = "Hoàn thành"
                    binding.tvStatus.background = ContextCompat.getDrawable(
                        binding.root.context,
                        R.drawable.bg_status_completed
                    )
                }

                "cancelled" -> {
                    binding.tvStatus.text = "Đã hủy"
                    binding.tvStatus.background = ContextCompat.getDrawable(
                        binding.root.context,
                        R.drawable.bg_status_cancelled
                    )
                }

                "expired" -> {
                    binding.tvStatus.text = "Hết hạn"
                    binding.tvStatus.background = ContextCompat.getDrawable(
                        binding.root.context,
                        R.drawable.bg_status_expired
                    )
                }

                else -> {
                    binding.tvStatus.text = "Không xác định"
                    binding.tvStatus.background = ContextCompat.getDrawable(
                        binding.root.context,
                        R.drawable.bg_status_upcoming
                    )
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Appointment>() {
            override fun areItemsTheSame(oldItem: Appointment, newItem: Appointment): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Appointment, newItem: Appointment): Boolean {
                return oldItem == newItem
            }
        }

        // Phương thức tính toán trạng thái thực tế của cuộc hẹn
        private fun calculateAppointmentStatus(appointment: Appointment): String {
            // Nếu đã bị hủy, giữ nguyên trạng thái
            if (appointment.status == "cancelled") {
                return "cancelled"
            }

            val now = Calendar.getInstance()

            // Lấy thời gian kết thúc của cuộc hẹn
            val appointmentDate =
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(appointment.date)
            if (appointmentDate == null) {
                return appointment.status
            }

            val appointmentCalendar = Calendar.getInstance()
            appointmentCalendar.time = appointmentDate

            // Thiết lập giờ, phút từ thời gian kết thúc cuộc hẹn
            val (hour, minute) = getAppointmentEndTime(appointment.slotId)
            appointmentCalendar.set(Calendar.HOUR_OF_DAY, hour)
            appointmentCalendar.set(Calendar.MINUTE, minute)

            // So sánh với thời gian hiện tại
            return if (now.after(appointmentCalendar)) {
                if (appointment.status == "completed") "completed" else "expired"
            } else {
                "upcoming"
            }
        }

        // Lấy thời gian kết thúc của slot dựa trên database
        private fun getAppointmentEndTime(slotId: Int): Pair<Int, Int> {
            // Mapping từ slotId sang giờ kết thúc (giờ, phút) theo thực tế từ database
            return when (slotId) {
                // Morning slots
                0 -> Pair(9, 0)   // 08:00 - 09:00
                1 -> Pair(10, 0)  // 09:00 - 10:00
                2 -> Pair(11, 0)  // 10:00 - 11:00
                3 -> Pair(12, 0)  // 11:00 - 12:00

                // Afternoon slots
                4 -> Pair(14, 30) // 13:30 - 14:30
                5 -> Pair(15, 30) // 14:30 - 15:30
                6 -> Pair(16, 30) // 15:30 - 16:30
                7 -> Pair(17, 30) // 16:30 - 17:30

                // Evening slots
                8 -> Pair(19, 30) // 18:30 - 19:30
                9 -> Pair(20, 30) // 19:30 - 20:30
                10 -> Pair(21, 30) // 20:30 - 21:30
                11 -> Pair(22, 30) // 21:30 - 22:30

                else -> Pair(23, 59) // Mặc định nếu không tìm thấy
            }
        }
    }
}