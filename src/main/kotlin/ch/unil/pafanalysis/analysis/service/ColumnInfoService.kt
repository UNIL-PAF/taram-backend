package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.ColumnInfo
import ch.unil.pafanalysis.analysis.steps.CommonResult
import ch.unil.pafanalysis.common.Crc32HashComputations
import ch.unil.pafanalysis.results.model.ResultType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Transactional
@Service
class ColumnInfoService {

    @Autowired
    private var columnParser: ColumnMappingParser? = null

    fun createColumnInfo(filePath: String?, resultPath: String?, type: ResultType?): Pair<ColumnInfo, CommonResult> {
        val (columnMapping, commonResult) = columnParser!!.parse(filePath, resultPath, type)
        val crc32Hash = Crc32HashComputations().computeStringHash(columnMapping.toString())
        return Pair(ColumnInfo(columnMapping = columnMapping, columnMappingHash = crc32Hash), commonResult)
    }

}