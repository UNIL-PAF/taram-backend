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
import com.itextpdf.layout.properties.TextAlignment
import java.util.*


open class PdfCommon {

    val gson = Gson()

    private val fontSizeConst = 10f
    private val myGrayConst: Color = WebColors.getRGBColor("WhiteSmoke")

    fun getPlotWidth(pageSize: PageSize?, document: Document?): Float {
        return pageSize?.width?.minus(document?.rightMargin?: 0f)?.minus(document?.leftMargin?: 0f) ?: 10f
    }

    fun addTwoRowTable(tableData: List<Pair<String, Paragraph?>>): Div {
        val div = Div()

        val table = Table(2)
        tableData.map{ (name, cont) ->
            val cell1 = Cell().add(Paragraph(name).setBold().setFontSize(fontSizeConst-1f));
            cell1.setBorder(Border.NO_BORDER)
            table.addCell(cell1)
            val cell2= Cell().add(cont?.setFontSize(fontSizeConst));
            cell2.setBorder(Border.NO_BORDER)
            table.addCell(cell2)
        }

        div.add(table)
        return div
    }

    fun titleDiv(title: String, nrProts: Int?, tableNr: Int?, plotWidth: Float? = 500f): Div {
        val titlePadding = 5f

        val p = Paragraph().setBackgroundColor(myGrayConst)
        p.setPaddingLeft(titlePadding)

        val t = Table(3)
        t.setWidth(plotWidth?.minus(titlePadding) ?: 500f)

        val text = Paragraph(Text(title))
        text.setBold()
        text.setFontSize(12f)
        val colLeft = Cell()
        colLeft.setWidth(170f)
        colLeft.setTextAlignment(TextAlignment.LEFT)
        colLeft.setBorder(Border.NO_BORDER)
        colLeft.add(text)
        t.addCell(colLeft)

        val colCenter = Cell()
        colCenter .setWidth(170f)
        colCenter.setTextAlignment(TextAlignment.CENTER)
        colCenter.setBorder(Border.NO_BORDER)
        val nrProtsText = Text("$nrProts protein groups")
        nrProtsText.setFontSize(fontSizeConst)
        colCenter.add(Paragraph(nrProtsText))
        t.addCell(colCenter)

        val colRight = Cell().setTextAlignment(TextAlignment.RIGHT)
        colRight.setTextAlignment(TextAlignment.RIGHT)
        colRight.setBorder(Border.NO_BORDER)
        colRight.setPaddingRight(titlePadding)

        if(tableNr != null) colRight.add(Paragraph(Text("Table-$tableNr").setItalic()))
        t.addCell(colRight)

        p.add(t)

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
        val line = SolidLine(2f)
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