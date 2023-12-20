package ch.unil.pafanalysis.analysis.steps.one_d_enrichment

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ColType
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.annotations.model.AnnotationInfo
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

       val (newHeaderStrings, annoMapping) = parseProteinMapping(annotationPath, params?.categoryIds)

       val transposedMatrix = Array(params?.categoryIds?.size ?: 0) { Array(proteinAcList?.size ?: 0) { "" }}

       proteinAcList?.forEachIndexed{ i, resAcs ->
           val ac = resAcs.find{annoMapping.contains(it)}
           val myMatch: Pair<String?, List<String>?>? = annoMapping[ac]

           val newRows: List<String>? = if(myMatch?.first != null){
               annoMapping[myMatch?.first]?.second
           }else myMatch?.second

           newRows?.forEachIndexed { k, r -> transposedMatrix[k][i] = r }
       }

       val newCols = table?.cols?.plus(transposedMatrix.toList().map{it.toList()})
       val oldHeaderSize = table?.headers?.size
       val newHeaders: List<Header> = newHeaderStrings?.mapIndexed{ i, h -> Header(name = h, idx = (oldHeaderSize ?: 0).plus(i), type = ColType.CHARACTER)} ?: emptyList()
       val oldHeaders: List<Header> = table?.headers ?: emptyList()
       return Table(cols = newCols, headers = oldHeaders.plus(newHeaders))
   }

    // get a map with key => proteinAC and value => pair of either other proteinAC or list of annotations
    private fun parseProteinMapping(annotationFilePath: String?, selColIdxs: List<Int>?): Pair<List<String>?, Map<String, Pair<String?, List<String>?>>> {
        val sep = "\t"

        val reader = BufferedReader(FileReader(annotationFilePath))
        val headers: List<String> = reader.readLine().split(sep)
        val protIdx = headers.indexOf("UniProt")

        fun parseLine(line: String, acc: Map<String, Pair<String?, List<String>?>>, protIdx: Int): Map<String, Pair<String?, List<String>?>>{
            val cols = line.split(sep)
            val prots = cols[protIdx].split(";")
            val first = prots.first()

            val selCols = selColIdxs?.map{cols[it]}
            val newAcc = acc.plus(Pair(first, Pair(null, selCols)))

            return if (prots.size > 1) {
                val fromList = prots.takeLast(prots.size - 1)
                fromList.fold(acc) { innerAcc, from ->
                    newAcc.plus(Pair(from, Pair(first, null)))
                }
            } else newAcc
        }

        // first line can be the "Type" line from Perseus
        val initialLine = reader.readLine()
        val initialMap = if(initialLine.contains("Type")){
            emptyMap<String, Pair<String?, List<String>?>>()
        }else{
            parseLine(initialLine, emptyMap(), protIdx)
        }

        val annotationMapping =  reader.useLines {
            it.fold(initialMap) { acc, line ->
                parseLine(line, acc, protIdx)
            }
        }

        return Pair(selColIdxs?.map{headers[it]}, annotationMapping)
    }

}