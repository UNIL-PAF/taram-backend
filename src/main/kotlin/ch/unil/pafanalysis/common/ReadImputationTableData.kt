package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.model.Header
import java.io.BufferedReader
import java.io.FileReader

class ReadImputationTableData {

    fun getTable(fileName: String?, allHeaders: List<Header>?): Table {
        val reader = BufferedReader(FileReader(fileName))
        val sep = "\t"

        val headerIdxs: List<Int> = reader.readLine().split(sep).map{it.toInt()}
        val headers: List<Header>? = headerIdxs.map{ idx -> allHeaders?.find{it.idx == idx}!!}

        val rows: List<List<Boolean>> = reader.readLines().map{ line ->
            line.split(sep).map{ it == "1" }
        }

        return Table(headers, rows)
    }
}