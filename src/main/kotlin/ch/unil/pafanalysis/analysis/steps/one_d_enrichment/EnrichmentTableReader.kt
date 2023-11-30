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
                size = cols[3].toInt(),
                score = cols[4].toDouble(),
                pValue = cols[5].toDouble(),
                qValue = cols[6].toDouble(),
                mean = cols[7].toDouble(),
                median = cols[8].toDouble(),
            )
        }

        val sortedRows = rows.sortedBy { r -> r.pValue }.mapIndexed{i, a -> a.copy(id = i)}
        return FullEnrichmentTable(rows = sortedRows)
    }

}