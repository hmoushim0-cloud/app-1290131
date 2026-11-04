package sa.factory.app1290131.curriculum

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

enum class ChemistryTopic {
    STOICHIOMETRY,
    THERMOCHEMISTRY,
    CHEMICAL_EQUILIBRIUM,
    ACIDS_AND_BASES,
    ELECTROCHEMISTRY,
    ORGANIC_HYDROCARBONS
}

data class MolarMassResult(
    val formula: String,
    val molarMassGPerMol: Double,
    val elementalPercentages: Map<String, Double>
)

data class AcidBaseEquilibriumMetrics(
    val hydroniumConcentrationM: Double,
    val hydroxideConcentrationM: Double,
    val pH: Double,
    val pOH: Double,
    val isAcidic: Boolean,
    val classificationArabic: String
)

data class TitrationEquivalenceResult(
    val analyteConcentrationM: Double,
    val analyteVolumeMl: Double,
    val titrantConcentrationM: Double,
    val titrantEquivalenceVolumeMl: Double,
    val acidProtons: Int,
    val baseProtons: Int
)

data class GalvanicCellMetrics(
    val cathodeReductionPotentialV: Double,
    val anodeReductionPotentialV: Double,
    val standardCellPotentialV: Double,
    val isSpontaneous: Boolean,
    val gibbsFreeEnergyStatus: String
)

data class HessLawEnthalpyResult(
    val reactionEquation: String,
    val deltaHReactionKj: Double,
    val isExothermic: Boolean,
    val thermodynamicTypeArabic: String
)

data class TahsiliQuestionVerification(
    val questionId: String,
    val topic: ChemistryTopic,
    val stemArabic: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val scientificExplanationArabic: String,
    val isMinistryCurriculumCompliant: Boolean = false,
    val auditNotes: List<String> = emptyList()
)

class TahsiliChemistryAuditEngine {

    // Standard atomic weights based on IUPAC / Saudi Ministry of Education Chemistry 1-3
    private val standardAtomicWeights = mapOf(
        "H" to 1.008,
        "C" to 12.011,
        "N" to 14.007,
        "O" to 15.999,
        "Na" to 22.990,
        "Mg" to 24.305,
        "Al" to 26.982,
        "S" to 32.065,
        "Cl" to 35.453,
        "K" to 39.098,
        "Ca" to 40.078,
        "Fe" to 55.845,
        "Cu" to 63.546,
        "Zn" to 65.380
    )

    fun computeMolarMass(formula: String): MolarMassResult {
        val elementCounts = parseFormula(formula)
        var totalMass = 0.0
        val elementalWeights = mutableMapOf<String, Double>()

        for ((elem, count) in elementCounts) {
            val atomicWeight = standardAtomicWeights[elem]
                ?: throw IllegalArgumentException("Element symbol not supported in standard curriculum: $elem")
            val subMass = atomicWeight * count
            elementalWeights[elem] = subMass
            totalMass += subMass
        }

        val percentages = mutableMapOf<String, Double>()
        for ((elem, subMass) in elementalWeights) {
            val pct = (subMass / totalMass) * 100.0
            percentages[elem] = (pct * 100.0).roundToInt() / 100.0
        }

        return MolarMassResult(
            formula = formula,
            molarMassGPerMol = (totalMass * 100.0).roundToInt() / 100.0,
            elementalPercentages = percentages
        )
    }

    private fun parseFormula(formula: String): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        var i = 0
        val n = formula.length

