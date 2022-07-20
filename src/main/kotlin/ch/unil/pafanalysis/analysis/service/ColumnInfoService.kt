package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.ColumnInfo
import ch.unil.pafanalysis.analysis.model.ColumnMapping
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.steps.CommonResult
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.CheckTypes
import ch.unil.pafanalysis.common.Crc32HashComputations
import ch.unil.pafanalysis.results.model.ResultType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File


@Transactional
@Service
class ColumnInfoService {

    @Autowired
    private var columnInfoRepository: ColumnInfoRepository? = null

    @Autowired
    private var columnParser: ColumnMappingParser? = null

    fun createAndSaveColumnInfo(filePath: String?, resultPath: String?, type: ResultType?): Pair<ColumnInfo?, CommonResult> ? {
        val (columnInfo, commonResult) = createColumnInfo(filePath, resultPath, type)
        return Pair(columnInfoRepository?.saveAndFlush(columnInfo), commonResult)
    }

    fun createColumnInfo(filePath: String?, resultPath: String?, type: ResultType?): Pair<ColumnInfo, CommonResult> {
        val (columnMapping, commonResult) = columnParser!!.parse(filePath, resultPath, type)
        val crc32Hash = Crc32HashComputations().computeStringHash(columnMapping.toString())
        return Pair(ColumnInfo(columnMapping = columnMapping, columnMappingHash = crc32Hash), commonResult)
    }

}