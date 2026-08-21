package ch.unil.pafanalysis.analysis.steps.normalization

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.analysis.steps.boxplot.BoxPlotParams
import ch.unil.pafanalysis.analysis.steps.boxplot.SelProtData
import ch.unil.pafanalysis.common.DefaultColors
import ch.unil.pafanalysis.common.HeaderTypeMapping
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import com.google.common.math.Quantiles
import org.springframework.stereotype.Service
import kotlin.collections.ifEmpty
import kotlin.collections.sortedByDescending
import kotlin.math.log2

@Service
class NormalizationComputation() : CommonStep() {

    private val readTableData = ReadTableData()
    private val hMap = HeaderTypeMapping()

    fun runNormalization(
        step: AnalysisStep?,
        ints: List<List<Double>>,
        params: NormalizationParams,
        intCol: String?,
    ): List<List<Double>> {
        if (params.normalizationType == NormalizationType.NONE.value) return ints

        val selProtNorm = computeSelProtNorm(step, params, intCol)

        val myNumber = when (params.normalizationType) {
            NormalizationType.MEDIAN.value -> fun(orig: List<Double>, i: Int?): Double {
                return Quantiles.median().compute(orig)
            }
            NormalizationType.MEAN.value -> fun(orig: List<Double>, i: Int?): Double { return orig.average() }
            NormalizationType.SEL_PROT.value -> fun(orig: List<Double>, i: Int?): Double {
                val avg = selProtNorm?.filterNotNull()?.average() ?: 0.0
                val normVals = selProtNorm?.map{a -> a?.minus(avg)}
                return normVals?.get(i ?: 0) ?: 0.0
            }
            else -> {
                throw StepException("${params.normalizationType} is not implemented.")
            }
        }

        return ints.mapIndexed {i:Int,  orig: List<Double> ->
            val noNaNs = orig.filter { !it.isNaN() }
            when (params.normalizationCalculation) {
                NormalizationCalculation.DIVISION.value -> orig.map { it - myNumber(noNaNs, i) }
                NormalizationCalculation.SUBSTRACTION.value -> orig.map { it - myNumber(noNaNs, i) }
                else -> throw StepException("${params.normalizationCalculation} is not implemented.")
            }
        }
    }

    private fun computeSelProtNorm(step: AnalysisStep?, params: NormalizationParams, intCol: String?,): List<Double?>? {
        val table = readTableData.getTable(
            getOutputRoot().plus(step?.resultTablePath),
            step?.commonResult?.headers
        )
        val selProts = getSelProtData(table, intCol, params, step?.analysis?.result?.type, step?.columnInfo?.columnMapping?.experimentDetails)
        return selProts?.get(0)
    }

    private fun getSelProtData(table: Table?,
                               intCol: String?,
                               params: NormalizationParams?,
                               resType: String?,
                               expDetails: Map<String, ExpInfo>?): List<List<Double?>>? {
        val (headers, intMatrix) = readTableData.getDoubleMatrix(table, intCol, expDetails)

        val protGroup = readTableData.getStringColumn(table, hMap.getCol("proteinIds", resType))?.map { it.split(";")[0] }
        val selProts: List<List<Double?>>? = params?.selProts?.map{p ->
            val i = protGroup?.indexOf(p)
            if(i != null && i >= 0){
                intMatrix.map { if (it[i].isNaN()) null else it[i] }
            }else{
                null
            }
        }?.filterNotNull()

        return selProts
    }

}