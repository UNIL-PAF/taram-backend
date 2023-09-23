package ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File


@SpringBootTest
class ParseSpectronautSetupTests {

    @Autowired
    private val parser: ParseSpectronautSetup? = null

    @Test
    fun bockSetup() {
        val resPath = "./src/test/resources/results/spectronaut/"
        val filePath = File(resPath + "20230921_Bock_16619-42_directDIA_Report.setup.txt")
        val spectronautSetup = parser?.parseSetup(filePath)
        println(spectronautSetup)
    }


    @Test
    fun gaspariSetup() {
        val resPath = "./src/test/resources/results/spectronaut/"
        val filePath = File(resPath + "Gaspari-15321-32-Library_Report.setup.txt")
        val spectronautSetup = parser?.parseSetup(filePath)

        assert(spectronautSetup?.analysisType == "Peptide-Centric")
        assert(spectronautSetup?.analysisDate == "13-December-2022 12:15:49 +01:00")
        assert(spectronautSetup?.softwareVersion == "Spectronaut 17.0.221202.55965")

        // runs
        assert(spectronautSetup?.runs?.size == 12)
        val firstRun = spectronautSetup?.runs?.get(0)
        assert(firstRun?.name == "4450_12-7-2022_Gaspari_15321_DIA_140min_3ul_RB1.htrms")
        assert(firstRun?.condition == "Ctrl")
        assert(firstRun?.fileName == "4450_12-7-2022_Gaspari_15321_DIA_140min_3ul_RB1")
        assert(firstRun?.vendor == "Bruker")
        assert(firstRun?.version == "17.0.221202.55965")

        // libraries
        assert(spectronautSetup?.libraries?.size == 2)
        val firstLib = spectronautSetup?.libraries?.get(0)
        assert(firstLib?.name == "Gaspari-15377-RPB-TIMS")
        assert(firstLib?.fileName == "20221209_160314_Gaspari-15377-RPB-TIMS.kit")

        // DB's
        assert(spectronautSetup?.proteinDBs?.size == 3)
        val firstDb = spectronautSetup?.proteinDBs?.get(0)
        assert(firstDb?.name == "iRT")
        assert(firstDb?.fileName == "iRT.fasta")
        assert(firstDb?.creationDate == "24-April-2017 09:27:39 +02:00")
        assert(firstDb?.modificationDate == "09-December-2022 18:52:00 +01:00")
    }

}
