package com.myclaero.claerolibrary

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class PhoneTextWatcher(private val editText: EditText): TextWatcher {

	var onPhoneCheckedListener: ((isValid: Boolean) -> Unit)? = null
	private var cursorPosition: Int? = null

	override fun afterTextChanged(p0: Editable?) {
		// Get just the numbers, and add a 1 to the front if it's not there.
		if (p0.isNullOrBlank()) return

		val no: String = p0
			.toString()
			.filter { it.isDigit() }
			.let { if (it[0] != '1') "1$it" else it }

		// Format it.
		val newString = when (no.length) {
			1 -> "+$no "
			in 2..4 -> "+${no[0]} (${no.substring(1)}"
			in 5..7 -> "+${no[0]} (${no.substring(1, 4)}) ${no.substring(4)}"
			else -> "+${no[0]} (${no.substring(1, 4)}) ${no.substring(4, 7)}-${no.substring(7)}"
		}

		// If the original and the formatted don't match, replace with formatted.
		// This is important to keep it from looping.
		if (p0.toString() != newString) {
			// Determine which number the cursor is behind.
			cursorPosition = p0
				.substring(0, editText.selectionStart)
				.filter { it.isDigit() }
				.let { if (it.isNotEmpty() && it[0] != '1') "1$it" else it }
				.length

			// Determine where on the new string the cursor should go.
			cursorPosition = when (cursorPosition) {
				0 -> 1
				1 -> 3
				in 2..4 -> cursorPosition?.plus(3)
				in 5..7 -> cursorPosition?.plus(5)
				else -> cursorPosition?.plus(6)
			}
			// Replace String.
			p0.replace(0, p0.length, newString)
		}

		// Re-place cursor to where it should be (assuming it needs re-placing)
		cursorPosition = cursorPosition?.let {
			editText.setSelection(it)
			null
		}

		onPhoneCheckedListener?.invoke(no.length == 11)
	}

	override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit

	override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit

}