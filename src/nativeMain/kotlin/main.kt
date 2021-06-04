import kotlin.math.*

const val K_p = 4.16
const val K_d = -2.99944
const val Amp = 0.4
const val a = 0.26
const val b = 4.47
const val step = 0.001

fun main() {
    val testOmegaList = listOf(
        0.4, 0.6, 0.9, 1.35, 2.025, 3.04, 4.56, 6.83,
        10.3, 15.4, 23.0, 34.6, 40.0
    )
    val testOmegaList2 = List(100) { i -> 0.4 + i / 100.0 * 40.0}
    for(omega in testOmegaList) {
        val tester = FreqGenerator(omega)
        tester.run()
        //println("size: ${tester.targetPeakAmps.size}, ${tester.xPeakAmps.size}, ${tester.targetZeroTimes.size}, ${tester.xZeroTimes.size}")
        val pickIndex = ceil((tester.xZeroTimes.size - 1) * 0.8).toInt()
        val axd = tester.targetPeakAmps[pickIndex]
        val ax = tester.xPeakAmps[pickIndex]
        val y = 20 * log10(ax/axd)

        val mx = abs(tester.targetZeroTimes[pickIndex] - tester.xZeroTimes[pickIndex])
        val nx = (tester.xZeroTimes[pickIndex] - tester.xZeroTimes[pickIndex-1])
        val phi = -360.0 * mx / nx
        println("$omega\t$y\t$phi")
    }
}

class FreqGenerator(
    val w: Double
) {
    var xd = 0.0
    var x = 0.0
    var dx = 0.0
    var va = 0.0
    var tmp = 0.0
    var t = 0.0

    private var targetIsUp = true
    private var xIsUp = true
    private var targetIsPlus = true
    private var xIsPlus = true
    private var prevXd = 0.0
    private var prevX = 0.0

    val targetZeroTimes = mutableListOf<Double>()
    val xZeroTimes = mutableListOf<Double>()
    val targetPeakAmps = mutableListOf<Double>()
    val xPeakAmps = mutableListOf<Double>()

    private fun sine() {
        xd = if(t < 0.0) 0.0 else Amp * sin(w * t)
    }

    private fun controller() {
        va = K_p * (xd - x) - K_d * dx
    }

    private fun motor() {
        x += dx * step
        tmp += (b / a) * (va - tmp) * step
        dx = (1 / b) * tmp
    }

    fun run(): List<FreqResultData> {
        val res = mutableListOf<FreqResultData>()
        while(t <= 30) {
            sine()
            controller()
            motor()

            //0クロス(立ち下がり時検知)
            if(!targetIsUp) {
                if(xd == 0.0 || (targetIsPlus && xd < 0)) {
                    targetZeroTimes.add(t)
                }
            }
            if(!xIsUp) {
                if(x == 0.0 || (xIsPlus && x < 0)) {
                    if(xZeroTimes.isEmpty() && targetZeroTimes.size >= 2) {
                        //最初の方で0クロスが起きない場合の対処
                        for(i in 0 until targetZeroTimes.size - 1) {
                            xZeroTimes.add(0.0)
                        }
                    }
                    xZeroTimes.add(t)
                }
            }

            //ピーク
            if(targetIsUp && xd < prevXd) {
                targetPeakAmps.add(xd)
            }
            if(xIsUp && x < prevX) {
                xPeakAmps.add(x)
            }

            res.add(FreqResultData(t,xd,x))

            if(xd > prevXd) targetIsUp = true
            else if(xd < prevXd) targetIsUp = false

            if(x > prevX) xIsUp = true
            else if(x < prevX) xIsUp = false

            targetIsPlus = xd >= 0
            xIsPlus = x >= 0

            prevXd = xd
            prevX = x

            t += step
        }
        return res
    }

    data class FreqResultData(
        val time: Double,
        val xd: Double,
        val x: Double
    )
}
