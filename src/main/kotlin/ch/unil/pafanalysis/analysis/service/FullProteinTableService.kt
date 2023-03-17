package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.HeaderMaps
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.results.model.ResultType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class FullProteinTableService {

    val readTable = ReadTableData()

    @Autowired
    private var commonStep: CommonStep? = null

    fun getTable(step: AnalysisStep?): FullProteinTable {
        val tableRows = readTable.getRows(commonStep?.getOutputRoot() + step?.resultTablePath, step?.commonResult?.headers)
        val protRows = tableRows?.mapIndexed{i, row -> FullProtRow(key = i, cols = row)}
        return FullProteinTable(rows = protRows, headers = step?.commonResult?.headers, stepId = step?.id)
    }

}