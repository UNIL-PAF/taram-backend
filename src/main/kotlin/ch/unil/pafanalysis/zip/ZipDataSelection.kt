package ch.unil.pafanalysis.zip

data class ZipDataSelection (
  val plots: List<Int>? = null,
  val mainTables: List<Int>? = null,
  val specialTables: List<Int>? = null,
  val steps: List<Int>? = null,
  val analysisId: Int? = null
)