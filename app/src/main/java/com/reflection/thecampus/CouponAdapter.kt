package com.reflection.thecampus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.reflection.thecampus.data.model.Offer
import java.text.SimpleDateFormat
import java.util.Locale

class CouponAdapter(
    private val onApplyClick: (Offer) -> Unit
) : RecyclerView.Adapter<CouponAdapter.CouponViewHolder>() {

    private var coupons: List<Offer> = emptyList()

    fun submitList(list: List<Offer>) {
        coupons = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CouponViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_coupon, parent, false)
        return CouponViewHolder(view)
    }

    override fun onBindViewHolder(holder: CouponViewHolder, position: Int) {
        holder.bind(coupons[position])
    }

    override fun getItemCount() = coupons.size

    inner class CouponViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCouponCode: TextView = itemView.findViewById(R.id.tvCouponCode)
        private val tvCouponTitle: TextView = itemView.findViewById(R.id.tvCouponTitle)
        private val tvCouponDesc: TextView = itemView.findViewById(R.id.tvCouponDesc)
        private val btnApply: MaterialButton = itemView.findViewById(R.id.btnApply)

        fun bind(offer: Offer) {
            tvCouponCode.text = offer.couponCode
            tvCouponTitle.text = offer.title
            
            val desc = if (offer.discountType == "percentage") {
                "Get ${offer.discountValue.toInt()}% OFF up to ₹${offer.minPrice.toInt()}" // Just example text
            } else {
                "Flat ₹${offer.discountValue.toInt()} OFF"
            }
            tvCouponDesc.text = desc

            btnApply.setOnClickListener {
                onApplyClick(offer)
            }
        }
    }
}
