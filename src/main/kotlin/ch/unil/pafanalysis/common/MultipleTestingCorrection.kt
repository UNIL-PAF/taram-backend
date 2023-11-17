package ch.unil.pafanalysis.common

class MultipleTestingCorrection {

    fun fdrCorrection(pVals: List<Double>): List<Double> {
        val m = pVals.size
        var adjustedPvalues: Array<Double?> = arrayOfNulls(m)
        val pValsWithIdx = pVals.zip(pVals.indices)

        // order the pvalues.
        val (pvalues, idx) = pValsWithIdx.sortedBy { it.first }.unzip() // pVals.sorted()

        // iterate through all p-values:  largest to smallest
        for (i in m - 1 downTo 0) {
            if (i == m - 1) {
                adjustedPvalues[i] = pvalues[i]
            } else {
                val unadjustedPvalue = pvalues[i];
                val divideByM: Double = (i + 1).toDouble();
                val left: Double = adjustedPvalues[i + 1] ?: throw Exception("AdjustedPValues out of bound")
                val right: Double = (m / divideByM) * unadjustedPvalue;
                adjustedPvalues[i] = left.coerceAtMost(right);
            }
        }

        val adjustedZipped = adjustedPvalues.map { it ?: throw Exception("AdjustedPValue cannot be null.") }.toList().zip(idx)
        return adjustedZipped.sortedBy { it.second }.map{it.first}
    }

}