package codegen.controlflow.for_loops_lowering.test18

import kotlin.test.*

@Test fun runTest() {
    var sum = 0
    for (a in 0 .. 10) {
      for (b in 0 .. 10) {
        for (c in 0 .. 10) {
          for (d in 0 .. 10) {
            for (e in 0 .. 10) {
              for (f in 0 .. 10) {
                for (g in 0 .. 10) {
                  for (h in 0 .. 10) {
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
