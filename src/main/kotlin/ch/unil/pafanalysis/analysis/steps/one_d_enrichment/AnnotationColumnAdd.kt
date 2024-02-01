package ch.unil.pafanalysis.analysis.steps.one_d_enrichment

import ch.unil.pafanalysis.analysis.model.ColType
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.HeaderTypeMapping
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import java.io.BufferedReader
import java.io.FileReader

object AnnotationColumnAdd {

    private val readTableData = ReadTableData()
    private val headerTypeMapping = HeaderTypeMapping()

   fun addAnnotations(table: Table?, resType: String?, annotationPath: String?, params: OneDEnrichmentParams): Table?{
       // prepare result data
       val proteinAcList: List<List<String>>? =
           readTableData.getStringColumn(table, headerTypeMapping.getCol("proteinIds", resType))
               ?.map { it.split(";") }

       val proteinAcMap: Map<String, Pair<Int, Int>>? = proteinAcList?.foldIndexed(emptyMap()){ i, acc, list ->
           list.foldIndexed(acc){k, acc2, ac -> acc2.plus(Pair(ac, Pair(i, k)))}
       }

       val newCols = parseProteinMapping(annotationPath, params?.categoryIds, proteinAcMap, proteinAcList?.size)
       val newHeaders: List<Header> = getNewHeaders(annotationPath, params?.categoryIds, table?.headers?.size) ?: emptyList()
       val oldHeaders: List<Header> = table?.headers ?: emptyList()
       return Table(cols = table?.cols?.plus(newCols ?: emptyList()), headers = oldHeaders.plus(newHeaders))
   }

    private fun getNewHeaders(annotationFilePath: String?, selColIdxs: List<Int>?, nextHeaderIdx: Int?): List<Header>? {
        val sep = "\t"

        val reader = BufferedReader(FileReader(annotationFilePath))
        val headers: List<String> = reader.readLine().split(sep)
        val selHeaders = selColIdxs?.map{ idx -> headers[idx]}
        reader.close()
        return selHeaders?.mapIndexed{ i, h -> Header(name = h, idx = nextHeaderIdx?.plus(i) ?: throw StepException("idx is missing."), type = ColType.CHARACTER)}
    }

    private fun parseLine(line: String, matrix: Array<Array<Pair<String, Int?>>>, proteinAcMap: Map<String, Pair<Int, Int>>?, selColIdxs: List<Int>?, protIdx: Int){
        val sep = "\t"

        val cols = line.split(sep)
        val selCols = selColIdxs?.map{ idx -> cols[idx]}
        val protAcs = cols[protIdx].split(";")

        protAcs.forEach{ protAc ->
            val lineIdx = proteinAcMap?.get(protAc)
            if(lineIdx != null){
                selCols?.forEachIndexed{ i, col ->
                    val alreadyRank = matrix[i][lineIdx.first].second
                    if(alreadyRank == null || alreadyRank > lineIdx.second) {
                        matrix[i][lineIdx.first] = Pair(col, lineIdx.second)
                    }
                }
            }
        }
    }

    private fun parseProteinMapping(annotationFilePath: String?, selColIdxs: List<Int>?, proteinAcMap: Map<String, Pair<Int, Int>>?, nrRows: Int?): List<List<String>>? {
        val sep = "\t"

        val reader = BufferedReader(FileReader(annotationFilePath))
        val headers: List<String> = reader.readLine().split(sep)
        val protIdx = headers.indexOf("UniProt")

        val matrix = Array(selColIdxs?.size ?: 0) { Array<Pair<String, Int?>>(nrRows ?: 0) { Pair("", null) }}

        // first line can be the "Type" line from Perseus
        var line = reader.readLine()
        if(!line.contains("Type")){
            line = reader.readLine()
        }

        while (line != null) {
            parseLine(line, matrix, proteinAcMap, selColIdxs, protIdx)
            line = reader.readLine()
        }

        reader.close()

        return matrix.map{col -> col.map{it.first}.toList()}.toList()
    }

}