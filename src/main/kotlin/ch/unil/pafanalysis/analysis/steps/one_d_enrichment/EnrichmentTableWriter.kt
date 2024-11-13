package ch.unil.pafanalysis.analysis.steps.one_d_enrichment

import java.io.BufferedWriter
import java.io.FileWriter

class EnrichmentTableWriter {

    private val headers =
        listOf("Column", "Type", "Name", "Size", "Score", "P-value", "Adj. p-value", "Mean", "Median")

    fun write(filePath: String, rows: List<EnrichmentRow>): String {
        val writer = BufferedWriter(FileWriter(filePath))
        val sep = "\t"
        writer.write(headers.joinToString(separator = sep))
        writer.newLine()
        for (row in rows) {
            writer.write(row.column + sep)
            writer.write(row.type + sep)
            writer.write(row.name + sep)
            writer.write(row.size.toString() + sep)
            writer.write(row.score.toString() + sep)
            writer.write(row.pvalue.toString() + sep)
            writer.write(row.qvalue.toString() + sep)
            writer.write(row.mean.toString() + sep)
            writer.write(row.median.toString())
            writer.newLine()
        }
        writer.close()
        return filePath
    }

}