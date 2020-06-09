package com.myclaero.claerolibrary

import android.icu.util.Calendar
import com.myclaero.claerolibrary.Subscription.Status.*
import com.myclaero.claerolibrary.core.Ticket
import com.parse.FindCallback
import com.parse.ParseClassName
import com.parse.ParseException
import com.parse.ParseObject
import kotlinx.coroutines.*

@ParseClassName(Subscription.NAME)
class Subscription constructor() : ParseObject() {

	companion object {
		const val NAME = "Subscription"
		const val TAG = "Subscription"

		const val TIER = "tier"
		const val BODY = "body"
		const val STATUS = "status"
		const val PRICE = "priceAnnual"
		const val PRICE_MONTHLY = "priceMonth"
		const val PRICE_QUARTERLY = "priceQuarter"
		const val PRICE_SEMIANNUALLY = "priceSemiannual"
		const val INITIALIZATION = "contractStart"
		const val TERMINATION = "contractEnd"
		const val TICKETS = "tickets"
	}

	/**
	 * The options for a given Subscription's status.
	 *
	 * @property GOOD In good standing.
	 * @property DUE Payment is late, but contract is still active.
	 * @property CANCELLED Contract was cancelled while in good standing.
	 * @property TERMINATED Contract was cancelled due to bad standing.
	 */
	enum class Status(val i: Int) {
		GOOD(1),
		DUE(3),
		CANCELLED(5),
		TERMINATED(7)
	}

	val tier: String
		get() = getString(TIER)!!

	val body: String
		get() = getString(BODY)!!

	val status: Status?
		get() = values().firstOrNull { it.i == getInt(STATUS) }

	val startDate: Calendar
		get() = Calendar.getInstance().apply { time = getDate(INITIALIZATION) }

	val endDate: Calendar?
		get() = getDate(TERMINATION)?.let {
			Calendar.getInstance().apply { time = it }
		}

	/**
	 * The annual price of this contract in the smallest discrete units available in the given
	 * currency, such as 1 cent for USD or 1 yen or JPY.
	 */
	val price: Int
		get() = getInt(PRICE)

	val priceMonthly: Int
		get() = getInt(PRICE_MONTHLY)

	val priceQuarterly: Int
		get() = getInt(PRICE_QUARTERLY)

	val priceSemiannually: Int
		get() = getInt(PRICE_SEMIANNUALLY)

	/**
	 * The collection of Tickets associated with this Subscription. This property *synchronously*
	 * calls a Relation object from the server.
	 */
	val tickets: List<Ticket>
		get() = getRelation<Ticket>(TICKETS).query.orderByDescending(
			Ticket.START_DATE).find()

	fun getTicketsAsync(): Deferred<List<Ticket>> =
		GlobalScope.async(Dispatchers.IO) { tickets }

	fun getTicketsAsync(callback: FindCallback<Ticket>) {
		GlobalScope.launch {
			try {
				callback.done(getTicketsAsync().await(), null)
			} catch (e: ParseException) {
				callback.done(null, e)
			}
		}
	}




}