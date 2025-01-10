package ch.unil.pafanalysis.html_plot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.boxplot.BoxPlot
import com.google.gson.Gson
import org.bouncycastle.asn1.x500.style.RFC4519Style.title
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.File


@Service
class HtmlPlot {

    @Autowired
    private var env: Environment? = null

    val gson = Gson()

    val toolboxSettings = """
        {toolbox: {
            show: true,
            feature: {
                dataZoom: {},
                saveAsImage: {}
            }
        },
    """.trimIndent()

    fun getHtmlPlot(step: AnalysisStep?, path: String, title: String){
        val template = loadTemplate()
        val options = getEchartsOptions(step)
        createHtmlPlot(step, options, template, path, title)
    }

    private fun loadTemplate(): String? {
        val templatePath = "/resources/html_plot/template.html"
        val serverFile = File(ClassPathResource(templatePath).path)
        val templateFile = if(serverFile.exists()) serverFile else File(ClassPathResource("/src/main$templatePath").path)
        return templateFile.readText()
    }

    private fun getEchartsOptions(step: AnalysisStep?): String? {
        val results = gson.fromJson(step?.results, BoxPlot::class.java)
        val options = replaceEmbeddedFunction(results.plot?.echartsOptions)
        return if(options != null) toolboxSettings + options.drop(1) else null
    }

    private fun getEchartsLibrary(): String {
        val templatePath = "/resources/html_plot/echarts_5.3.1.min.js"
        val serverFile = File(ClassPathResource(templatePath).path)
        val templateFile = if(serverFile.exists()) serverFile else File(ClassPathResource("/src/main$templatePath").path)
        return templateFile.readText()
    }

    private fun createHtmlPlot(step: AnalysisStep?, option: String?, template: String?, path: String, title: String){
        var html = if(option != null) {
            template?.replace("__OPTION__", option)
        } else {
            template?.replace("__OPTION__","{}")
            template?.replace("__ERROR__", "<p>there was an error<p>")
        }

        html = html?.replace("__TITLE__", title)

        html = html?.replace("__WIDTH__", "100%")?.replace("__HEIGHT__", "100vh")
        html = html?.replace("__ECHARTS_LIB__", getEchartsLibrary())

        // remove ERROR tag if not used
        html = if(html?.contains("__ERROR__") == true) {
            html.replace("__ERROR__", "")
        } else html

        html = addSpecificScripts(step, html)

        File(env?.getProperty("output.path") + path).writeText(html ?: "HTML template not found")

    }

    private fun addSpecificScripts(step: AnalysisStep?, html: String?): String? {
        return when (step?.type){
            AnalysisStepType.VOLCANO_PLOT.value -> HtmlVolcanoPlot.addScript(html)
            AnalysisStepType.BOXPLOT.value -> HtmlBoxPlot.addScript(html, step)
            AnalysisStepType.PCA.value -> HtmlPcaPlot.addScript(html)
            else -> html?.replace("__SCRIPT__", "")
        }
    }

    private fun replaceEmbeddedFunction(option: String?): String? {
        return option?.replace("\"/Function(", "")?.replace("\\n", "")?.replace("})/\"", "}")?.replace("\\", "")
    }


}