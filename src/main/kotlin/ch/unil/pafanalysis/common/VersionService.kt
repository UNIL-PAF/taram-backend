package ch.unil.pafanalysis.common

import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Component

@Component
class VersionService(private val buildProperties: BuildProperties?) {
    fun version(): String = buildProperties?.version ?: ""
}
