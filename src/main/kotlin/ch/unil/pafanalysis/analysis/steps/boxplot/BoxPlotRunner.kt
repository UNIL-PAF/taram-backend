package ch.unil.pafanalysis.analysis.steps.boxplot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.EchartsPlot
import ch.unil.pafanalysis.common.ReadTableData
import com.google.common.math.Quantiles
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import org.springframework.stereotype.Service
import kotlin.math.log2


@Service
class BoxPlotRunner() : CommonStep(), CommonRunner {

    override var type: AnalysisStepType? = AnalysisStepType.BOXPLOT

    private val readTableData = ReadTableData()

    override fun createPdf(step: AnalysisStep, document: Document?, pdf: PdfDocument): Document? {
        val title = Paragraph().add(Text(step.type).setBold())
        val params = gson.fromJson(step.parameters, BoxPlotParams::class.java)
        val selCol = Paragraph().add(Text("Selected column: ${params?.column}"))

        document?.add(title)
        document?.add(selCol)
        document?.add(makeEchartsPlot(step, pdf))
        if(step.comments !== null) document?.add(Paragraph().add(Text(step.comments)))

        return document
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(AnalysisStepType.BOXPLOT, oldStepId, false, step, params)
        val boxplot = createBoxplotObj(newStep)
        val updatedStep = newStep?.copy(status = AnalysisStepStatus.DONE.value, results = gson.toJson(boxplot))
        analysisStepRepository?.save(updatedStep!!)
        return updatedStep!!
    }

    override fun updatePlotOptions(step: AnalysisStep, echartsPlot: EchartsPlot): String {
        val newResults = gson.fromJson(step.results, BoxPlot().javaClass).copy(plot = echartsPlot)
        val newStep = step.copy(results = gson.toJson(newResults))
        analysisStepRepository?.save(newStep)
        return echartsPlot.echartsHash.toString()
    }

    private fun createBoxplotObj(analysisStep: AnalysisStep?): BoxPlot {
        val expDetailsTable = analysisStep?.columnInfo?.columnMapping?.experimentNames?.map { name ->
            analysisStep?.columnInfo?.columnMapping?.experimentDetails?.get(name)
        }?.filter { it?.isSelected ?: false }

        val experimentNames = expDetailsTable?.map { it?.name!! }
        val groupedExpDetails: Map<String?, List<ExpInfo?>>? = expDetailsTable?.groupBy { it?.group }
        val boxplotGroupData = groupedExpDetails?.mapKeys { createBoxplotGroupData(it.key, it.value, analysisStep) }

        return BoxPlot(experimentNames = experimentNames, data = boxplotGroupData?.keys?.toList())
    }

    private fun createBoxplotGroupData(
        group: String?,
        expInfoList: List<ExpInfo?>?,
        analysisStep: AnalysisStep?
    ): BoxPlotGroupData {
        val logScale = if (analysisStep?.parameters != null) {
            val boxPlotParams: BoxPlotParams = gson.fromJson(analysisStep.parameters, BoxPlotParams().javaClass)
            boxPlotParams.logScale
        } else false

        val intColumn = if (analysisStep?.parameters != null) {
            val boxPlotParams: BoxPlotParams = gson.fromJson(analysisStep.parameters, BoxPlotParams().javaClass)
            boxPlotParams.column
        } else null

        val listOfInts = readTableData.getListOfInts(expInfoList, analysisStep, outputRoot, intColumn)
        val listOfBoxplots = listOfInts.map { BoxPlotData(it.first, computeBoxplotData(it.second, logScale)) }
        return BoxPlotGroupData(group = group, data = listOfBoxplots)
    }


    private fun computeBoxplotData(ints: List<Double>, logScale: Boolean?): List<Double>? {
        val normInts = if (logScale != false) {
            ints.filter { it != 0.0 && !it.isNaN() }.map { log2(it) }
        } else {
            ints
        }

        val intsFlt = normInts.filter { !it.isNaN() }

        val min = intsFlt.minOrNull()!!
        val q25: Double = Quantiles.percentiles().index(25).compute(intsFlt)
        val q50: Double = Quantiles.percentiles().index(50).compute(intsFlt)
        val q75: Double = Quantiles.percentiles().index(75).compute(intsFlt)
        val max = intsFlt.maxOrNull()!!
        return listOf(min, q25, q50, q75, max)
    }

}