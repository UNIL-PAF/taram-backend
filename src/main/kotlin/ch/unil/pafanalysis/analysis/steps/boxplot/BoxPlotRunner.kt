package ch.unil.pafanalysis.analysis.steps.boxplot

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.EchartsPlot
import ch.unil.pafanalysis.analysis.steps.transformation.TransformationParams
import ch.unil.pafanalysis.common.ReadTableData
import com.google.common.math.Quantiles
import com.google.gson.Gson
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.IBlockElement
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.svg.converter.SvgConverter
import org.springframework.stereotype.Service
import java.io.FileInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.math.log2

@Service
class BoxPlotRunner() : CommonStep(), CommonRunner {

    override var type: AnalysisStepType? = AnalysisStepType.BOXPLOT

    private val gson = Gson()

    private val readTableData = ReadTableData()

    override fun createPdf(step: AnalysisStep, document: Document?): Document? {
        val title = Paragraph().add(Text(step.type).setBold())
        val params = gson.fromJson(step.parameters, BoxPlotParams::class.java)
        val selCol = Paragraph().add(Text("Selected column: ${params?.column}"))
        val results = gson.fromJson(step.results, BoxPlot::class.java)
        val echartsPlot = results.plot?.copy(outputPath = step.resultPath)

        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:3001/svg"))
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(echartsPlot)))
            .build();
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val outputRoot = getOutputRoot(getResultType(step?.analysis?.result?.type))

        val svgPath = outputRoot + response.body()

        //val image: Image = SvgConverter.convertToImage(FileInputStream(svgPath), pdf)

        document?.add(title)
        document?.add(selCol)

        return document
    }

    override fun run(oldStepId: Int, step: AnalysisStep?): AnalysisStep {
        val newStep = runCommonStep(AnalysisStepType.BOXPLOT, oldStepId, false, step)
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