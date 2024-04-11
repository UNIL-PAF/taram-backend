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
    fun siddeekSetup() {
        val resPath = "./src/test/resources/results/spectronaut/"
        val filePath = File(resPath + "Siddeek-17243-54-library-TIMS_Report.setup.txt")
        val spectronautSetup = parser?.parseSetup(filePath)

        assert(spectronautSetup?.analysisType == "Peptide-Centric")
        assert(spectronautSetup?.analysisDate == "11-April-2024 11:48:54 UTC+0")
        assert(spectronautSetup?.softwareVersion == "Spectronaut 18.7.240325.55695")

        // runs
        assert(spectronautSetup?.runs?.size == 12)
        val firstRun = spectronautSetup?.runs?.get(0)

        assert(firstRun?.name == "6697_Siddeek_17243_15SPD.htrms")
        assert(firstRun?.condition == "Ctrl")
        assert(firstRun?.fileName == "6697_Siddeek_17243_15SPD")
        assert(firstRun?.vendor == "Bruker")
        assert(firstRun?.version == "18.7.240325.55695")

    }


    @Test
    fun bockSetup() {
        val resPath = "./src/test/resources/results/spectronaut/"
        val filePath = File(resPath + "20230921_Bock_16619-42_directDIA_Report.setup.txt")
        val spectronautSetup = parser?.parseSetup(filePath)

        assert(spectronautSetup?.analysisType == "directDIA")
        assert(spectronautSetup?.analysisDate == "21-September-2023 12:35:51 UTC+0")
        assert(spectronautSetup?.softwareVersion == "Spectronaut 18.3.230830.50606")

        // runs
        assert(spectronautSetup?.runs?.size == 24)
        val firstRun = spectronautSetup?.runs?.get(0)

        assert(firstRun?.name == "5683_Bock_DIA_16619_140min_8ul.htrms")
        assert(firstRun?.condition == "WT cellular extract")
        assert(firstRun?.fileName == "5683_Bock_DIA_16619_140min_8ul")
        assert(firstRun?.vendor == "Bruker")
        assert(firstRun?.version == "18.3.230830.50606")

        // libraries
        assert(spectronautSetup?.libraries == null)

        // DB's
        assert(spectronautSetup?.proteinDBs?.size == 2)
        val firstDb = spectronautSetup?.proteinDBs?.get(0)
        assert(firstDb?.name == "Universal Contaminant Protein FASTA")
        assert(firstDb?.fileName == "Universal Contaminant Protein FASTA.fasta")
        assert(firstDb?.creationDate == "01-June-2022 11:47:15 UTC+0")
        assert(firstDb?.modificationDate == "21-September-2023 08:43:18 UTC+0")

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
