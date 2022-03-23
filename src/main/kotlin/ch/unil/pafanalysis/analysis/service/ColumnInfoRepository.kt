package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.ColumnInfo
import org.springframework.data.repository.CrudRepository

interface ColumnInfoRepository: CrudRepository<ColumnInfo, Integer> {

}
