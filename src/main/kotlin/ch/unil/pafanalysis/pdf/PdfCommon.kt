package ch.unil.pafanalysis.pdf

import com.google.gson.Gson
import com.itextpdf.kernel.colors.Color
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.WebColors
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TabAlignment
import java.util.*


open class PdfCommon {

    val gson = Gson()

    private val fontSizeConst = 10f
    private val myGrayConst: Color = WebColors.getRGBColor("WhiteSmoke")

    fun getPlotWidth(pageSize: PageSize?, document: Document?): Float {
        return pageSize?.width?.minus(document?.rightMargin?: 0f)?.minus(document?.leftMargin?: 0f) ?: 10f
    }

    fun addTable(tableData: SortedMap<String, String?>): Div {

        val div = Div()

        val table = Table(2)
        tableData.map{ (name, cont) ->
            val cell1 = Cell().add(Paragraph(name).setBold().setFontSize(fontSizeConst-1f));
            cell1.setBorder(Border.NO_BORDER)
            table.addCell(cell1)
            val cell2= Cell().add(Paragraph(cont).setFontSize(fontSizeConst));
            cell2.setBorder(Border.NO_BORDER)
            table.addCell(cell2)

        }

        div.add(table)
        return div
    }

    fun titleDiv(title: String, nrProts: Int?, plotWidth: Float? = 500f): Div {
        val paddingLeft = 10f

        val p = Paragraph().setBackgroundColor(myGrayConst)
        p.setPaddingLeft(paddingLeft)

        val text = Text("Step $title")
        text.setBold()
        text.setFontSize(12f)
        p.add(text)

        p.add(Tab())
        p.addTabStops(TabStop(plotWidth?.minus(paddingLeft) ?: 500f, TabAlignment.RIGHT))

        val nrProtsText = Text("$nrProts protein groups")
        nrProtsText.setFontSize(fontSizeConst)
        nrProtsText.setHorizontalAlignment(HorizontalAlignment.RIGHT)
        p.add(nrProtsText)

        val div = Div()
        div.add(p)
        return div
    }

    fun addTabbedText(text1: String, text2: String?): Div {
        val p = Paragraph()
        p.add(Text(text1).setBold())
        p.add(Tab())
        p.add(Text(text2))
        val div = Div()
        div.add(p)

        return div
    }

    fun addTabbedTextList(text1: String, text2: List<String?>): Div {
        val p = Paragraph()
        p.add(Text(text1).setBold())
        text2?.forEach {
            p.add(Text("\n"))
            p.add(Tab())
            p.add(Text(it))
        }

        val div = Div()
        div.add(p)

        return div
    }


    fun commentDiv(comment: String): Div {
        val p1 = Paragraph(comment)
        p1.setBackgroundColor(ColorConstants.YELLOW)
        val div = Div()
        div.add(p1)
        return div
    }

    fun horizontalLineDiv(): Div {
        val line = SolidLine(1f)
        line.setColor(ColorConstants.LIGHT_GRAY)
        val ls = LineSeparator(line)
        ls.setWidth(520f)
        ls.setMarginTop(5f)
        val div = Div()
        div.add(ls)
        return div
    }

    fun parametersDiv(parameters: List<String>): Div {
        val div = Div()
        div.setBackgroundColor(myGrayConst)
        val title = Paragraph("Parameters:")
        val p = Paragraph()
        div.add(title)

        parameters.forEach{ param ->
            val text = Text(param)
            text.setItalic()
            text.setFontSize(10f)
            p.add(text)
            p.add("\n")
        }

        div.add(p)
        return div
    }

}