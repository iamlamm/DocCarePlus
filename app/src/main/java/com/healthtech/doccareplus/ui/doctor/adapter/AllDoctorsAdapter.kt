package com.healthtech.doccareplus.ui.doctor.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.ItemDoctorBinding
import com.healthtech.doccareplus.domain.model.Doctor
import com.healthtech.doccareplus.ui.home.adapter.DoctorDiffCallback

class AllDoctorsAdapter : RecyclerView.Adapter<AllDoctorsAdapter.AllDoctorsViewHolder>() {
    private var doctors = listOf<Doctor>()
    private var onDoctorClickListener: ((Doctor) -> Unit)? = null
    private var onBookClickListener: ((Doctor) -> Unit)? = null

    fun setDoctors(newDoctors: List<Doctor>) {
        val diffCallback = DoctorDiffCallback(doctors, newDoctors)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        doctors = newDoctors
        diffResult.dispatchUpdatesTo(this)
    }

    // Thêm click listener cho card bác sĩ
    fun setOnDoctorClickListener(listener: (Doctor) -> Unit) {
        onDoctorClickListener = listener
    }

    // Thêm click listener cho nút Book Now
    fun setOnBookClickListener(listener: (Doctor) -> Unit) {
        onBookClickListener = listener
    }

    inner class AllDoctorsViewHolder(private val binding: ItemDoctorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(doctor: Doctor) {
            binding.apply {
                tvDoctorName.text = doctor.name
                tvDoctorSpecialty.text = doctor.specialty
                tvDoctorRate.text = doctor.rating.toString()
                tvDoctorReviewCount.text = "(${doctor.reviews})"
                tvDoctorFee.text = "$${doctor.fee}"

                // Tối ưu Glide với caching
                val requestOptions = RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.doctor)

                Glide.with(root.context)
                    .load(doctor.avatar)
                    .apply(requestOptions)
                    .into(ivDoctorAvatar)

                // Click vào card bác sĩ
                root.setOnClickListener {
                    onDoctorClickListener?.invoke(doctor)
                }

                // Click vào nút Book Now
                btnBookNow.setOnClickListener {
                    onBookClickListener?.invoke(doctor)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllDoctorsViewHolder {
        val binding = ItemDoctorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AllDoctorsViewHolder(binding)
    }

    override fun getItemCount(): Int = doctors.size

    override fun onBindViewHolder(holder: AllDoctorsViewHolder, position: Int) {
        holder.bind(doctors[position])
    }
}