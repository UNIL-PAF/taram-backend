package ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut

data class SpectronautSetup(
    val softwareVersion: String? = null,
    val analysisDate: String? = null,
    val analysisType: String? = null,
//    val massTolerances: String? = null,
//    val missedCleavages: String? = null,
//    val modifications: String? = null,
//    val fdr: String? = null,
//    val normalization: String? = null,
    val runs: List<SpectronautRun>? = null,
    val proteinDBs: List<SpectronautProteinDB>? = null,
    val libraries: List<SpectronautLibraries>? = null
)

data class SpectronautRun(
    val name: String? = null,
    val vendor: String? = null,
    val fileName: String? = null,
    val condition: String? = null,
    val version: String? = null,
)

data class SpectronautLibraries(
    val name: String? = null,
    val fileName: String? = null,
    //val precursorTargeted: Int? = null,
    //val decoysAdded: Int? = null
)

data class SpectronautProteinDB(
    val name: String? = null,
    val fileName: String? = null,
    //val entries: Int? = null,
    val creationDate: String? = null,
    val modificationDate: String? = null,
    //val parsingRule: String? = null
)