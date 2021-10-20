package ch.unil.pafanalysis.results.service

import org.springframework.data.repository.CrudRepository
import ch.unil.pafanalysis.results.model.InitialResult

interface ResultRepository: CrudRepository<InitialResult, Integer> {
}