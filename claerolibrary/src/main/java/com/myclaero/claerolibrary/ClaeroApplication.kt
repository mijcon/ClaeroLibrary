package com.myclaero.claerolibrary

import android.app.Application
import androidx.annotation.CallSuper
import com.myclaero.claerolibrary.core.*
import com.parse.Parse
import com.parse.ParseObject

abstract class ClaeroApplication : Application() {

	/**
	 *
	 */
	@CallSuper
	override fun onCreate() {
		super.onCreate()

		Parse.enableLocalDatastore(this)

		ParseObject.registerSubclass(Vehicle::class.java)
		ParseObject.registerSubclass(Ticket::class.java)
		ParseObject.registerSubclass(Locus::class.java)
		ParseObject.registerSubclass(Shift::class.java)
		ParseObject.registerSubclass(Service::class.java)
		ParseObject.registerSubclass(ParseHub::class.java)

	}

	abstract fun initializeParse()

}