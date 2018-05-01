fun main(args: Array<String>) {
    val rng = 'a' .. 'z'
    for (c in rng) {
      if (c in 'a' until 'e' step 2) {
        println(c)
      }
    }
}
