package ch.unil.pafanalysis.analysis.steps.imputation

import ch.unil.pafanalysis.analysis.steps.StepException
import com.github.rcaller.rstuff.RCaller
import com.github.rcaller.rstuff.RCallerOptions
import com.github.rcaller.rstuff.RCode
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.random.RandomGenerator
import org.apache.commons.math3.random.Well1024a
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import org.springframework.stereotype.Service

@Service
class ImputationComputation() {

    val standardDeviation = StandardDeviation()

    fun runImputation(
        ints: List<List<Double>>,
        params: ImputationParams
    ): Pair<List<List<Double>>, List<List<Boolean>>?> {
        val newInts = when (params.imputationType) {
            ImputationType.NONE.value -> ints
            ImputationType.NAN.value -> replaceMissingBy(ints, Double.NaN)
            ImputationType.VALUE.value -> replaceMissingBy(ints, params.replaceValue)
            ImputationType.NORMAL.value -> replaceByNormal(ints, params.normImputationParams)
            ImputationType.FOREST.value -> randomForestImputation(ints, params.forestImputationParams?.nTree, params.forestImputationParams?.maxIter, params.forestImputationParams?.fixedRes)
            ImputationType.QRILC.value -> qrilcImpuation(ints, params.qrilcImputationParams?.fixedRes)
            else -> {
                throw StepException("${params.imputationType} is not implemented.")
            }
        }

        val imputedRows = if (params.imputationType != ImputationType.NONE.value) getImputedPos(ints) else null
        return Pair(newInts, imputedRows)
    }

    fun getImputedPos(cols: List<List<Double>>): List<List<Boolean>> {
        return cols.fold(emptyList()) { acc, col ->
            if (acc.isEmpty()) {
                col.map { listOf((it.isNaN() || it == 0.0)) }
            } else {
                col.mapIndexed { i, c -> acc[i].plus(c.isNaN() || c == 0.0) }
            }
        }
    }

    fun replaceByNormal(
        ints: List<List<Double>>,
        params: NormImputationParams?
    ): List<List<Double>> {
        return ints.map { col ->
            val cleanInts = col.filter { !it.isNaN() && !it.isInfinite() }
            val sd = standardDeviation.evaluate(cleanInts.toDoubleArray())
            val sdCorr = params!!.width!! * sd
            val mean = cleanInts.average() - params!!.downshift!! * sd
            val random: RandomGenerator = Well1024a()
            val normDist = NormalDistribution(random, mean, sdCorr)
            if (params!!.seed != null) random.setSeed(params!!.seed!!)
            col.map { i ->
                if (i.isNaN() || i == 0.0) normDist.sample() else i
            }
        }
    }

    fun replaceMissingBy(
        ints: List<List<Double>>,
        replaceValue: Double?
    ): List<List<Double>> {
        if (replaceValue == null) return ints
        return ints.map { col ->
            col.map { i -> if (i.isNaN() || i == 0.0) replaceValue else i }
        }
    }

    fun qrilcImpuation(ints: List<List<Double>>, fixedRes: Boolean? = false): List<List<Double>> {
        val code = RCode.create()
        code.addDoubleMatrix("m", ints.map { it.toDoubleArray() }.toTypedArray())
        code.R_require("imputeLCMD")
        code.addRCode("m[m == 0] <- NA")
        if(fixedRes == true) code.addRCode("set.seed(123)")
        code.addRCode("qrilc_res <- impute.QRILC(t(m))")
        code.addRCode("res <- list(qrilc = t(qrilc_res[[1]]))")
        val caller = RCaller.create(code, RCallerOptions.create())
        caller.runAndReturnResult("res")

        return caller.parser.getAsDoubleMatrix("qrilc").map { it.toList() }
    }


    fun randomForestImputation(
        ints: List<List<Double>>,
        nTree: Int? = null,
        maxIter: Int? = null,
        fixedRes: Boolean? = false
    ): List<List<Double>> {
        val code = RCode.create()
        code.addDoubleMatrix("m", ints.map { it.toDoubleArray() }.toTypedArray())
        code.addInt("max_iter", maxIter ?: 10)
        code.addInt("n_tree", nTree ?: 100)
        code.R_require("missForest")
        code.R_require("parallel")
        code.R_require("doParallel")
        code.addRCode("m[m == 0] <- NA")
        code.addRCode("n_cores <- detectCores() - 1")
        code.addRCode("cl <- makeCluster(n_cores)")
        code.addRCode("registerDoParallel(cl)")
        if(fixedRes == true) code.addRCode("set.seed(123, kind = \"L'Ecuyer-CMRG\")")
        code.addRCode("forest_res <- missForest(as.data.frame(m), maxiter = max_iter, ntree = n_tree, parallelize = \"variables\")")
        code.addRCode("stopCluster(cl)")


        /* DEBUG R CODE
        code.addRCode("r_output <- capture.output(summary(t(forest\$ximp)))")
        code.addRCode("r_output");
        val caller = RCaller.create(code, RCallerOptions.create())
        caller.runAndReturnResult("r_output")


        val outputLines = caller.parser.getAsStringArray("r_output")
        for (line in outputLines) {
            println("R said: $line")
        }
         */

        code.addRCode("forest <- data.matrix(forest_res\$ximp)")
        code.addRCode("forest")
        val caller = RCaller.create(code, RCallerOptions.create())
        caller.runAndReturnResult("forest")
        return caller.parser.getAsDoubleMatrix("forest").map { it.toList() }
    }

}