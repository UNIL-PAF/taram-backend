package ch.unil.pafanalysis.analysis.steps.one_d_enrichment

import java.io.BufferedReader
import java.io.FileReader

class EnrichmentTableReader {

    fun readTable(filePath: String): FullEnrichmentTable{

        val reader = BufferedReader(FileReader(filePath))
        val headers = reader.readLine().split("\t")

        val rows = reader.readLines().map{ l ->
            val cols = l.split("\t")
            EnrichmentRow(
                id = null,
                column = cols[0],
                type = cols[1],
                name = cols[2],
                size = cols[3].toIntOrNull(),
                score = cols[4].toDoubleOrNull(),
                pvalue = cols[5].toDoubleOrNull(),
                qvalue = cols[6].toDoubleOrNull(),
                mean = cols[7].toDoubleOrNull(),
                median = cols[8].toDoubleOrNull(),
            )
        }

        val sortedRows = rows.sortedByDescending { r -> r.median }.mapIndexed{i, a -> a.copy(id = i)}
        return FullEnrichmentTable(rows = sortedRows)
    }

}