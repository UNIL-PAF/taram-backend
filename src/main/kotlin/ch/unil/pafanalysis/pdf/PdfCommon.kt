package ch.unil.pafanalysis.pdf

import com.google.gson.Gson
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.*
import com.itextpdf.layout.renderer.CellRenderer


open class PdfCommon {

    val gson = Gson()

    private val fontSizeConst = 8f
    private val myFont = StandardFonts.HELVETICA
    private val myBoldFont = StandardFonts.HELVETICA_BOLD
    private val antCyan = DeviceRgb(242, 242, 242)
    private val lightCyan = DeviceRgb( 	251,	211,	121)
    val red = DeviceRgb(255, 77, 79)

    fun getTwoRowTableWithList(tableData: List<Pair<String, List<String>>>): Table {
        val table = Table(2)
        tableData.forEach{ (name, cont) ->
            val cell1 = Cell().add(getParagraph(name, bold = true, dense = true));
            cell1.setBorder(Border.NO_BORDER)
            cell1.setMarginTop(0f).setMarginBottom(0f).setPaddingTop(0f).setPaddingBottom(0f)
            table.addCell(cell1)
            val cell2Div = Div()
            cont.forEach{ cell2Div.add(getParagraph(it, dense = true).setVerticalAlignment(VerticalAlignment.TOP).setMarginTop(0.0f))}
            val cell2= Cell().add(cell2Div)
            cell2.setMarginTop(0f).setMarginBottom(0f).setPaddingTop(0f).setPaddingBottom(0f)
            cell2.setBorder(Border.NO_BORDER)
            table.addCell(cell2)
        }
        return table
    }

    fun getTwoRowTable(tableData: List<Pair<String, String>>, noBold: Boolean? = null, leftColMinWidth: Float? = null): Table {
        val table = Table(2)
        tableData.forEach{ (name, cont) ->
            val cell1 = Cell().add(getParagraph(name, bold = noBold != true, dense = true));
            if(leftColMinWidth != null) cell1.setMinWidth(leftColMinWidth)
            cell1.setBorder(Border.NO_BORDER)
            cell1.setMarginTop(0f).setMarginBottom(0f).setPaddingTop(0f).setPaddingBottom(0f)
            table.addCell(cell1)
            val cell2= Cell().add(getParagraph(cont, dense = true));
            cell2.setBorder(Border.NO_BORDER)
            cell2.setMarginTop(0f).setMarginBottom(0f).setPaddingTop(0f).setPaddingBottom(0f)
            table.addCell(cell2)
        }
        return table
    }

    fun getOneRowTable(tableData: List<Paragraph>): Table {
        val table = Table(1)
        tableData.forEach{ cont ->
            val cell1 = Cell().add(cont);
            cell1.setBorder(Border.NO_BORDER)
            cell1.setMarginTop(0f).setMarginBottom(0f).setPaddingTop(0f).setPaddingBottom(0f)
            table.addCell(cell1)
        }
        return table
    }

    fun descriptionPara(description: String): Paragraph {
        val p1 = Paragraph(description).setFont(PdfFontFactory.createFont(myFont))
        p1.setBackgroundColor(antCyan)
        p1.setFontSize(fontSizeConst)
        p1.setItalic()
        p1.setPaddingTop(5f)
        return p1
    }

    fun titleDiv(title: String, plotWidth: Float, description: String? = null, table: String? = null, nrProteins: Int? = null, extraParagraph: Paragraph? = null, link: String? = null ): Div {
        val titlePadding = 5f

        val p = Paragraph().setBackgroundColor(antCyan)
        p.setPaddingLeft(titlePadding)
        p.setPaddingTop(5f)

        val t = Table(2)
        t.setWidth(plotWidth?.minus(titlePadding))

        val text = Paragraph(Text(title))
        text.setBold()
        text.setFontSize(11f)
        text.setFontColor(ColorConstants.BLACK)
        text.setFont(PdfFontFactory.createFont(myFont))

        // add the link
        if(link != null){
            text.setProperty(Property.DESTINATION, link)
        }

        val colLeft = Cell()
        //colLeft.setWidth(170f)
        colLeft.setTextAlignment(TextAlignment.LEFT)
        colLeft.setBorder(Border.NO_BORDER)
        colLeft.add(text)

        if(description != null){
            colLeft.add(descriptionPara(description))
        }

        t.addCell(colLeft)

        val colRight = Cell()
        colRight.setBorder(Border.NO_BORDER)
        colRight.setWidth(plotWidth/5)
        colRight.setTextAlignment(TextAlignment.LEFT)

        if(table != null){
            colRight.add(getTableParagraph(table).setHorizontalAlignment(HorizontalAlignment.RIGHT).setMarginRight(5f).setMarginTop(3f))
        }

        if(extraParagraph != null){
            colRight.add(extraParagraph.setHorizontalAlignment(HorizontalAlignment.RIGHT).setMarginRight(5f).setMarginTop(5f))
        }

        if(nrProteins != null){
            colRight.add(getParagraph("$nrProteins protein groups", bold = true).setTextAlignment(TextAlignment.RIGHT).setMarginRight(5f).setMarginTop(5f))
        }

        t.addCell(colRight)

        p.add(t)

        val div = Div()
        div.add(p)
        return div
    }

    fun getTableParagraph(s: String): Paragraph {
        return getParagraph(s, bold = true, underline = false)
            .setWidth(40f)
            .setBackgroundColor(lightCyan)
            .setBorder(Border.NO_BORDER)
            .setPadding(2f)
            .setPaddingLeft(5f)
            .setBorderRadius(BorderRadius(2f))
    }

    fun getParagraph(s: String, bold: Boolean = false, underline: Boolean = false, dense: Boolean = false): Paragraph {
        val t = Text(s)
        val p = Paragraph(t)
        p.setFontSize(fontSizeConst)
        p.setFont(if(bold) PdfFontFactory.createFont(myBoldFont) else PdfFontFactory.createFont(myFont))
        if(underline) p.setUnderline()
        if(dense) p.setMargin(0f).setPadding(0f)
        return p
    }

    fun getParamsCell(content: Div, width: Float, addTitle: Boolean = true, rightBorder: Boolean = true): Cell {
        val div = Div()
        div.setPaddingLeft(5f)
        val title = getParagraph("Parameters:", bold = true, underline = false, dense = true)
        if(addTitle) div.add(title)
        div.add(content)

        val paramsCell = Cell().add(div)
        paramsCell.setWidth(width)
        paramsCell.setBorder(Border.NO_BORDER)
        return paramsCell
    }

    fun getDataCell(div: Div, width: Float): Cell {
        val cell = Cell()
        cell.add(div)
        cell.setWidth(width)
        cell.setBorder(Border.NO_BORDER)
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