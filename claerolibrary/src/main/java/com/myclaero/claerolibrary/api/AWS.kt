package com.myclaero.claerolibrary.api

import com.myclaero.claerolibrary.BuildConfig
import com.parse.ParseConfig

private object AWS {

	private enum class Endpoints(val url: String) {
		VERIFICATION_SMS(BuildConfig.CLAERO_API_URL + "/verify/phone")
	}

	private val claeroApiHeader = mapOf(
		"X-Api-Key" to ParseConfig.getCurrentConfig().getString("claero_api_key", BuildConfig.CLAERO_API_KEY)
	)



}