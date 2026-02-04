package ch.unil.pafanalysis.analysis.steps.initial_result

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.results.model.ResultType
import com.github.rcaller.rstuff.RCaller
import com.github.rcaller.rstuff.RCallerOptions
import com.github.rcaller.rstuff.RCode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.io.File

@Service
class PtxQcComputation {

    @Autowired
    private var env: Environment? = null

    fun run(step: AnalysisStep?): String? {
        val isMaxQuant = step?.analysis?.result?.type == ResultType.MaxQuant.value
        val resultPathName = if(isMaxQuant) "result.path.maxquant" else throw StepException("PTXQC only works on MaxQuant data")
        val txtPath = env?.getProperty(resultPathName).plus(step?.analysis?.result?.path)
        val outName = step?.resultPath + "/PTXQC_report"

        return try{
            rComputePtxQc(txtPath, outName)
        }catch (e: Exception) {
            e.printStackTrace()
            throw StepException("Failed to run PTXQC")
        }
    }

    private fun rComputePtxQc(txtPath: String, outName: String): String? {

        val code = RCode.create()
        code.R_require("PTXQC")
        code.R_require("methods")
        code.addString("txt_path", txtPath)
        code.addString("out_name", env?.getProperty("output.path") + outName)

        code.addRCode("""            
            fh <- getReportFilenames(txt_path)
            fh_2 <- lapply(fh, function(x) sub(".*\\.(\\w+)${'$'}", paste0(out_name, ".\\1"), x) )
            fh_2${'$'}report_file_prefix <- out_name
            
            createReport(
              txt_folder        = txt_path,
              report_filenames  = fh_2
            )            
            capture.output(print(rmarkdown::find_pandoc()), file="/tmp/a.txt")
            """.trimIndent())


        val caller = RCaller.create(code, RCallerOptions.create())
        caller.runOnly()

        val htmlFullPath = env?.getProperty("output.path") + "$outName.html"
        return if(File(htmlFullPath).exists()) "$outName.html" else null
    }
}