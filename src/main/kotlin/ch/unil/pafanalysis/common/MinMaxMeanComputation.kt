package ch.unil.pafanalysis.common

class MinMaxMeanComputation {

    data class MinMaxMean(val min: Double, val max: Double, val mean: Double)

    fun getMinMaxMean(ints: List<List<Double>>): MinMaxMean {
        val (min, max, sum) = ints.fold(kotlin.Triple(kotlin.Double.MAX_VALUE, kotlin.Double.MIN_VALUE, 0.0)) { acc, i ->
            val myFolded = i.fold(acc) { acc2, i2 -> getMinMaxSum(acc2, myI = i2) }
            getMinMaxSum(myFolded, myTrip = acc)
        }
        val nrEntries = ints.size * ints[0].size
        return MinMaxMean(min, max, sum/nrEntries)
    }

    private fun getMinMaxSum(
        myAcc: Triple<Double, Double, Double>,
        myTrip: Triple<Double, Double, Double>? = null,
        myI: Double? = null
    ): Triple<Double, Double, Double> {
        val t = if (myTrip == null) Triple(myI!!, myI!!, myI!!) else myTrip
        val min = if (t.first < myAcc.first) t.first else myAcc.first
        val max = if (t.second > myAcc.second) t.second else myAcc.second
        return Triple(min, max, myAcc.third + t.third)
    }
}