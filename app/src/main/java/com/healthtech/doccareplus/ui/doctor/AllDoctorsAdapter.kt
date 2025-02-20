package com.healthtech.doccareplus.ui.doctor

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.ItemDoctorBinding
import com.healthtech.doccareplus.domain.model.Doctor

class AllDoctorsAdapter : RecyclerView.Adapter<AllDoctorsAdapter.AllDoctorsViewHolder>() {
    private var doctors = listOf<Doctor>()

    @SuppressLint("NotifyDataSetChanged")
    fun setDoctors(newDoctor: List<Doctor>) {
        doctors = newDoctor
        notifyDataSetChanged()
    }

    inner class AllDoctorsViewHolder(private val binding: ItemDoctorBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ) {
        @SuppressLint("SetTextI18n")
        fun bind(doctor: Doctor) {
            binding.apply {
                tvDoctorName.text = doctor.name
                tvDoctorSpecialty.text = doctor.specialty
                tvDoctorRate.text = doctor.rating.toString()
                tvDoctorReviewCount.text = "(${doctor.reviews})"
                tvDoctorFee.text = "$${doctor.fee}"

                Glide.with(root.context).load(doctor.image).error(R.drawable.doctor)
                    .into(ivDoctorAvatar)

                btnBookNow.setOnClickListener {
                    //todo
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