package ch.unil.pafanalysis.analysis.steps.filter

import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.ReadTableData
import org.springframework.stereotype.Service
import kotlin.math.ln

@Service
class FixFilterRunner() {

    fun run(table: ReadTableData.Table, params: FilterParams): ReadTableData.Table {
        return table
    }

    fun removeContaminants( table: ReadTableData.Table, params: FilterParams):  ReadTableData.Table{
        return table
    }

    fun removeOnlyIdentifiedBySite( table: ReadTableData.Table, params: FilterParams):  ReadTableData.Table{
        return table
    }

    fun remove( table: ReadTableData.Table, params: FilterParams):  ReadTableData.Table{
        return table
    }

}