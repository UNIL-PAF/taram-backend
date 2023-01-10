package ch.unil.pafanalysis.analysis.steps.pca

data class PcaRes (val groups: List<PcGroup>? = null, val expNames: List<String>? = null, val nrPc: Int?)

data class PcGroup(val groupName: String?, val pcList: List<OnePc>, val expIdxs: List<Int>? = null)

data class OnePc(val idx: Int, val pcVals: List<Double>, val explVar: Double)