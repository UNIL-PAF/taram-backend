package ch.unil.pafanalysis.analysis.steps.summary_stat

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.EchartsPlot
import ch.unil.pafanalysis.analysis.steps.boxplot.BoxPlot
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Path

@Service
class SummaryStatRunner() : CommonStep(), CommonRunner {

    @Lazy
    @Autowired
    var asyncRunner: AsyncSummaryStatRunner? = null

    override var type: AnalysisStepType? = AnalysisStepType.SUMMARY_STAT

    fun getParameters(step: AnalysisStep?): SummaryStatParams {
        return if (step?.parameters != null) gson.fromJson(
            step?.parameters,
            SummaryStatParams().javaClass
        ) else SummaryStatParams()
    }

    override fun createPdf(step: AnalysisStep, document: Document?, pdf: PdfDocument, pageSize: PageSize?, stepNr: Int): Document? {
        val title = Paragraph().add(Text(step.type).setBold())
        val transParams = gson.fromJson(step.parameters, SummaryStatParams::class.java)
        val selCol = Paragraph().add(Text("Selected column: ${transParams.intCol}"))
        document?.add(title)
        document?.add(selCol)
        if (step.comments !== null) document?.add(Paragraph().add(Text(step.comments)))
        return document
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, oldStepId, false, step, params)
        asyncRunner?.runAsync(oldStepId, newStep)
        return newStep!!
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = getParameters(step)
        val origParams = getParameters(origStep)

        return "Parameter(s) changed:"
            .plus(if (params.intCol != origParams?.intCol) " [Column: ${params.intCol}]" else "")
    }

    override fun getResultByteArray(step: AnalysisStep?): ByteArray? {
        val results = if (step?.results != null) gson.fromJson(
            step?.results,
            SummaryStat().javaClass
        ) else SummaryStat()

        val tableFile = createTmpFile(results)

        val inputStream: InputStream = FileInputStream(tableFile)
        return inputStream.readAllBytes()
    }

    private fun createTmpFile(results: SummaryStat): File {
        val tmpFile = kotlin.io.path.createTempFile("summary_table_", ".txt").toFile()
        val writer = tmpFile.bufferedWriter()

        // there should always be a min
        val nrEntries = results.min?.size

        writeResLine("Name", results.expNames, nrEntries, writer)
        writeResLine("Group", results.groups, nrEntries, writer)
        writeResLine("Min", results.min, nrEntries, writer)
        writeResLine("Max", results.max, nrEntries, writer)
        writeResLine("Mean", results.mean, nrEntries, writer)
        writeResLine("Median", results.median, nrEntries, writer)
        writeResLine("Sum", results.sum, nrEntries, writer)
        writeResLine("Std dev", results.stdDev, nrEntries, writer)
        writeResLine("Std err", results.stdErr, nrEntries, writer)
        writeResLine("Coef of var", results.coefOfVar, nrEntries, writer)
        writeResLine("Nr of valid", results.nrValid, nrEntries, writer)
        writeResLine("Nr of NaN", results.nrNaN, nrEntries, writer)

        writer.close()
        return tmpFile
    }

    private fun writeResLine(name: String, data: List<Any>?, nrEntries: Int?, tmpFile: BufferedWriter){
        val sep = "\t"
        tmpFile.write(name)
        tmpFile.write(sep)
        val conc = data?.map { it.toString() }?.joinToString(sep)
        tmpFile.write(conc?:List<String>(nrEntries?:0){""}.joinToString(sep))
        tmpFile.write("\n")
    }

}