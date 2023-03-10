package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ProteinGroup
import ch.unil.pafanalysis.analysis.model.ProteinTable
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.results.model.ResultType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProteinTableService {

    val readTable = ReadTableData()

    @Autowired
    private var commonStep: CommonStep? = null

    val headerMapMQ = mapOf(
        "id" to "id",
        "prot" to "Majority.protein.IDs",
        "gene" to "Gene.names",
        "desc" to "Protein.names"
    )

    val headerMapSN = mapOf(
        "id" to "PG.ProteinGroups",
        "prot" to "PG.ProteinGroups",
        "gene" to "PG.Genes",
        "desc" to "PG.FASTAHeader"
    )


    fun getProteinTable(step: AnalysisStep?, selProteins: List<String>?, resType: String?): ProteinTable {
        val table = readTable.getTable(commonStep?.getOutputRoot() + step?.resultTablePath, step?.commonResult?.headers)
        val defaultInt = step?.columnInfo?.columnMapping?.intCol
        val protTable = tableToProteinTable(table = table, resultType = step?.analysis?.result?.type, selProteins = selProteins, defaultInt = defaultInt, resType = resType)
        return protTable.copy(table = protTable.table?.sortedByDescending{ it.sel })
    }

    private fun tableToProteinTable(table: Table?, resultType: String?, selProteins: List<String>?, defaultInt: String?, resType: String?): ProteinTable {
        val headerMap = if(resType == ResultType.MaxQuant.value) headerMapMQ else headerMapSN

        val prots = readTable.getStringColumn(table, headerMap["prot"]!!)?.map { it.split(";")?.get(0) }
        val genes = readTable.getStringColumn(table, headerMap["gene"]!!)?.map { it.split(";")?.get(0) }
        val descs = readTable.getStringColumn(table, headerMap["desc"]!!)
        val intCol = readTable.getDoubleColumn(table, defaultInt!!)
        val ids: List<Int>? = if(resType == ResultType.MaxQuant.value){
            readTable.getDoubleColumn(table, headerMap["id"]!!)?.map { it.toInt() }
        } else listOf(1..(prots?.size ?: 1)).flatten()
        val colOrMeans = intCol ?: readTable.getDoubleMatrixByRow(table, defaultInt!!).second.map{ it.average() }
        val fltInt = colOrMeans.filter{ !it.isNaN() }
        val sel = prots?.map{ selProteins?.contains(it) ?: false }

        val proteinRows: List<ProteinGroup>? = fltInt?.mapIndexed { i, colOrMean ->
            ProteinGroup(ids?.get(i) ?: i, prots?.get(i), genes?.get(i), descs?.get(i), colOrMean, sel?.get(i))
        }

        return ProteinTable(table = proteinRows, resultType = resultType, intField = defaultInt)
    }

}