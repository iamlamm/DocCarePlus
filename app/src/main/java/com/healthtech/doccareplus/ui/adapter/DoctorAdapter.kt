package com.healthtech.doccareplus.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.domain.model.Doctor
import com.healthtech.doccareplus.databinding.ItemDoctorBinding

class DoctorAdapter : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {
    private var doctors = listOf<Doctor>()

    @SuppressLint("NotifyDataSetChanged")
    fun setDoctors(newDoctors: List<Doctor>) {
        doctors = newDoctors
        notifyDataSetChanged()
    }

    class DoctorViewHolder(private val binding: ItemDoctorBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(doctor: Doctor) {
            binding.apply {
                tvDoctorName.text = doctor.name
                tvDoctorSpecialty.text = doctor.specialty
                tvDoctorRate.text = doctor.rating.toString()
                tvDoctorReviewCount.text = "(${doctor.reviews})"
                tvDoctorFee.text = "$${doctor.fee}"

                Glide.with(root.context)
                    .load(doctor.image)
                    .error(R.drawable.doctor)
                    .into(ivDoctorAvatar)

                btnBookNow.setOnClickListener {
                    //todo
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val binding = ItemDoctorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DoctorViewHolder(binding)
    }

    override fun getItemCount(): Int = doctors.size

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        holder.bind(doctors[position])
    }
}