package ch.unil.pafanalysis.pdf

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepNames
import ch.unil.pafanalysis.results.model.Result
import ch.unil.pafanalysis.zip.ZipDataSelection
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfAnnotationBorder
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.action.PdfAction
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.DashedBorder
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.*
import com.itextpdf.layout.renderer.CellRenderer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@Service
class PdfService {

    @Autowired
    private var analysisRepo: AnalysisRepository? = null

    @Autowired
    private var analysisService: AnalysisService? = null

    @Autowired
    private var commonStep: CommonStep? = null

    @Autowired
    private var env: Environment? = null

    private val fontSizeConst = 8f
    val myFont = StandardFonts.HELVETICA
    val myBoldFont = StandardFonts.HELVETICA_BOLD
    val antCyan = DeviceRgb(242, 242, 242)
    private val lightCyan = DeviceRgb( 	251,	211,	121)

    fun createPdf(analysisId: Int, zipSelection: ZipDataSelection? = null): File {
        val analysis = analysisRepo?.findById(analysisId)
        val steps = analysisService?.sortAnalysisSteps(analysis?.analysisSteps)

        val filePath = kotlin.io.path.createTempFile(suffix = ".pdf").toFile()
        val pdf = PdfDocument(PdfWriter(filePath))

        val pageSize: PageSize = PageSize.A4
        val document: Document? = Document(pdf, pageSize, false)

        val plotWidth: Float = pageSize?.width?.minus(document?.rightMargin?: 0f)?.minus(document?.leftMargin?: 0f)

        addLogo(document, pdf, plotWidth)
        addResultInfo(analysis, document)

        addOverview(steps, document, plotWidth)
        addZipDescription(document)
        addConclusion(analysis, document)
        // make a page break
        document?.add(AreaBreak());

        addSteps(steps, document, pdf, plotWidth, zipSelection)
        addHeaderAndFooter(document, pdf, pageSize, plotWidth)

        document?.close()
        pdf.close()

        return filePath
    }

    private fun getParagraph(s: String, bold: Boolean = false, link: String? = null): Paragraph {
        val p = if(link != null){
            val link = Link(s, PdfAction.createGoTo(link))
            link.linkAnnotation.setBorder(PdfAnnotationBorder(0f, 0f, 0f))
            Paragraph(link)
        } else Paragraph(s)
        p.setFontSize(fontSizeConst)
        p.setFont(if(bold) PdfFontFactory.createFont(myBoldFont) else PdfFontFactory.createFont(myFont))
        return p
    }

    private fun addConclusion(analysis: Analysis?, document: Document?){
        if(analysis?.conclusion != null){
            val conclusionDiv = Div().setMarginTop(20f).setKeepTogether(true)

            val title = Paragraph("Conclusion").setFont(PdfFontFactory.createFont(myBoldFont))
                .setFontSize(14f)
                .setPaddingLeft(5f)
                .setFontColor(ColorConstants.BLACK)
            val conclusion = commentDiv(analysis.conclusion)
            conclusionDiv.add(title)
            conclusionDiv.add(conclusion)
            document?.add(conclusionDiv)
        }
    }

    private fun addZipDescription(document: Document?){
        val myDiv = Div()
            .setMarginTop(15f)
            .setWidth(280f)
            .setBackgroundColor(lightCyan)
            .setBorder(Border.NO_BORDER)
            .setPadding(2f)
            .setPaddingLeft(5f)
            .setBorderRadius(BorderRadius(2f))

        val list = com.itextpdf.layout.element.List()

        val item1 = ListItem()
        val item1Title = Text("plots: ").setFont(PdfFontFactory.createFont(myBoldFont)).setFontSize(fontSizeConst)
        val item1Text = Text("Contains all plots in PNG, SVG and ").setFont(PdfFontFactory.createFont(myFont)).setFontSize(fontSizeConst)
        val item1Text2 = Text("interactive HTML").setFont(PdfFontFactory.createFont(myBoldFont)).setFontSize(fontSizeConst)
        val item1Text3 = Text(" formats.").setFont(PdfFontFactory.createFont(myFont)).setFontSize(fontSizeConst)
        val item1Para = Paragraph().setMargin(0.0f)
        item1Para.add(item1Title)
        item1Para.add(item1Text)
        item1Para.add(item1Text2)
        item1Para.add(item1Text3)
        item1.add(item1Para)
        list.add(item1)

        val item2 = ListItem()
        val item2Title = Text("tables: ").setFont(PdfFontFactory.createFont(myBoldFont)).setFontSize(fontSizeConst)
        val item2Text = Text("Contains all tables in tab-separated format.").setFont(PdfFontFactory.createFont(myFont)).setFontSize(fontSizeConst)
        val item2Para = Paragraph().setMargin(0.0f)
        item2Para.add(item2Title)
        item2Para.add(item2Text)
        item2.add(item2Para)
        list.add(item2)

        val p1 = Paragraph().setMargin(0.0f)
        val t1 = Text("The ZIP file includes two folders:").setFont(PdfFontFactory.createFont(myFont)).setFontSize(fontSizeConst)
        p1.add(t1)
        myDiv.add(p1)
        val p2 = Paragraph()
        p2.add(list)
        myDiv.add(list)

        document?.add(myDiv)
    }

