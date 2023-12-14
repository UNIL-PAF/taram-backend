package ch.unil.pafanalysis.annotation.service

import ch.unil.pafanalysis.annotations.service.AnnotationService
import ch.unil.pafanalysis.common.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File

@SpringBootTest
class AnnotationServiceTests {

    @Autowired
    val annotationService: AnnotationService? = null

    @Test
    fun replaceImputedVals() {
        val annotationFile = "./src/test/resources/annotations/mainAnnot.saccharomyces_cerevisiae_strain_atcc_204508_s288c.txt"
        val (headers, nrEntries) = annotationService!!.getHeadersAndNrEntries(File(annotationFile))
        assert(nrEntries == 6748)
        assert(headers.size == 42)
    }
}