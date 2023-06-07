package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.boxplot.BoxPlot
import com.google.gson.Gson
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.font.FontProvider
import com.itextpdf.svg.converter.SvgConverter
import com.itextpdf.svg.processors.ISvgConverterProperties
import com.itextpdf.svg.processors.impl.SvgConverterProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.awt.GraphicsEnvironment
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration


@Service
class EchartsServer {

    @Autowired
    private var env: Environment? = null

    val gson = Gson()

    fun makeEchartsPlot(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float): Image? {
        val results = gson.fromJson(step.results, BoxPlot::class.java)
        val echartsPlot = results.plot?.copy(outputPath = step.resultPath)

        val echartsServerUrl = env?.getProperty("echarts.server.url").plus("/pdf")

        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        val request = HttpRequest.newBuilder()
            .uri(URI.create(echartsServerUrl))
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(echartsPlot)))
            .build();
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if(response.statusCode() != HttpStatus.OK.value()) throw Exception("Could not generate eCharts pdf graph: " + response.body())

        val pdfPath: String = env?.getProperty("output.path") + response.body()
        val sourcePdf = PdfDocument(PdfReader(pdfPath))
        val pdfPlot = sourcePdf.getPage(1)
        val pdfPlotCopy: PdfFormXObject = pdfPlot.copyAsFormXObject(pdf)
        sourcePdf.close()
        val img = Image(pdfPlotCopy)
        return img.scaleToFit(plotWidth, 300f)
    }

    fun getSvgPlot(step: AnalysisStep?, svgPath: String): String? {
        val results = gson.fromJson(step?.results, BoxPlot::class.java)

        val echartsServerUrl = env?.getProperty("echarts.server.url").plus("/svg?path=$svgPath")
        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

        val request = HttpRequest.newBuilder()
            .uri(URI.create(echartsServerUrl))
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(results.plot)))
            .build();

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            throw Exception("Could not get the svg from service: Error code [${response.statusCode()}].")
        }

        return env?.getProperty("output.path") + response.body()
    }

    fun getPngPlot(step: AnalysisStep?, svgPath: String): String? {
        val results = gson.fromJson(step?.results, BoxPlot::class.java)

        val echartsServerUrl = env?.getProperty("echarts.server.url").plus("/png?path=$svgPath")
        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

        val request = HttpRequest.newBuilder()
            .uri(URI.create(echartsServerUrl))
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(results.plot)))
            .build();

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw Exception("Could not get the png from service: Error code [${response.statusCode()}].")
        }

        return env?.getProperty("output.path") + response.body()
    }

}