    private fun addOverview(steps: List<AnalysisStep>?, document: Document?, plotWidth: Float){
        val title = Paragraph("Overview of data analysis").setFont(PdfFontFactory.createFont(myBoldFont))
            .setFontSize(14f)
            .setPaddingLeft(5f)
            .setFontColor(ColorConstants.BLACK)

        document?.add(title)

        val table = Table(3).setWidth(plotWidth)

        fun createCell(name: String, bold: Boolean = false, link: String? = null, minWidth: Float? = null): Cell {
            val cell = Cell().setBorder(Border.NO_BORDER)
            if(minWidth != null) cell.setMinWidth(minWidth)
            val cellRenderer = CellRenderer(cell)
            cellRenderer.setProperty(Property.BORDER_BOTTOM, DashedBorder(ColorConstants.LIGHT_GRAY, 0.5f))
            cell.setNextRenderer(cellRenderer)
            cell.add(getParagraph(name, bold = bold, link = link))
            return cell
        }

        fun getTables(step: AnalysisStep?, stepNr: Int): String {
            return if(step?.modifiesResult == true){
                when(step?.type){
                    AnalysisStepType.ONE_D_ENRICHMENT.value -> "Table $stepNr, Enrichment table $stepNr"
                    else -> "Table $stepNr"
                }
            } else when(step?.type){
                AnalysisStepType.SUMMARY_STAT.value -> "Summary table $stepNr"
                AnalysisStepType.UMAP.value -> "UMAP plot $stepNr"
                AnalysisStepType.SCATTER_PLOT.value -> "Scatter plot $stepNr"
                AnalysisStepType.VOLCANO_PLOT.value -> "Volcano plot $stepNr"
                AnalysisStepType.PCA.value -> "PCA plot $stepNr"
                AnalysisStepType.BOXPLOT.value -> "Boxplot $stepNr"
                else -> ""
            }
        }

        steps?.forEachIndexed { i, step ->
            val stepNr = i + 1
            val link = "$stepNr-${step.type}"
            table.addCell(createCell(stepNr.toString(), link = link, minWidth = 30f))
            table.addCell(createCell(StepNames.getName(step.type), link = link, minWidth = 260f))
            table.addCell(createCell(getTables(step, stepNr), link = link))
        }

        val tableDiv = Div().setWidth(plotWidth-10f).setPaddingLeft(5f)
        tableDiv.add(table)
        document?.add(tableDiv)
    }

    private fun addResultInfo(analysis: Analysis?, document: Document?){
        val div = Div().setMarginTop(0f).setPaddingTop(0f)

        val result: Result? = analysis?.result

        val title = Paragraph(result?.name).setFont(PdfFontFactory.createFont(myBoldFont))
            .setFontSize(14f)
            .setBackgroundColor(antCyan)
            .setPaddingLeft(5f)
            .setFontColor(ColorConstants.BLACK)
        title.setMarginTop(0f)
        div.add(title)
        div.setBackgroundColor(antCyan)

        val infoTable = Table(2).setPaddingLeft(5f)

        addInfoCell("Description", result?.description ?: "", infoTable)

        val analysisName = if(analysis?.name != null) analysis.name else if(analysis?.idx != null) "#${analysis.idx + 1}" else ""
        addInfoCell("Analysis", analysisName, infoTable)

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        addInfoCell("Report creation date", LocalDateTime.now().format(formatter), infoTable)

        val (versionNr, _) = getBackendVersion()
        addInfoCell("Report created with", "TARAM $versionNr", infoTable)

        div.setBorder(SolidBorder(antCyan, 1f))
        //div.setMarginTop(10f)
        div.setMarginBottom(20f)
        div.add(infoTable)

        document?.add(div)
    }

    private fun getBackendVersion(): Pair<String, String> {
        val v = env?.getProperty("taram.version") ?: ":-/"
        val githubBase = "https://github.com/UNIL-PAF/taram-backend/releases/tag/"
        return Pair(v, githubBase + v)
    }

