package ch.unil.pafanalysis.analysis.steps

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import com.itextpdf.layout.element.Paragraph

interface CommonRunner {
    fun run(oldStepId: Int, step: AnalysisStep? = null): AnalysisStep

    fun updatePlotOptions(step: AnalysisStep, echartsPlot: EchartsPlot): String {
        throw Exception("'updatePlotOptions' is not implemented for this Runner.")
    }

    fun createPdf(step: AnalysisStep): List<Paragraph>
}