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
        "desc" to "Protein.names",
        "int" to "Intensity",
        "ibaq" to "iBAQ"
    )

    fun getProteinTable(step: AnalysisStep?): ProteinTable {
        val table = readTable.getTable(commonStep?.getOutputRoot() + step?.resultTablePath, step?.commonResult?.headers)
        return tableToProteinTable(table, step?.analysis?.result?.type)
    }

    private fun tableToProteinTable(table: Table?, resultType: String?): ProteinTable {
        val ids = readTable.getDoubleColumn(table, headerMap["id"]!!)?.map { it.toInt() }
        val prots = readTable.getStringColumn(table, headerMap["prot"]!!)
        val genes = readTable.getStringColumn(table, headerMap["gene"]!!)
        val descs = readTable.getStringColumn(table, headerMap["desc"]!!)
        val int = readTable.getDoubleColumn(table, headerMap["int"]!!)
        val ibaq = readTable.getDoubleColumn(table, headerMap["ibaq"]!!)

        val proteinRows: List<ProteinGroup>? = ids?.mapIndexed{ i, id ->
            ProteinGroup(id, prots?.get(i), genes?.get(i), descs?.get(i), int?.get(i), ibaq?.get(i))
        }

        return ProteinTable(table = proteinRows, resultType = resultType)
    }

}