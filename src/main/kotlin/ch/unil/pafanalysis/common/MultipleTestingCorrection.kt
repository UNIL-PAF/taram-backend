package ch.unil.pafanalysis.common

class MultipleTestingCorrection {

    fun fdrCorrection(pVals: List<Double>): List<Double> {
        val m = pVals.size
        var adjustedPvalues: Array<Double?> = arrayOfNulls<Double>(m)

            // order the pvalues.
            val pvalues = pVals.sorted()

            // iterate through all p-values:  largest to smallest
            for(i in m-1 downTo 0){
                if(i == m-1){
                    adjustedPvalues[i] = pvalues[i]
                } else {
                    val unadjustedPvalue = pvalues[i];
                    val divideByM: Double = (i + 1).toDouble();
                    val left: Double = adjustedPvalues[i + 1] ?: throw Exception("adjustedPvalues out of bound")
                    val right: Double = (m / divideByM) * unadjustedPvalue;
                    adjustedPvalues[i] = left.coerceAtMost(right);
                }
        /*

            for ( i: Int = m - 1; i >= 0; i--) {
            if (i == m - 1) {
                adjustedPvalues[i] = pvalues[i];
            } else {
                double unadjustedPvalue = pvalues[i];
                int divideByM = i + 1;
                double left = adjustedPvalues[i + 1];
                double right = (m / (double) divideByM) * unadjustedPvalue;
                adjustedPvalues[i] = Math.min(left, right);
            }
        }

         */
        }
        return adjustedPvalues.map{ it ?: throw Exception("AdjustedPValue cannot be null.") }.toList()
    }

}