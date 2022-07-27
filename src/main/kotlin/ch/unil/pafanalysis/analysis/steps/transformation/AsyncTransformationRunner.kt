package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.steps.CommonResult
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.Crc32HashComputations
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.WriteTableData
import ch.unil.pafanalysis.results.model.ResultType
import com.google.common.math.Quantiles
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import kotlin.math.ln

@Service
class AsyncTransformationRunner() : CommonStep() {

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()

    @Autowired
    val logTransformationRunner: LogTransformationRunner? = null

    @Autowired
    val normalizationRunner: NormalizationRunner? = null

    @Autowired
    val imputationRunner: ImputationRunner? = null

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?, paramsString: String?) {
        val funToRun: () -> AnalysisStep? = {
            val defaultResult = Transformation(newStep?.commonResult?.numericalColumns, newStep?.commonResult?.intCol)
            val transformationParams = gson.fromJson(paramsString, TransformationParams().javaClass)

            val resultTableHash = transformTable(
                newStep,
                transformationParams,
                getOutputRoot()
            )

            newStep?.copy(
                resultTableHash = resultTableHash,
                results = gson.toJson(defaultResult),
            )
        }

        tryToRun(funToRun, newStep)
    }

    fun transformTable(
        step: AnalysisStep?,
        transformationParams: TransformationParams,
        outputRoot: String?
    ): Long {
        transformationParams.intCol

        val intCol = transformationParams.intCol ?: step?.commonResult?.intCol
        val table = readTableData.getTable(getOutputRoot() + step?.resultTablePath, step?.columnInfo?.columnMapping)
        val (selHeaders, ints) = readTableData.getDoubleMatrix(table, intCol)

        val transInts = logTransformationRunner!!.runTransformation(ints, transformationParams)
        val normInts = normalizationRunner!!.runNormalization(transInts, transformationParams)
        val impInts = imputationRunner!!.runImputation(normInts, transformationParams)

        val newCols: List<List<Any>>? = table.cols?.mapIndexed{ i, c ->
            val selHeader = selHeaders.withIndex().find{ it.value.idx == i }
            if (selHeader != null) {
                impInts[selHeader.index]
            }else c

        }

        val resTable = writeTableData.write(getOutputRoot() + step?.resultTablePath, table.copy(cols = newCols))
        return Crc32HashComputations().computeFileHash(File(resTable))
    }

}