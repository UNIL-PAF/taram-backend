package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.ColumnInfo
import org.springframework.data.jpa.repository.JpaRepository

interface ColumnInfoRepository: JpaRepository<ColumnInfo, Integer> {

}
