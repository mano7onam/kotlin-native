fun foo() : Int {
  println("foo")
  return 2
}

fun bar() : Int {
  return 1
}

fun main(args: Array<String>) {
    val rng = (Int.MAX_VALUE - 5) .. Int.MAX_VALUE
    var arr = listOf(4, 5, 6)
    for (i in rng) {
    	for (j in arr.indices) {
          for (k in 4 downTo 0 step foo() step bar()) {
              println("$i + $j = ${i + j}")
          }
      }
    }
}