        while (i < n) {
            if (formula[i].isUpperCase()) {
                val elemStart = i
                i++
                while (i < n && formula[i].isLowerCase()) {
                    i++
                }
                val element = formula.substring(elemStart, i)

                val countStart = i
                while (i < n && formula[i].isDigit()) {
                    i++
                }
                val count = if (countStart < i) formula.substring(countStart, i).toInt() else 1
                counts[element] = (counts[element] ?: 0) + count
            } else {
                i++
            }
        }
        return counts
    }

    fun calculateAcidBaseEquilibrium(hydroniumConcentrationM: Double): AcidBaseEquilibriumMetrics {
        if (hydroniumConcentrationM <= 0.0) {
            throw IllegalArgumentException("Hydronium concentration must be strictly positive")
        }

        val pH = -log10(hydroniumConcentrationM)
        val pOH = 14.0 - pH
        val hydroxideConcM = 10.0.pow(-pOH)
        val isAcidic = pH < 7.0

        val classification = when {
            pH < 3.0 -> "حمض قوي جدا (تركيز عال لأيونات الهيدرونيوم)"
            pH < 6.5 -> "حمض ضعيف أو محلول حامضي معتدل"
            pH in 6.5..7.5 -> "محلول متعادل هيدروجينيا (نقطة التعادل التام)"
            pH in 7.5..11.0 -> "قاعدة ضعيفة أو محلول قلوي معتدل"
            else -> "قاعدة قوية جدا (تركيز عال لأيونات الهيدروكسيد)"
        }

        return AcidBaseEquilibriumMetrics(
            hydroniumConcentrationM = hydroniumConcentrationM,
            hydroxideConcentrationM = hydroxideConcM,
            pH = (pH * 100.0).roundToInt() / 100.0,
            pOH = (pOH * 100.0).roundToInt() / 100.0,
            isAcidic = isAcidic,
            classificationArabic = classification
        )
    }

    fun calculateTitrationEquivalenceVolume(
        analyteM: Double,
        analyteVolMl: Double,
        titrantM: Double,
        acidProtons: Int = 1,
        baseProtons: Int = 1
    ): TitrationEquivalenceResult {
        if (titrantM <= 0.0) throw IllegalArgumentException("Titrant concentration must be positive")
        // n_acid * M_acid * V_acid = n_base * M_base * V_base
        // V_titrant = (analyteM * analyteVolMl * acidProtons) / (titrantM * baseProtons)
        val titrantVolMl = (analyteM * analyteVolMl * acidProtons.toDouble()) / (titrantM * baseProtons.toDouble())

        return TitrationEquivalenceResult(
            analyteConcentrationM = analyteM,
            analyteVolumeMl = analyteVolMl,
            titrantConcentrationM = titrantM,
            titrantEquivalenceVolumeMl = (titrantVolMl * 100.0).roundToInt() / 100.0,
            acidProtons = acidProtons,
            baseProtons = baseProtons
        )
    }

    fun evaluateGalvanicCell(cathodeReductionPotentialV: Double, anodeReductionPotentialV: Double): GalvanicCellMetrics {
        val e0Cell = cathodeReductionPotentialV - anodeReductionPotentialV
        val isSpontaneous = e0Cell > 0.0
        val gibbsStatus = if (isSpontaneous) {
            "تفاعل أكسدة واختزال تلقائي (Delta G0 < 0) - يولد تيارا كهربائيا في الخلية الجلفانية"
        } else {
            "تفاعل غير تلقائي (Delta G0 > 0) - يتطلب تيارا خارجيا في خلية تحليل كهربائي"
        }

        return GalvanicCellMetrics(
            cathodeReductionPotentialV = cathodeReductionPotentialV,
            anodeReductionPotentialV = anodeReductionPotentialV,
            standardCellPotentialV = (e0Cell * 1000.0).roundToInt() / 1000.0,
            isSpontaneous = isSpontaneous,
            gibbsFreeEnergyStatus = gibbsStatus
        )
    }

    fun calculateReactionEnthalpy(
        productEnthalpiesKj: List<Double>,
        reactantEnthalpiesKj: List<Double>,
        reactionEquation: String = "Generic Reaction"
    ): HessLawEnthalpyResult {
        val sumProducts = productEnthalpiesKj.sum()
        val sumReactants = reactantEnthalpiesKj.sum()
        val deltaH = sumProducts - sumReactants
        val isExo = deltaH < 0.0

        val thermodynamicType = if (isExo) {
            "تفاعل طارد للحرارة (Delta H سالب) - يطلق طاقة حرارية إلى المحيط الخارجي"
        } else {
            "تفاعل ماص للحرارة (Delta H موجب) - يمتص طاقة حرارية من الوسط المحيط"
        }

        return HessLawEnthalpyResult(
            reactionEquation = reactionEquation,
            deltaHReactionKj = (deltaH * 10.0).roundToInt() / 10.0,
            isExothermic = isExo,
            thermodynamicTypeArabic = thermodynamicType
        )
    }

    fun auditTahsiliMultipleChoiceQuestion(question: TahsiliQuestionVerification): TahsiliQuestionVerification {
        val notes = mutableListOf<String>()

        // Rule 1: Exactly 4 options mandated by Qiyas / Tahseeli
        if (question.options.size != 4) {
            notes.add("مخالفة معيار قياس: يجب أن يتضمن السؤال 4 خيارات حصرية بالضبط (الحالي: ${question.options.size})")
        }

        // Rule 2: Options must be distinct and non-empty
        val distinctOptions = question.options.map { it.trim() }.toSet()
        if (distinctOptions.size != question.options.size) {
            notes.add("مخالفة جودة: وجود خيارات مكررة أو فارغة بين بدائل الإجابة")
        }

        // Rule 3: Valid correct option index
        if (question.correctOptionIndex !in 0 until question.options.size) {
            notes.add("خطأ منهجي: مؤشر الإجابة الصحيحة يقع خارج نطاق الخيارات المتاحة")
        }

        // Rule 4: Clear question stem in Arabic
        if (question.stemArabic.trim().length < 15) {
            notes.add("نص السؤال قصير جدا ولا يفي بالمتطلبات الإرشادية لمركز قياس الوطني")
        }

        // Rule 5: Scientific explanation provided
        if (question.scientificExplanationArabic.trim().length < 20) {
            notes.add("شرح الحل النموذجي مقتضب - يلزم توضيح الخطوات الكيميائية والقوانين المستخدمة")
        }

        val isCompliant = notes.isEmpty()

        return question.copy(
            isMinistryCurriculumCompliant = isCompliant,
            auditNotes = notes
        )
    }
}
