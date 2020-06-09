package com.myclaero.claerolibrary.core.component

import com.myclaero.claerolibrary.R

class Gearbox : FluidSystem() {

	companion object {
		const val DRIVE = "drive"
	}

	enum class Drive(stringId: Int) {
		FDiff(R.string.gearbox_fdiff),
		RDiff(R.string.gearbox_rdiff),
		TCase(R.string.gearbox_tcase),
		FDrive(R.string.gearbox_fdrive)
	}

	var drive: Drive
		get() = Drive.valueOf(optString(DRIVE))
		set(value) { put(DRIVE, value.name) }

}