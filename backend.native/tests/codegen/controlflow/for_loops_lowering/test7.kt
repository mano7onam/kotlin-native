fun main(args: Array<String>) {
    val rng = 'a' .. 'z'
    for (c in rng) {
      if (c in 'f' downTo 'd' step 2) {
        println(c)
      }
    }
}
