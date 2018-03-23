import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class TestGenerator (pFileName : String) {
    private val fileName = pFileName
    val fileWriter = PrintWriter(fileName)
    private val maxDepth = 3
    private val maxStep = 3

    private val defaultStart = 100
    private val defaultInterval = 100

    private val rand = Random()

    private fun printWithTabs(depth: Int, line : String) {
        for (i in 0 until depth) {
//            print("  ")
            fileWriter.print("  ")
        }
//        println(line)
        fileWriter.println(line)
    }

    enum class RangeType {
        RANGETO, DOWNTO, UNTIL
    }

    enum class RangeValuesType {
        INT, LONG, CHAR
    }

    class RangeInfo <T> (
            pRangeValuesType : RangeValuesType,
            pRangeType : RangeType,
            pA : T,
            pB : T)
    {
        val rangeValuesType = pRangeValuesType
        val rangeType = pRangeType
        val a = pA
        val b = pB

        private fun rangeTypeToString(rangeType : RangeType) : String {
            return when (rangeType) {
                RangeType.RANGETO -> ".."
                RangeType.DOWNTO -> "downTo"
                else -> "until"
            }
        }

        private fun paramToString(param : T) : String {
            when (param) {
                is Int -> return param.toString()
                is Long -> return param.toString() + "L"
                is Char -> return "'" + param.toString() + "'"
            }
            return ""
        }

        override fun toString() : String {
            return "${paramToString(a)} ${rangeTypeToString(rangeType)} ${paramToString(b)}"
        }
    }

    private fun <T : Comparable<T>> getNonEmptyRange (a : T, b : T) : RangeInfo<T> {
        // TODO range with step
        val rangeValueType = when (a) {
            is Int -> RangeValuesType.INT
            is Long -> RangeValuesType.LONG
            is Char -> RangeValuesType.CHAR
            else -> throw IllegalArgumentException("bad type")
        }
        val r = rand.nextInt(2)
        val rangeType = when {
            a > b -> RangeType.DOWNTO
            a == b -> RangeType.RANGETO
            r == 0 -> RangeType.UNTIL
            else -> RangeType.RANGETO
        }
        return RangeInfo(rangeValueType, rangeType, a, b)
    }

    private fun getRange(type : RangeValuesType) : RangeInfo<*> {
        val sgn = if (rand.nextInt(2) == 0) 1 else -1
        val start = rand.nextInt(defaultStart)
        val interval = rand.nextInt(defaultInterval)
        when (type) {
            RangeValuesType.INT -> {
                val a = start
                val b = start + sgn * interval
                return getNonEmptyRange(a, b)
            }
            RangeValuesType.CHAR -> {
                val a = 'a'
                val b = 'z'
                return getNonEmptyRange(a, b)
            }
            else -> {
                val a = start.toLong()
                val b = (start + sgn * interval).toLong()
                return getNonEmptyRange(a, b)
            }
        }
    }

    private fun getRange() : RangeInfo<*> {
        val r = rand.nextInt(3)
        return when (r) {
            0 -> getRange(RangeValuesType.INT)
            1 -> getRange(RangeValuesType.CHAR)
            else -> getRange(RangeValuesType.LONG)
        }
    }

    private fun genForFromConsts(step: Int, depth : Int) {
        val forCounterName = "i_${step}_$depth"
        val rangeInfo = getRange()
        printWithTabs(depth, "for ($forCounterName in $rangeInfo) {")
        printWithTabs(depth + 1, "println($forCounterName)")
    }

    private fun genForFromNewList(step : Int, depth : Int) {
        val forCounterName = "i_${step}_$depth"
        val tmpName = "tmp_${step}_$depth"
        val listName = "list_${step}_$depth"
        val r = rand.nextInt(2)
        when (r) {
            0 -> {
                printWithTabs(depth, "val $tmpName = listOf(1, 2, 3, 4, 5)")
                printWithTabs(depth, "val $listName = $tmpName.indices")
            }
            1 -> {
                val rangeInfo = getRange()
                printWithTabs(depth, "val $listName = $rangeInfo")
            }
        }
        printWithTabs(depth, "for ($forCounterName in $listName) {")
        printWithTabs(depth + 1, "println($forCounterName)")
    }

    private fun genDefinitionIfVar(ifVarName : String, rangeInfo : RangeInfo<*>, depth : Int) {
        if (rangeInfo.rangeType == RangeType.DOWNTO) {
            when (rangeInfo.a) {
                is Int -> printWithTabs(depth, "val $ifVarName = ${rangeInfo.a - 4}")
                is Long -> printWithTabs(depth, "val $ifVarName = ${rangeInfo.a - 4L}L")
                is Char -> printWithTabs(depth, "val $ifVarName = '${rangeInfo.a - 4}'")
            }
        } else {
            when (rangeInfo.a) {
                is Int -> printWithTabs(depth, "val $ifVarName = ${rangeInfo.a + 4}")
                is Long -> printWithTabs(depth, "val $ifVarName = ${rangeInfo.a + 4L}L")
                is Char -> printWithTabs(depth, "val $ifVarName = '${rangeInfo.a + 4}'")
            }
        }
    }

    private fun genIfFromNewList(step : Int, depth : Int) {
        val ifVarName = "v_${step}_$depth"
        val tmpName = "tmp_${step}_$depth"
        val listName = "list_${step}_$depth"
        val r = rand.nextInt(2)
        when (r) {
            0 -> {
                printWithTabs(depth, "val $tmpName = listOf(1, 2, 3, 4, 5)")
                printWithTabs(depth, "val $listName = $tmpName.indices")
                val rr = rand.nextInt(8) - 2
                printWithTabs(depth, "val $ifVarName = $rr")
            }
            1 -> {
                val rangeInfo = getRange()
                genDefinitionIfVar(ifVarName, rangeInfo, depth)
                printWithTabs(depth, "val $listName = $rangeInfo")
            }
        }
        printWithTabs(depth, "if ($ifVarName in $listName) {")
        printWithTabs(depth + 1, "println($ifVarName)")
    }

    private fun genIfFromConsts(step : Int, depth : Int) {
        val rangeInfo = getRange()
        val ifVarName = "v_${step}_$depth"

        genDefinitionIfVar(ifVarName, rangeInfo, depth)

        printWithTabs(depth, "if ($ifVarName in $rangeInfo) {")
        printWithTabs(depth + 1, "println($ifVarName)")
    }

    private fun generateBody(depth: Int) {
        val steps = rand.nextInt(maxStep) + 1
        for (i in 1 .. steps) {
            val type = rand.nextInt(4)
            when (type) {
                0 -> genForFromConsts(i, depth)
                1 -> genForFromNewList(i, depth)
                2 -> genIfFromNewList(i, depth)
                3 -> genIfFromConsts(i, depth)
            }
            if (depth < maxDepth) {
                generateBody(depth + 1)
            }
            printWithTabs(depth, "}")
        }
    }

    fun generate() {
        printWithTabs(0, "fun main(args: Array<String>) {")
        generateBody(1)
        printWithTabs(0, "}")
        fileWriter.close()
    }
}

