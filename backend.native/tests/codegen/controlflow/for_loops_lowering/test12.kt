fun main(args: Array<String>) {
    val arr = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    var sum = 0
    for (i in 0 .. Int.MAX_VALUE step 500) {
      for (j in arr.indices) {
        sum += i + j
      }  
    }
    println(sum)
}
