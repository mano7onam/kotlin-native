fun main(args: Array<String>) {
    val arr = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    var list = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    var sum = 0
    for (a in 0 .. 100) {
      for (b in arr.indices) {
        for (c in list.indices) {
          for (d in 0 until 10) {
            for (e in 10 downTo 0) {
              for (f in a .. c) {
                for (g in d .. e) {
                  for (h in g .. a) {
                    if (c in 0 .. a) {
                      for (i in 0 .. 1) {
                        sum += 1
                      }
                    }
                    if (f in 0 until a) {
                      sum += 1
                    }
                    for (i in 0 .. 50) {
                      sum += arr[b] + list[c] + d + e + f
                    }
                    if (d in a downTo 0) {
                      for (i in 4 downTo 1) {
                        sum += 1
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }  
    }
    println(sum)
}
