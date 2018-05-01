fun main(args: Array<String>) {
    var sum = 0
    for (a in 0 .. 10) {
      for (b in 0 .. 10) {
        for (c in 0 .. 10) {
          for (d in 0 .. 10) {
            for (e in 0 .. 10) {
              for (f in 0 .. 10) {
                for (g in 0 until 10) {
                  for (h in 10 downTo 0) {
                    sum += a + b + c + d + e + f + g + h
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
