package ch.unil.pafanalysis.analysis.steps.correlation_table

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

@Service
class CorrelationTableRunner() : CommonStep(), CommonRunner {

    val version = "1.0"

    @Lazy
    @Autowired
    var asyncRunner: AsyncOneCorrelationTableRunner? = null

    @Autowired
    var correlantionTablePdf: CorrelationTablePdf? = null

    override var type: AnalysisStepType? = AnalysisStepType.CORRELATION_TABLE

    @Autowired
    private var env: Environment? = null

    private fun getOutputPath(): String? {
        return env?.getProperty("output.path")
    }

    fun getParameters(step: AnalysisStep?): CorrelationTableParams {
        return if (step?.parameters != null) gson.fromJson(
            step?.parameters,
            CorrelationTableParams().javaClass
        ) else CorrelationTableParams()
    }

    override fun createPdf(step: AnalysisStep, pdf: PdfDocument, pageWidth: Float, stepNr: Int): Div? {
        return correlantionTablePdf?.createPdf(step, pdf, pageWidth, stepNr)
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, version, oldStepId, true, step, params)
        asyncRunner?.runAsync(oldStepId, newStep)
        return newStep!!
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = getParameters(step)
        val origParams = getParameters(origStep)

        return "Parameter(s) changed:"
    }

    override fun getOtherTableName(idx: Int): String? {
        return "Enrichment-table-$idx.txt"
    }


    override fun getResultByteArray(step: AnalysisStep?): ByteArray? {
        val result: CorrelationTable = gson.fromJson(step?.results, CorrelationTable().javaClass)
        val enrichmentResFilePath = getOutputPath() + step?.resultPath + "/" + result.correlationTable
        val inputStream: InputStream = FileInputStream(enrichmentResFilePath)
        return inputStream.readAllBytes()
    }
}