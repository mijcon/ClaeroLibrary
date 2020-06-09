package com.myclaero.claerolibrary.api

import android.os.Bundle
import android.provider.Telephony
import androidx.appcompat.app.AppCompatActivity
import com.myclaero.claerolibrary.R

class TextVerificationActivity: AppCompatActivity() {

	companion object {
		const val TAG = "SmsVerifiedActivity"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.layout_claero_login)

		val message = Telephony.Sms.Intents.getMessagesFromIntent(intent)[0]
		val code = Regex("([0-9]){6}").find(message.messageBody)?.value

		TextVerification.checkVerificationCodeAsync(code!!)

		finish()
	}

}