package ch.unil.pafanalysis.analysis.steps.scatter_plot

import org.apache.commons.math3.stat.regression.SimpleRegression

class ComputeRegression {

    fun computeRegression(data: List<ScatterPoint>?): LinearRegression? {
        val regression = SimpleRegression()
        val rData: Array<DoubleArray>? = data?.map{a -> doubleArrayOf(a.x ?: Double.NaN, a.y ?: Double.NaN) }?.toTypedArray()
        return if(rData != null){
            regression.addData(rData)
           LinearRegression(slope = regression.slope, intercept = regression.intercept, rSquare = regression.rSquare)
        } else null
    }

}