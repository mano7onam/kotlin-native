fun main(args: Array<String>) {
    val rng = 'a' .. 'z'
    for (c in rng) {
      if (c in 'k' downTo 'e') {
        println(c)
      }
    }
}
