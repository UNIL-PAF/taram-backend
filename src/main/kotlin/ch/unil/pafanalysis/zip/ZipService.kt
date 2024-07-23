package ch.unil.pafanalysis.zip

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.analysis.service.AnalysisStepService
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepNames
import ch.unil.pafanalysis.common.EchartsServer
import ch.unil.pafanalysis.common.ZipTool
import ch.unil.pafanalysis.pdf.PdfService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString


@Service
class ZipService {

    @Autowired
    private var analysisRepo: AnalysisRepository? = null

    @Autowired
    private var analysisService: AnalysisService? = null

    @Autowired
    private var analysisStepService: AnalysisStepService? = null

    @Autowired
    private var pdfService: PdfService? = null

    @Autowired
    private var echartsServer: EchartsServer? = null

    @Autowired
    private var commonStep: CommonStep? = null

    @Autowired
    private var env: Environment? = null

    fun createZip(analysisId: Int, zipSelection: ZipDataSelection): File {
        val analysis = analysisRepo?.findById(analysisId)
        val steps = analysisService?.sortAnalysisSteps(analysis?.analysisSteps)

        val zipName = prettyName(analysis?.result?.name + if(analysis?.name != null) ("-"+analysis?.name) else "")
        val zipDir = createZipDir(zipName)

        createPdf(analysisId, zipSelection, "$zipDir/$zipName.pdf")
        createPlots(steps, zipSelection, zipDir)
        createTables(steps, zipSelection, zipDir)
        createSummary(steps, zipSelection, zipDir)

        return File(ZipTool().zipDir(zipDir, "$zipName.zip", createTempDirectory().pathString, true))
    }

    private fun prettyName(s: String): String {
        return s.replace("\\s+".toRegex(), "-").replace("--+".toRegex(), "-")
    }

    private fun createZipDir(dirName: String): String {
        val tmpDir =  createTempDirectory()
        val zipDir = tmpDir.pathString+"/"+dirName
        File(zipDir).mkdir()
        return zipDir
    }

    private fun createPdf(analysisId: Int, zipSelection: ZipDataSelection, pdfPath: String){
        val pdfFile = pdfService?.createPdf(analysisId, zipSelection)
        pdfFile?.copyTo(File(pdfPath))
        pdfFile?.delete()
    }

    private fun createSummary(steps: List<AnalysisStep>?, zipSelection: ZipDataSelection, zipDir: String) {
        val sep = "\t"

        File("$zipDir/summary.txt").printWriter().use { out ->
            // header
            val header = listOf("Id", "Step", "Table", "Plots").joinToString(separator = sep)
            out.println(header)

            steps?.forEachIndexed { i, step ->
                if (zipSelection?.steps != null && zipSelection.steps?.contains(step.id!!)) {
                    val idx = i + 1
                    val mainTable = if (zipSelection.mainTables?.contains(step.id) == true) listOf("tables/Table-$idx.txt") else emptyList()
                    val specialTable = if (zipSelection.specialTables?.contains(step.id) == true) listOf("tables/" + commonStep?.getRunner(step?.type)?.getOtherTableName(idx)) else emptyList()
                    val plots = if (zipSelection.plots?.contains(step.id) == true) {
                        val plotBase = "plots/$idx-${step.type}"
                        "$plotBase.png;$plotBase.svg"
                    } else ""
                    val row =
                        listOf(idx.toString(), StepNames.getName(step.type), mainTable.plus(specialTable).joinToString(separator = ";"), plots).joinToString(separator = sep)
                    out.println(row)
                }
            }
        }
    }

    private fun createTables(steps: List<AnalysisStep>?, zipSelection: ZipDataSelection, zipDir: String){
        val tableDir = "$zipDir/tables"
        File(tableDir).mkdir()

        steps?.forEachIndexed{ i, step ->
            val idx = i+1
            if(zipSelection.mainTables?.contains(step.id) == true) {
                val origTableFile = analysisStepService?.getTable(step.id!!)
                File(origTableFile).copyTo(File("$tableDir/Table-$idx.txt"))
            }
            if(zipSelection.specialTables?.contains(step.id) == true) {
                commonStep?.getRunner(step?.type)?.getOtherTable(step, tableDir, idx)
            }
        }
    }

    private fun createPlots(steps: List<AnalysisStep>?, zipSelection: ZipDataSelection, zipDir: String){
        val plotDir = "$zipDir/plots"
        File(plotDir).mkdir()

        steps?.forEachIndexed{ i, step ->
            if(zipSelection.plots?.contains(step.id) == true){
                getPlot(step, i+1, "$plotDir/")
            }
        }
    }

    private fun getPlot(step: AnalysisStep?, idx: Int, path: String){
        val fileName = "$idx-${step?.type}"
        val resultDir = env?.getProperty("output.path").plus(step?.resultPath)
        val resName = step?.id.toString()?.plus("-")?.plus(step?.type)

        echartsServer?.getSvgPlot(step, "${step?.resultPath}/$resName.svg")
        echartsServer?.getPngPlot(step, "${step?.resultPath}/$resName.png")

        // move generated files
        File("$resultDir/$resName.svg").copyTo(File("$path/$fileName.svg"))
        File("$resultDir/$resName.png").copyTo(File("$path/$fileName.png"))

        // delete original files
        File("$resultDir/$resName.svg").delete()
        File("$resultDir/$resName.png").delete()
    }

}

