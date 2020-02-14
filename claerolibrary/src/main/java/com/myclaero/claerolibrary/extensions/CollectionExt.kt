package com.myclaero.claerolibrary.extensions

/**
 * Takes two [Collection]s, including Sets, Lists and Arrays, and returns the items that exist in
 * either one of the arrays exclusively, or are shared between them.
 *
 * @receiver The first array to be compared, labeled as "a" in the callback.
 * @param that The second array to be compared, labeled as "b" in the callback.
 * @param callback The function to be invoked upon completion of the comparison. First Collection
 * contains the objects exclusive to Collection A, the second Collection contains the objects
 * shared between both Collections, and the third Collection contains the objects exclusive to
 * Collection B.
 */
inline fun <E: Any> Collection<E>.versus(
	that: Collection<E>,
	callback: (a: Collection<E>, both: Collection<E>, b: Collection<E>) -> Unit
) {
	// Finds all <E> in Collection A that are not in Collection B
	val a = this.filterNot { that.contains(it) }
	// Finds all <E> in Collection B that are not in Collection A
	val b = that.filterNot { this.contains(it) }
	// Finds all <E> in Collection A that are in Collection B
	val both = this.filter { that.contains(it) }

	callback(a, both, b)
}