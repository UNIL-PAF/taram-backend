package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.File

@Service
class AsyncTransformationRunner() : CommonStep() {

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()
    private val writeImputation = WriteImputationTableData()

    @Autowired
    val logTransformationRunner: LogTransformationRunner? = null

    @Autowired
    val normalizationRunner: NormalizationRunner? = null

    @Autowired
    val imputationRunner: ImputationRunner? = null

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val defaultResult = Transformation(newStep?.commonResult?.numericalColumns, newStep?.columnInfo?.columnMapping?.intCol)
            val transformationParams = gson.fromJson(newStep?.parameters, TransformationParams().javaClass)

            transformTable(
                newStep,
                transformationParams
            )

            val commonResult = newStep?.commonResult?.copy(intColIsLog = transformationParams.transformationType == TransformationType.LOG2.value)

            newStep?.copy(
                results = gson.toJson(defaultResult),
                commonResult = commonResult
            )
        }

        tryToRun(funToRun, newStep)
    }

    fun transformTable(
        step: AnalysisStep?,
        transformationParams: TransformationParams,
    ): Long {
        val intCol = transformationParams.intCol ?: step?.columnInfo?.columnMapping?.intCol
        val table = readTableData.getTable(getOutputRoot() + step?.resultTablePath, step?.commonResult?.headers)
        val (selHeaders, ints) = readTableData.getDoubleMatrix(table, intCol)

        val transInts = logTransformationRunner!!.runTransformation(ints, transformationParams)
        val normInts = normalizationRunner!!.runNormalization(transInts, transformationParams)
        val (impInts, imputedRows) = imputationRunner!!.runImputation(normInts, transformationParams)

        val newCols: List<List<Any>>? = table.cols?.mapIndexed{ i, c ->
            val selHeader = selHeaders.withIndex().find{ it.value.idx == i }
            if (selHeader != null) {
                impInts[selHeader.index]
            }else c
        }

        val imputationTable = ImputationTable(selHeaders, imputedRows)
        val imputationFileName = (getOutputRoot() + step?.resultTablePath).replace(".txt", "_imputation.txt")
        writeImputation.write(imputationFileName, imputationTable)
        val resTable = writeTableData.write(getOutputRoot() + step?.resultTablePath, table.copy(cols = newCols))
        return Crc32HashComputations().computeFileHash(File(resTable))
    }

}