    private fun addInfoCell(name: String, cont: String, table: Table) {
        val cell1 = Cell().setBorder(Border.NO_BORDER)
        val p1 = Paragraph(name).setFont(PdfFontFactory.createFont(myBoldFont)).setFontSize(fontSizeConst)
        cell1.add(p1)
        val cell2 = Cell().setBorder(Border.NO_BORDER)
        val p2 = Paragraph(cont).setFont(PdfFontFactory.createFont(myFont)).setFontSize(fontSizeConst)
        cell2.add(p2)
        table.addCell(cell1)
        table.addCell(cell2)
    }

    private fun pdfToImage(imagePath: String, pdf: PdfDocument): Image {
        val unilLogoServer = File(ClassPathResource(imagePath).path)
        val unilLogo = if(unilLogoServer.exists()) unilLogoServer else File(ClassPathResource("/src/main$imagePath").path)
        val sourcePdf = PdfDocument(PdfReader(unilLogo))
        val pdfPlot = sourcePdf.getPage(1)
        val pdfPlotCopy: PdfFormXObject = pdfPlot.copyAsFormXObject(pdf)
        sourcePdf.close()
        return Image(pdfPlotCopy)
    }

    private fun addLogo(document: Document?, pdf: PdfDocument, plotWidth: Float){
        val imagePath = "/resources/images/personalized_logo_blue_cropped.pdf"
        val img1 = pdfToImage(imagePath, pdf).setBorder(Border.NO_BORDER)
        img1.setHorizontalAlignment(HorizontalAlignment.RIGHT).setBorder(Border.NO_BORDER)
        val table = Table(1)
        table.setWidth(plotWidth)
        val cell2 = Cell().add(img1).setBorder(Border.NO_BORDER)
        table.addCell(cell2)
        val p = Paragraph().add(table)
        document?.add(p)
    }

    private fun commentDiv(comment: String): Div {
        val p1 = Paragraph(comment).setFont(PdfFontFactory.createFont(myFont))
        p1.setBackgroundColor(lightCyan)
        p1.setFontSize(fontSizeConst)
        p1.setPadding(2f)
        p1.setPaddingLeft(5f)
        p1.setBorder(Border.NO_BORDER)
        p1.setBorderRadius(BorderRadius(2f))
        val div = Div()
        div.add(p1)
        return div
    }

    private fun addSteps(steps: List<AnalysisStep>?, document: Document?, pdf: PdfDocument, plotWidth: Float,  zipSelection: ZipDataSelection? = null){
        steps?.forEachIndexed { i, step ->
            val div = commonStep?.getRunner(step.type)?.createPdf(step, pdf, plotWidth, i + 1)
            if(step.comments != null) div?.add(commentDiv(step.comments))
            div?.setMarginBottom(30f)
            div?.isKeepTogether = true
            // only add the step if it is in the zipSelection
            if(zipSelection?.steps === null || zipSelection?.steps.contains(step.id!!)){
                document?.add(div)
            }
        }
    }

    private fun addHeaderAndFooter(document: Document?, pdf: PdfDocument, pageSize: PageSize, plotWidth: Float){
        val headerFontSize = 8f

        // paging position
        val xPosNum = plotWidth/2 + 30f
        val yPosNum = 60f

        // left header pos
        val xPosLeft = 78f

        val yPosTop = pageSize?.height.minus(8f)
        val leftHeader = Paragraph("PAF - UNIL").setFont(PdfFontFactory.createFont(myFont)).setFontSize(headerFontSize)

        // right header
        val xPosRight= pageSize?.width.minus(36f)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val rightHeader = Paragraph(LocalDateTime.now().format(formatter)).setFont(PdfFontFactory.createFont(myFont)).setFontSize(headerFontSize)

        val numberOfPages: Int = pdf.getNumberOfPages()
        for (i in 1..numberOfPages) {
            // Write aligned text to the specified by parameters point
            document?.showTextAligned(
                Paragraph(String.format("page %s of %s", i, numberOfPages)).setFontSize(headerFontSize),
                xPosNum, yPosNum, i, TextAlignment.CENTER, VerticalAlignment.TOP, 0f
            )

            document?.showTextAligned(
                leftHeader, xPosLeft, yPosTop, i, TextAlignment.RIGHT, VerticalAlignment.TOP, 0f
            )

            document?.showTextAligned(
                rightHeader, xPosRight, yPosTop, i, TextAlignment.RIGHT, VerticalAlignment.TOP, 0f
            )
        }
    }

}