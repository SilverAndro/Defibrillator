package mc.defibrillator.util.classes

class DualHashMap<KEY, TYPE_A, TYPE_B> {
    private val backingA: HashMap<KEY, TYPE_A> = hashMapOf()
    private val backingB: HashMap<KEY, TYPE_B> = hashMapOf()

    fun set(key: KEY, value1: TYPE_A, value2: TYPE_B) {
        backingA[key] = value1
        backingB[key] = value2
    }

    fun remove(key: KEY) {
        backingA.remove(key)
        backingB.remove(key)
    }

    fun contains(key: KEY): Boolean {
        // They should always be updated in parallel, so no need to check twice
        return backingA.containsKey(key)
    }

    fun getA(key: KEY): TYPE_A = backingA[key]!!
    fun getB(key: KEY): TYPE_B = backingB[key]!!
    operator fun get(key: KEY): Pair<TYPE_A, TYPE_B> = Pair(getA(key), getB(key))

    fun forEach(receiver: (KEY, TYPE_A, TYPE_B) -> Unit) {
        for (key in backingA.keys) {
            val entry = get(key)
            receiver(key, entry.first, entry.second)
        }
    }
}
