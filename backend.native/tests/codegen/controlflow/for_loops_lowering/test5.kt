fun main(args: Array<String>) {
    val rng = 'a' .. 'z'
    for (c in rng) {
      if (c in 'e' until 'k') {
        println(c)
      }
    }
}
