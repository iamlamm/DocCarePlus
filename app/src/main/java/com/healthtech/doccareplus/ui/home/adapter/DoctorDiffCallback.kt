package com.healthtech.doccareplus.ui.home.adapter

import androidx.recyclerview.widget.DiffUtil
import com.healthtech.doccareplus.domain.model.Doctor

class DoctorDiffCallback(
    private val oldList: List<Doctor>,
    private val newList: List<Doctor>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem.name == newItem.name &&
                oldItem.specialty == newItem.specialty &&
                oldItem.avatar == newItem.avatar &&
                oldItem.rating == newItem.rating &&
                oldItem.reviews == newItem.reviews &&
                oldItem.fee == newItem.fee
    }
}