package ch.unil.pafanalysis

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PafAnalysisBackendApplication

fun main(args: Array<String>) {
    runApplication<PafAnalysisBackendApplication>(*args)
}
