package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonResult
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.Crc32HashComputations
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.WriteTableData
import com.google.common.math.Quantiles
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import org.springframework.stereotype.Service
import java.io.File
import kotlin.math.ln

@Service
class TransformationRunner() : CommonStep(), CommonRunner {

    override var type: AnalysisStepType? = AnalysisStepType.BOXPLOT

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()

    val defaultParams = TransformationParams(
        normalizationType = NormalizationType.MEDIAN.value,
        transformationType = TransformationType.NONE.value,
        imputationType = ImputationType.NAN.value
    )

    override fun createPdf(step: AnalysisStep, document: Document?, pdf: PdfDocument): Document?{
        val title = Paragraph().add(Text(step.type).setBold())
        val transParams = gson.fromJson(step.parameters, TransformationParams::class.java)
        val selCol = Paragraph().add(Text("Selected column: ${transParams.intCol}"))
        document?.add(title)
        document?.add(selCol)
        if(step.comments !== null) document?.add(Paragraph().add(Text(step.comments)))
        return document
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val paramsString: String = params ?: ((step?.parameters) ?: gson.toJson(defaultParams))
        val newStep = runCommonStep(AnalysisStepType.TRANSFORMATION, oldStepId, true, step, paramsString)
        val defaultResult = Transformation(newStep?.commonResult?.numericalColumns, newStep?.commonResult?.intCol)

        val (resultTableHash, commonResult) = transformTable(newStep, gson.fromJson(paramsString, TransformationParams().javaClass))
        val stepWithRes = newStep?.copy(
            parameters = paramsString,
            parametersHash = hashComp.computeStringHash(paramsString),
            resultTableHash = resultTableHash,
            results = gson.toJson(defaultResult)
        )
        val oldStep = analysisStepRepository?.findById(oldStepId)
        val newHash = computeStepHash(stepWithRes, oldStep)

        val updatedStep =
            stepWithRes?.copy(status = AnalysisStepStatus.DONE.value, stepHash = newHash, commonResult = commonResult)
        return analysisStepRepository?.save(updatedStep!!)!!
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = gson.fromJson(step.parameters, TransformationParams::class.java)
        val origParams = if(origStep?.parameters != null) gson.fromJson(step.parameters, TransformationParams::class.java) else null

        return "Parameter(s) changed:"
            .plus(if(params.intCol != origParams?.intCol) " [Column: ${params.intCol}]" else null)
            .plus(if(params.normalizationType != origParams?.normalizationType) " [Normmalization: ${params.normalizationType}]" else "")
            .plus(if(params.transformationType != origParams?.transformationType) " [Transformation: ${params.transformationType}]" else "")
    }

    private fun transformTable(
        step: AnalysisStep?,
        transformationParams: TransformationParams
    ): Pair<Long, CommonResult?> {
        val expDetailsTable = step?.columnInfo?.columnMapping?.experimentNames?.map { name ->
            step?.columnInfo?.columnMapping?.experimentDetails?.get(name)
        }?.filter { it?.isSelected ?: false }

        val ints =
            readTableData.getListOfInts(expInfoList = expDetailsTable, analysisStep = step, outputRoot = outputRoot)
        val transInts = transformation(ints, transformationParams)
        val normInts: List<Pair<String, List<Double>>> = normalization(transInts, transformationParams)
        val intCol = transformationParams.intCol ?: step?.commonResult?.intCol

        val newColName = "trans $intCol"

        val resTable = writeTableData.writeTable(step, normInts, outputRoot = outputRoot, newColName)
        val resTableHash = Crc32HashComputations().computeFileHash(File(resTable))
        val numCols =
            step?.commonResult?.numericalColumns?.filter { it != step?.commonResult?.intCol }?.plus(newColName)
        val commonRes = step?.commonResult?.copy(intCol = newColName, numericalColumns = numCols)
        return Pair(resTableHash, commonRes)
    }

    private fun transformation(
        ints: List<Pair<String, List<Double>>>,
        transformationParams: TransformationParams
    ): List<Pair<String, List<Double>>> {

        fun transformList(intList: List<Double>): List<Double> {
            val a: List<Double> = when (transformationParams.transformationType) {
                TransformationType.NONE.value -> intList
                TransformationType.LOG2.value -> {
                    val newList = intList.map { i ->
                        if (i == 0.0) {
                            Double.NaN
                        } else {
                            ln(i)// / ln(2.0)
                        }
                    }
                    newList
                }
                else -> {
                    throw StepException("${transformationParams.normalizationType} is not implemented.")
                }
            }
            return a
        }

        return ints.map { (name, orig: List<Double>) ->
            Pair(name, transformList(orig))
        }
    }


    private fun normalization(
        ints: List<Pair<String, List<Double>>>,
        transformationParams: TransformationParams
    ): List<Pair<String, List<Double>>> {
        val subtract = when (transformationParams.normalizationType) {
            NormalizationType.MEDIAN.value -> fun(orig: List<Double>): Double {
                return Quantiles.median().compute(orig)
            }
            NormalizationType.MEAN.value -> fun(orig: List<Double>): Double { return orig.average() }
            else -> {
                throw StepException("${transformationParams.normalizationType} is not implemented.")
            }
        }
        return ints.map { (name, orig: List<Double>) ->
            val noNaNs = orig.filter { ! it.isNaN() }
            Pair(name, orig.map { it - subtract(noNaNs) })
        }
    }

}