package com.healthtech.doccareplus.ui.doctor.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.ItemCalendarBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalendarAdapter : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {
    private var dates = listOf<Date>()
    private var selectedPosition = -1
    private var onDateClickListener: ((Date) -> Unit)? = null

    inner class CalendarViewHolder(private val binding: ItemCalendarBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(date: Date, position: Int) {
            binding.apply {
                tvDate.text = SimpleDateFormat("dd", Locale.getDefault()).format(date)
                tvDay.text = SimpleDateFormat("EEE", Locale.getDefault()).format(date)

                cardView.isSelected = position == selectedPosition


                cardView.setOnClickListener {
                    val previousSelected = selectedPosition
                    selectedPosition = position
                    notifyItemChanged(previousSelected)
                    notifyItemChanged(selectedPosition)
                    onDateClickListener?.invoke(date)
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setDates(newDates: List<Date>) {
        dates = newDates
        notifyDataSetChanged()
    }

    fun setOnDateClickListener(listener: (Date) -> Unit) {
        onDateClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val binding = ItemCalendarBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CalendarViewHolder(binding)
    }

    override fun getItemCount() = dates.size

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        holder.bind(dates[position], position)
    }
}