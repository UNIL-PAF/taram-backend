package ch.unil.pafanalysis.analysis.steps.normalization

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.HeaderTypeMapping
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import com.google.common.math.Quantiles
import org.springframework.stereotype.Service

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

        val selProtNorm = if(params.normalizationType == NormalizationType.SEL_PROT.value) computeSelProtNorm(step, params, intCol) else null

        val myNumber = when (params.normalizationType) {
            NormalizationType.MEDIAN.value -> fun(orig: List<Double>, i: Int?): Double {
                return Quantiles.median().compute(orig)
            }
            NormalizationType.MEAN.value -> fun(orig: List<Double>, i: Int?): Double { return orig.average() }
            NormalizationType.SEL_PROT.value -> fun(orig: List<Double>, i: Int?): Double {
                if(selProtNorm == null) throw StepException("There is no data available for selected proteins.")
                val median = Quantiles.median().compute(selProtNorm.mapNotNull { it?.takeUnless(Double::isNaN) })
                val normVals = selProtNorm.map{a -> a?.minus(median)}
                return normVals[i ?: 0] ?: 0.0
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
        return transpose(selProts)?.map{ col ->
            Quantiles.median().compute(col.mapNotNull { it?.takeUnless(Double::isNaN) })
        }
    }

    private fun transpose(matrix: List<List<Double?>>?): List<List<Double?>>? {
        val rows = matrix?.size
        val cols = matrix?.get(0)?.size ?: return null

        return List(cols) { c ->
            List(rows ?: 0) { r ->
                matrix[r][c]
            }
        }
    }

    private fun getSelProtData(table: Table?,
                               intCol: String?,
                               params: NormalizationParams?,
                               resType: String?,
                               expDetails: Map<String, ExpInfo>?): List<List<Double?>>? {
        val (_, intMatrix) = readTableData.getDoubleMatrix(table, intCol, expDetails)

        val protGroup = readTableData.getStringColumn(table, hMap.getCol("proteinIds", resType))?.map { it.split(";")[0] }
        val selProts: List<List<Double?>>? = params?.selProts?.map{p ->
            val i = protGroup?.indexOf(p)
            if(i != null && i >= 0){
                intMatrix.map { if (it[i].isNaN()) null else it[i] }
            }else null
        }?.filterNotNull()

        return selProts
    }

}