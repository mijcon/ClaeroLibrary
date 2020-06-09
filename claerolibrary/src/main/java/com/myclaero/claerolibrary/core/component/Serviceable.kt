package com.myclaero.claerolibrary.core.component

import android.graphics.Bitmap
import com.myclaero.claerolibrary.ParseInventory
import com.myclaero.claerolibrary.R
import com.myclaero.claerolibrary.extensions.toList
import com.parse.ParseDecoder
import com.parse.ParseFile
import com.parse.ParseObject
import org.json.JSONObject

abstract class Serviceable: JSONObject() {

	companion object {
		const val SERVICE_PART = "servicePart"
		const val SERVICE_REC = "serviceRec"
		const val SERVICE_IMAGES = "serviceImages"
	}

	enum class ServiceRecommendation(stringId: Int) {
		ReplaceMileage(R.string.serviceable_rec_replace_mileage),
		ReplaceCondition(R.string.serviceable_rec_replace_condition),
		ReplaceAge(R.string.serviceable_rec_replace_age),
		NoService(R.string.serviceable_rec_okay),
		Repair(R.string.serviceable_rec_repair),
		Monitor(R.string.serviceable_rec_monitor)
	}

	var servicePart: ParseInventory?
		get() = optJSONObject(SERVICE_PART)?.let {
			ParseObject.fromJSON(it, ParseInventory.NAME, ParseDecoder.get())
		}
		set(value) {
			put(SERVICE_PART, value)
		}

	var serviceRecommendation: ServiceRecommendation
		get() = ServiceRecommendation.valueOf(optString(SERVICE_REC))
		set(value) {
			put(SERVICE_REC, value.name)
		}

	var pictures: List<ParseFile>
		get() = optJSONArray(SERVICE_IMAGES)?.toList() ?: listOf()
		set(value) {
			put(SERVICE_IMAGES, value)
		}

	fun addPicture(bitmap: Bitmap) {
		TODO("do the file upload and save to list")
	}

	fun removePicture(bitmap: Bitmap) {
		TODO("find ParseFile for this bitmap and delete from list")
	}

}