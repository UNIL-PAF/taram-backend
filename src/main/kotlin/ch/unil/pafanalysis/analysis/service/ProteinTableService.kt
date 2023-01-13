package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ProteinGroup
import ch.unil.pafanalysis.analysis.model.ProteinTable
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProteinTableService {

    val readTable = ReadTableData()

    @Autowired
    private var commonStep: CommonStep? = null

    val headerMap = mapOf(
        "id" to "id",
        "prot" to "Majority.protein.IDs",
        "gene" to "Gene.names",
        "desc" to "Protein.names"
    )

    fun getProteinTable(step: AnalysisStep?, selProteins: List<String>?): ProteinTable {
        val table = readTable.getTable(commonStep?.getOutputRoot() + step?.resultTablePath, step?.commonResult?.headers)
        val defaultInt = step?.columnInfo?.columnMapping?.intCol
        val protTable = tableToProteinTable(table = table, resultType = step?.analysis?.result?.type, selProteins = selProteins, defaultInt = defaultInt)
        return protTable.copy(table = protTable.table?.sortedByDescending{ it.sel })
    }

    private fun tableToProteinTable(table: Table?, resultType: String?, selProteins: List<String>?, defaultInt: String?): ProteinTable {
        val ids = readTable.getDoubleColumn(table, headerMap["id"]!!)?.map { it.toInt() }
        val prots = readTable.getStringColumn(table, headerMap["prot"]!!)?.map { it.split(";")?.get(0) }
        val genes = readTable.getStringColumn(table, headerMap["gene"]!!)
        val descs = readTable.getStringColumn(table, headerMap["desc"]!!)
        val intCol = readTable.getDoubleColumn(table, defaultInt!!)
        val colOrMeans = intCol ?: readTable.getDoubleMatrixByRow(table, defaultInt!!).second.map{ it.average() }
        val sel = prots?.map{ selProteins?.contains(it) ?: false }

        val proteinRows: List<ProteinGroup>? = colOrMeans?.mapIndexed { i, colOrMean ->
            ProteinGroup(ids?.get(i) ?: i, prots?.get(i), genes?.get(i), descs?.get(i), colOrMean, sel?.get(i))
        }

        return ProteinTable(table = proteinRows, resultType = resultType, intField = defaultInt)
    }

}