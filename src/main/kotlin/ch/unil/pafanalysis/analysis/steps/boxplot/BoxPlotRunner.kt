package ch.unil.pafanalysis.analysis.steps.boxplot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.EchartsPlot
import ch.unil.pafanalysis.analysis.steps.transformation.ImputationType
import ch.unil.pafanalysis.analysis.steps.transformation.NormalizationType
import ch.unil.pafanalysis.analysis.steps.transformation.TransformationParams
import ch.unil.pafanalysis.analysis.steps.transformation.TransformationType
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.swing.Box


@Service
class BoxPlotRunner() : CommonStep(), CommonRunner {

    override var type: AnalysisStepType? = AnalysisStepType.BOXPLOT

    @Autowired
    var asyncBoxplotRunner: AsyncBoxPlotRunner? = null

    override fun createPdf(step: AnalysisStep, document: Document?, pdf: PdfDocument): Document? {
        val title = Paragraph().add(Text(step.type).setBold())
        val params = gson.fromJson(step.parameters, BoxPlotParams::class.java)
        val selCol = Paragraph().add(Text("Selected column: ${params?.column}"))

        document?.add(title)
        document?.add(selCol)
        document?.add(makeEchartsPlot(step, pdf))
        if (step.comments !== null) document?.add(Paragraph().add(Text(step.comments)))

        return document
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, oldStepId, false, step, params)
        val boxplotParams: BoxPlotParams? = if(step?.parameters != null) gson.fromJson(step?.parameters, BoxPlotParams().javaClass) else BoxPlotParams()

        val paramsHash = hashComp.computeStringHash(boxplotParams?.toString())
        val stepWithHash = newStep?.copy(parametersHash = paramsHash, parameters = gson.toJson(boxplotParams))
        val stepWithDiff = stepWithHash?.copy(copyDifference = getCopyDifference(stepWithHash))

        asyncBoxplotRunner?.runAsync(oldStepId, stepWithDiff)
        return stepWithDiff!!
    }

    override fun updatePlotOptions(step: AnalysisStep, echartsPlot: EchartsPlot): String {
        val newResults = gson.fromJson(step.results, BoxPlot().javaClass).copy(plot = echartsPlot)
        val newStep = step.copy(results = gson.toJson(newResults))
        analysisStepRepository?.saveAndFlush(newStep)
        return echartsPlot.echartsHash.toString()
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = gson.fromJson(step.parameters, BoxPlotParams().javaClass)
        val origParams =
            if (origStep?.parameters != null) gson.fromJson(origStep?.parameters, BoxPlotParams().javaClass) else null

        return "Parameter(s) changed:"
            .plus(if (params.column != origParams?.column) " [Column: ${params.column}]" else "")
            .plus(if (params.logScale != origParams?.logScale) " [Log scale: ${params.logScale}]" else "")
    }

}