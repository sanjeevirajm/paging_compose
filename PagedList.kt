data class PagedList<E>(
    val actualList: @RawValue List<E>,
    private val hasNext: Boolean,
) : List<E> {
    companion object {
        const val LoaderKey = Int.MAX_VALUE
    }
    override val size: Int
        get() = if (hasNext) actualList.size + 1 else actualList.size

    override fun contains(element: E): Boolean {
        return actualList.contains(element)
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        return actualList.containsAll(elements)
    }

    fun isLoaderItem(position: Int): Boolean {
        return hasNext && position == size - 1
    }

    fun indexOfLoader(): Int {
        return if (hasNext) size - 1 else -1
    }

    fun hasLoader(): Boolean {
        return hasNext
    }

    override fun get(index: Int): E {
        if (hasNext && index == size - 1) {
            throw IllegalStateException(
                "get can't be performed. Loader item is present at the $index th position," +
                        " add isLoaderItem check before calling this method"
            )
        }
        return actualList[index]
    }

    override fun indexOf(element: E): Int {
        return actualList.indexOf(element)
    }

    override fun isEmpty(): Boolean {
        return size == 0
    }

    override fun iterator(): Iterator<E> {
        return actualList.iterator()
    }

    override fun lastIndexOf(element: E): Int {
        return actualList.lastIndexOf(element)
    }

    override fun listIterator(): ListIterator<E> {
        return actualList.listIterator()
    }

    override fun listIterator(index: Int): ListIterator<E> {
        return actualList.listIterator()
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<E> {
        throw IllegalStateException(
            "sublist is not implemented due the the nature of" +
                    " pagination complexity"
        )
    }
}