fun main(args: Array<String>) {
    for (i in 0 .. 0) {
        println(i)
        val testGenerator = TestGenerator("input.kt")
        testGenerator.generate()
        val processCompile = ProcessBuilder("/home/mano/KOTLIN/kotlin-native/dist/bin/konanc", "input.kt").start()
        processCompile.waitFor()
        println("Compiled")
        val logPath = Paths.get("/home/mano/KOTLIN/ForGenerator")
        val processRun1 = ProcessBuilder("/home/mano/KOTLIN/ForGenerator/program.kexe")
        processRun1.redirectOutput(ProcessBuilder.Redirect.to(logPath.resolve("output1.txt").toFile()))
        processRun1.start().waitFor()
        println("Runned 1")
        val processRun2 = ProcessBuilder("/home/mano/KOTLIN/ForGenerator/program.kexe")
        processRun2.redirectOutput(ProcessBuilder.Redirect.to(logPath.resolve("output2.txt").toFile()))
        processRun2.start().waitFor()
        println("Runned 2")
        val f1 = Files.readAllBytes(Paths.get("/home/mano/KOTLIN/ForGenerator/output1.txt"))
        val f2 = Files.readAllBytes(Paths.get("/home/mano/KOTLIN/ForGenerator/output2.txt"))
        var equals = f1.size == f2.size
        if (!equals) {
            println("FAILED")
            break
        }
        for (j in 0 until f1.size) {
            if (f1[j] != f2[j]) {
                equals = false
                break
            }
        }
        if (equals) {
            println("OK")
        } else {
            println("FAILED")
            break
        }
    }
}