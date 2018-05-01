fun foo() : Int {
    return 2
}

fun main(args: Array<String>) {
    val rng = 5 .. 10
    var arr = arrayOf(1, 2, 3)
    for (i in rng) {
    	for (j in arr.indices) {
          for (k in 4 downTo 0 step foo()) {
              println("$i + $j = ${i + j}")
          }
      }
    }
}