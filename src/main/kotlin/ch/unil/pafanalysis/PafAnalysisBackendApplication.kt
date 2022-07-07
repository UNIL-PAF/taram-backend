package ch.unil.pafanalysis

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class PafAnalysisBackendApplication

fun main(args: Array<String>) {
    runApplication<PafAnalysisBackendApplication>(*args)
}
