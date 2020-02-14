package com.myclaero.claerolibrary

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.layout_vehicle.view.*


class VehicleView(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {

    companion object {
        const val TAG = "VehicleView"
    }

    private val view = inflate(context, R.layout.layout_vehicle, this)

    var onAddVinClickListener: AddVinClickListener? = null
        set(value) {
            field = value
            refreshData()
        }
    var onAddImageClickListener: AddImageClickListener? = null
        set(value) {
            field = value
            refreshData()
        }
    var vehicle: Vehicle? = null
        set(value) {
            field = value!!
            refreshData()
        }

    fun refreshData() {
        // Get Thumbnail
        setThumbnail()
        setVin()

        // Present data on CardView
        view.textVehName.text = vehicle?.nickname ?: vehicle?.titleYmm
        view.textVehYmmt.text = vehicle?.titleYmmt
        view.textVehPlate.text = vehicle?.getLicense(context)

        // Hide YMMT text if YMMT == Nickname
        view.textVehYmmt.visibility = if (view.textVehName.text == view.textVehYmmt.text) TextView.GONE else TextView.VISIBLE
    }

    private fun setVin() {
        view.textVehVin.run {
            when {
                vehicle?.vin != null -> {
                    text = vehicle?.vin
                    setTextColor(view.textVehPlate.textColors)
                    setOnClickListener { }
                }
                onAddVinClickListener == null -> {
                    setText(R.string.blank)
                    setOnClickListener { }
                }
                else -> {
                    setText(R.string.vehicle_vin_add)
                    setTextColor(ContextCompat.getColor(context, R.color.colorPrimaryDark))
                    setOnClickListener { onAddVinClickListener?.onClick(vehicle!!) }
                }
            }
        }
    }

    fun setThumbnail(bitmap: Bitmap?) {
        view.imageVehThumb.apply {
            when {
                bitmap != null -> {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageBitmap(bitmap)
                    setOnClickListener { }
                }
                onAddImageClickListener == null -> {
                    setImageResource(android.R.color.transparent)
                    setOnClickListener { }
                }
                else -> {
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    setImageResource(R.drawable.ic_add_a_photo_black_24dp)
                    setOnClickListener { onAddImageClickListener?.onClick(vehicle!!) }
                }
            }
        }
    }

    private fun setThumbnail() {
        view.imageVehThumb.setImageResource(android.R.color.transparent)
        vehicle?.getThumbnailInBackground { vehicle, bitmap, e ->
            // e?.upload(TAG)

	        // When this VehicleView is part of a RecyclerView, this asynchronous task may finish after the Vehicle
	        // has been changed. This ensures that we don't set the thumbnail for a previously-recycled VehicleView.
            if (vehicle == this.vehicle)
                setThumbnail(bitmap)
        }
    }

    interface AddVinClickListener {

        fun onClick(vehicle: Vehicle)

    }

    interface AddImageClickListener {

        fun onClick(vehicle: Vehicle)

    }

}