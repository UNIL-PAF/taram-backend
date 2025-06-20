package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.model.ProteinGroup
import ch.unil.pafanalysis.analysis.model.ProteinTable
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.HeaderTypeMapping
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.results.model.ResultType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProteinTableService {

    private val readTable = ReadTableData()
    private val hMap = HeaderTypeMapping()

    @Autowired
    private var commonStep: CommonStep? = null

    fun getProteinTable(step: AnalysisStep?, selProteins: List<String>?, resType: String?): ProteinTable {
        val table = readTable.getTable(commonStep?.getOutputRoot() + step?.resultTablePath, step?.commonResult?.headers)
        val defaultInt = step?.columnInfo?.columnMapping?.intCol
        val protTable = tableToProteinTable(
            table = table,
            resultType = step?.analysis?.result?.type,
            selProteins = selProteins,
            defaultInt = defaultInt,
            resType = resType,
            step?.columnInfo?.columnMapping?.experimentDetails
        )
        return protTable.copy(table = protTable.table?.sortedByDescending { it.sel })
    }

    private fun tableToProteinTable(
        table: Table?,
        resultType: String?,
        selProteins: List<String>?,
        defaultInt: String?,
        resType: String?,
        expDetails: Map<String, ExpInfo>?
    ): ProteinTable {
        val protTable = readTable.getStringColumn(table, hMap.getCol("proteinIds", resType))
        val prots = protTable?.map { it.split(";")[0] }
        val allProts = protTable
        val genes = readTable.getStringColumn(table, hMap.getCol("geneNames", resType))//?.map { it.split(";")?.get(0) }
        val descs = readTable.getStringColumn(table, hMap.getCol("description", resType))
        val intCol = readTable.getDoubleColumn(table, defaultInt!!)
        val ids: List<Int>? = if (resType == ResultType.MaxQuant.value) {
            readTable.getDoubleColumn(table, hMap.getCol("id", resType))?.map { it.toInt() }
        } else listOf(1..(prots?.size ?: 1)).flatten()
        val colOrMeans: List<Double> =
            intCol ?: readTable.getDoubleMatrixByRow(table, defaultInt!!, expDetails).second.map { d ->
                val flt = d.filter { !it.isNaN() }
                if (flt.isNotEmpty()) flt.average()
                else Double.NaN
            }

        val sel = prots?.zip(genes ?: prots)?.map { selProteins?.contains(it.first)?:false ||  selProteins?.contains(it.second)?: false}

        val proteinRows: List<ProteinGroup>? = colOrMeans?.mapIndexed { i, colOrMean ->
            ProteinGroup(ids?.get(i) ?: i, prots?.get(i), allProts?.get(i), genes?.get(i), descs?.get(i), colOrMean, sel?.get(i))
        }

        val fltRows = proteinRows?.filter { it.int?.isNaN() != true }

        return ProteinTable(table = fltRows, resultType = resultType, intField = defaultInt)
    }

}