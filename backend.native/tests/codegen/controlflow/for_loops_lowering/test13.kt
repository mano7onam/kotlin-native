fun main(args: Array<String>) {
    val arr = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    var sum = 0L
    for (i in 0L .. Long.MAX_VALUE step 1000000000000L) {
      for (j in arr.indices) {
        sum += i + j
      }  
    }
    println(sum)
}
