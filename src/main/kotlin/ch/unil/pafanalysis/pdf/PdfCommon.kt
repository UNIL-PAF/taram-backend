package ch.unil.pafanalysis.pdf

import com.google.gson.Gson
import com.itextpdf.kernel.colors.Color
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.WebColors
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.TextAlignment


open class PdfCommon {

    val gson = Gson()

    val fontSizeConst = 10f
    val myGrayConst: Color = WebColors.getRGBColor("WhiteSmoke")

    fun addTwoRowTable(tableData: List<Pair<String, Paragraph?>>): Div {
        val div = Div()

        val table = Table(2)
        tableData.map{ (name, cont) ->
            val cell1 = Cell().add(Paragraph(name).setBold().setFontSize(fontSizeConst));
            cell1.setBorder(Border.NO_BORDER)
            table.addCell(cell1)
            val cell2= Cell().add(cont?.setFontSize(fontSizeConst));
            cell2.setBorder(Border.NO_BORDER)
            table.addCell(cell2)
        }

        div.add(table)
        return div
    }

    fun titleDiv(title: String, nrProts: Int?, tableNr: Int?, plotWidth: Float): Div {
        val titlePadding = 5f

        val p = Paragraph().setBackgroundColor(myGrayConst)
        p.setPaddingLeft(titlePadding)

        val t = Table(3)
        t.setWidth(plotWidth?.minus(titlePadding))

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
        if(tableNr != null) colRight.add(Paragraph(Text("Table-$tableNr").setFontSize(fontSizeConst)))
        t.addCell(colRight)

        p.add(t)

        val div = Div()
        div.add(p)
        return div
    }

    fun commentDiv(comment: String): Div {
        val p1 = Paragraph(comment)
        p1.setBackgroundColor(ColorConstants.YELLOW)
        p1.setFontSize(fontSizeConst)
        val div = Div()
        div.add(p1)
        return div
    }

    fun horizontalLineDiv(plotWidth: Float): Div {
        val line = SolidLine(2f)
        line.setColor(ColorConstants.LIGHT_GRAY)
        val ls = LineSeparator(line)
        ls.setWidth(plotWidth)
        ls.setMarginTop(5f)
        val div = Div()
        div.add(ls)
        return div
    }

    fun parametersDiv(parameters: List<Paragraph>): Div {
        val div = Div()
        div.setPaddingLeft(5f)
        //div.setBackgroundColor(myGrayConst)
        val title = Paragraph("Parameters:").setFontSize(fontSizeConst).setBold()
        div.add(title)

        parameters.forEach{ param ->
            param.setFontSize(fontSizeConst)
            div.add(param)
        }

        return div
    }

}