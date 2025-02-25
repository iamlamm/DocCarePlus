package com.healthtech.doccareplus.ui.widgets.slider

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.healthtech.doccareplus.databinding.ItemBannerSlideBinding

/**
 * Adapter cho BannerSlider, hỗ trợ infinite scrolling và hiệu quả
 * khi cập nhật các images.
 */
class SliderAdapter(private var images: List<Int>) :
    RecyclerView.Adapter<SliderAdapter.SliderViewHolder>() {

    companion object {
        private const val INFINITE_SCROLL_MULTIPLIER = 1000 // Đồng bộ với BannerSlider
    }

    inner class SliderViewHolder(private val binding: ItemBannerSlideBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(imageRes: Int) {
            binding.imageView.setImageResource(imageRes)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        val binding = ItemBannerSlideBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SliderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        if (images.isNotEmpty()) {
            holder.bind(images[position % images.size])
        }
    }

    override fun getItemCount(): Int =
        if (images.isEmpty()) 0 else images.size * INFINITE_SCROLL_MULTIPLIER

    /**
     * Cập nhật danh sách images với cơ chế so sánh đơn giản.
     * Không cần DiffUtil đầy đủ vì danh sách ảnh thường nhỏ và ít thay đổi.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updateImages(newImages: List<Int>) {
        if (newImages != images) {
            images = newImages
            notifyDataSetChanged()
        }
    }
}

/**
 * 15h 2502
 */
//class SliderAdapter(private val images: List<Int>) :
//    RecyclerView.Adapter<SliderAdapter.SliderViewHolder>() {
//
//    private val LOOP_COUNT = 1000
//
//    inner class SliderViewHolder(private val binding: ItemBannerSlideBinding) :
//        RecyclerView.ViewHolder(binding.root) {
//        fun bind(imageRes: Int) {
//            binding.imageView.setImageResource(imageRes)
//        }
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
//        val binding = ItemBannerSlideBinding.inflate(
//            LayoutInflater.from(parent.context),
//            parent,
//            false
//        )
//        return SliderViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
//        holder.bind(images[position % images.size])
//    }
//
//    override fun getItemCount(): Int = images.size * LOOP_COUNT
//}