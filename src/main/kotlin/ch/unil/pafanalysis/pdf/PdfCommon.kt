package ch.unil.pafanalysis.pdf

import com.google.gson.Gson
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.Color
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.colors.WebColors
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.Property
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.renderer.CellRenderer


open class PdfCommon {

    val gson = Gson()

    val fontSizeConst = 10f
    val myFont = StandardFonts.HELVETICA
    val myBoldFont = StandardFonts.HELVETICA_BOLD
    val myGrayConst: Color = WebColors.getRGBColor("WhiteSmoke")
    val antCyan = DeviceRgb(181, 245, 236)

    fun getTwoRowTableWithList(tableData: List<Pair<String, List<String>>>): Table {
        val table = Table(2)
        tableData.forEach{ (name, cont) ->
            val cell1 = Cell().add(getParagraph(name, bold = true));
            cell1.setBorder(Border.NO_BORDER)
            table.addCell(cell1)
            val cell2Div = Div()
            cont.forEach{ cell2Div.add(getParagraph(it))}
            val cell2= Cell().add(cell2Div);
            cell2.setBorder(Border.NO_BORDER)
            table.addCell(cell2)
        }
        return table
    }

    fun getTwoRowTable(tableData: List<Pair<String, String>>): Table {
        val table = Table(2)
        tableData.forEach{ (name, cont) ->
            val cell1 = Cell().add(getParagraph(name, bold = true));
            cell1.setBorder(Border.NO_BORDER)
            table.addCell(cell1)
            val cell2= Cell().add(getParagraph(cont));
            cell2.setBorder(Border.NO_BORDER)
            table.addCell(cell2)
        }
        return table
    }

    fun titleDiv(title: String, plotWidth: Float): Div {
        val titlePadding = 5f

        val p = Paragraph().setBackgroundColor(antCyan)
        p.setPaddingLeft(titlePadding)
        p.setPaddingTop(5f)

        val t = Table(1)
        t.setWidth(plotWidth?.minus(titlePadding))

        val text = Paragraph(Text(title))
        text.setFontSize(12f)
        text.setFontColor(ColorConstants.BLACK)
        text.setFont(PdfFontFactory.createFont(myFont))
        val colLeft = Cell()
        colLeft.setWidth(170f)
        colLeft.setTextAlignment(TextAlignment.LEFT)
        colLeft.setBorder(Border.NO_BORDER)
        colLeft.add(text)
        t.addCell(colLeft)
        p.add(t)

        val div = Div()
        div.add(p)
        return div
    }

    fun getParagraph(s: String, bold: Boolean = false, underline: Boolean = false): Paragraph {
        val p = Paragraph(s)
        p.setFontSize(fontSizeConst)
        p.setFont(if(bold) PdfFontFactory.createFont(myBoldFont) else PdfFontFactory.createFont(myFont))
        if(underline) p.setUnderline()
        return p
    }

    fun getParamsCell(content: Div, width: Float, addTitle: Boolean = true): Cell {
        val div = Div()
        div.setPaddingLeft(5f)
        val title = getParagraph("Parameters:", bold = true, underline = true)
        if(addTitle) div.add(title)
        div.add(content)

        val paramsCell = Cell().add(div)
        paramsCell.setWidth(width)
        paramsCell.setBorder(Border.NO_BORDER)
        val cellRenderer = CellRenderer(paramsCell)
        cellRenderer.setProperty(Property.BORDER_RIGHT, SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
        paramsCell.setNextRenderer(cellRenderer)

        return paramsCell
    }

    fun getDataCell(div: Div, width: Float): Cell {
        val cell = Cell()
        cell.add(div)
        cell.setWidth(width)
        cell.setBorder(Border.NO_BORDER)
        val cellRenderer = CellRenderer(cell)
        cellRenderer.setProperty(Property.BORDER_RIGHT, SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
        cell.setNextRenderer(cellRenderer)
        cell.setPaddingLeft(5f)
        return cell
    }

    fun getResultCell(div: Div, width: Float): Cell {
        val cell = Cell()
        cell.add(div)
        cell.setWidth(width)
        cell.setBorder(Border.NO_BORDER)
        cell.setPaddingLeft(5f)
        return cell
    }

    fun getText(s: String, bold: Boolean = false, italic: Boolean = false): Text {
        val text = Text(s)
        text.setFontSize(fontSizeConst)
        text.setFont(PdfFontFactory.createFont(myFont))
        if(bold) text.setBold()
        if(italic) text.setItalic()
        return text
    }

}