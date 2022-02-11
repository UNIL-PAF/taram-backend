package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import org.springframework.data.repository.CrudRepository

interface AnalysisStepRepository: CrudRepository<AnalysisStep, Integer> {
}
