fun main(args: Array<String>) {
    var sum = 0
    for (a in 0 .. 10 step 1) {
      for (b in 0 .. 10 step 2) {
        for (c in 0 .. 10 step 3) {
          for (d in 0 .. 10 step 4 step 3) {
            for (e in 0 .. 10 step 4 step 4 step 2) {
              for (f in 0 .. 10 step 1 step 2) {
                for (g in 0 until 10 step 10 step 2) {
                  for (h in 10 downTo 0 step 6 step 2) {
                    for (i in 0 .. 300) {
                      sum += a + b + c + d + e + f + g + h + i
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
