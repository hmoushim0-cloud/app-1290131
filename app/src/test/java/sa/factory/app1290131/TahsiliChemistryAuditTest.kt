package sa.factory.app1290131

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import sa.factory.app1290131.curriculum.*
import sa.factory.app1290131.hardware.BeerLambertCalibration
import sa.factory.app1290131.hardware.LabColorimeterPhotometerDriver

class TahsiliChemistryAuditTest {

    private lateinit var auditEngine: TahsiliChemistryAuditEngine
    private lateinit var photometerDriver: LabColorimeterPhotometerDriver

    @Before
    fun setUp() {
        auditEngine = TahsiliChemistryAuditEngine()
        photometerDriver = LabColorimeterPhotometerDriver()
    }

    @Test
    fun testMolarMassCalculationAndPercentages() {
        // Water H2O: 2*1.008 + 15.999 = 18.015 -> 18.02 g/mol
        val water = auditEngine.computeMolarMass("H2O")
        assertEquals(18.02, water.molarMassGPerMol, 0.05)
        assertEquals(11.19, water.elementalPercentages["H"]!!, 0.1)
        assertEquals(88.81, water.elementalPercentages["O"]!!, 0.1)

        // Calcium Carbonate CaCO3: 40.078 + 12.011 + 3*15.999 = 100.086 -> 100.09 g/mol
        val caco3 = auditEngine.computeMolarMass("CaCO3")
        assertEquals(100.09, caco3.molarMassGPerMol, 0.05)
        assertEquals(40.04, caco3.elementalPercentages["Ca"]!!, 0.1)
        assertEquals(12.0, caco3.elementalPercentages["C"]!!, 0.1)
        assertEquals(47.96, caco3.elementalPercentages["O"]!!, 0.1)
    }

    @Test
    fun testAcidBaseEquilibriumMetrics() {
        // Strong acid: [H+] = 0.01 M -> pH = 2.00, pOH = 12.00
        val acidMetrics = auditEngine.calculateAcidBaseEquilibrium(0.01)
        assertEquals(2.0, acidMetrics.pH, 0.01)
        assertEquals(12.0, acidMetrics.pOH, 0.01)
        assertTrue(acidMetrics.isAcidic)
        assertTrue(acidMetrics.classificationArabic.contains("حمض قوي"))

        // Neutral solution: [H+] = 1.0e-7 M -> pH = 7.00, pOH = 7.00
        val neutralMetrics = auditEngine.calculateAcidBaseEquilibrium(1.0e-7)
        assertEquals(7.0, neutralMetrics.pH, 0.01)
        assertEquals(7.0, neutralMetrics.pOH, 0.01)
        assertFalse(neutralMetrics.isAcidic)
        assertTrue(neutralMetrics.classificationArabic.contains("متعادل"))
    }

    @Test
    fun testTitrationEquivalenceVolumeCalculation() {
        // Monoprotic: 25 mL of 0.10 M HCl titrated with 0.10 M NaOH -> 25 mL
        val mono = auditEngine.calculateTitrationEquivalenceVolume(
            analyteM = 0.10,
            analyteVolMl = 25.0,
            titrantM = 0.10,
            acidProtons = 1,
            baseProtons = 1
        )
        assertEquals(25.0, mono.titrantEquivalenceVolumeMl, 0.01)

        // Diprotic: 25 mL of 0.10 M H2SO4 titrated with 0.10 M NaOH -> 50 mL
        val diprotic = auditEngine.calculateTitrationEquivalenceVolume(
            analyteM = 0.10,
            analyteVolMl = 25.0,
            titrantM = 0.10,
            acidProtons = 2,
            baseProtons = 1
        )
        assertEquals(50.0, diprotic.titrantEquivalenceVolumeMl, 0.01)
    }

    @Test
    fun testGalvanicCellPotentialAndSpontaneity() {
        // Standard Daniell Cell: Cu cathode (+0.34 V), Zn anode (-0.76 V)
        // E0_cell = +0.34 - (-0.76) = +1.10 V
        val daniell = auditEngine.evaluateGalvanicCell(0.34, -0.76)
        assertEquals(1.10, daniell.standardCellPotentialV, 0.01)
        assertTrue(daniell.isSpontaneous)
        assertTrue(daniell.gibbsFreeEnergyStatus.contains("تلقائي"))

        // Electrolytic non-spontaneous cell
        val electrolytic = auditEngine.evaluateGalvanicCell(-0.83, 1.36)
        assertTrue(electrolytic.standardCellPotentialV < 0.0)
        assertFalse(electrolytic.isSpontaneous)
        assertTrue(electrolytic.gibbsFreeEnergyStatus.contains("غير تلقائي"))
    }

    @Test
    fun testHessLawReactionEnthalpy() {
        // Exothermic: Products = -393.5 kJ, Reactants = 0 kJ -> Delta H = -393.5 kJ
        val exo = auditEngine.calculateReactionEnthalpy(
            productEnthalpiesKj = listOf(-393.5),
            reactantEnthalpiesKj = listOf(0.0),
            reactionEquation = "C + O2 -> CO2"
        )
        assertEquals(-393.5, exo.deltaHReactionKj, 0.1)
        assertTrue(exo.isExothermic)
        assertTrue(exo.thermodynamicTypeArabic.contains("طارد للحرارة"))

        // Endothermic reaction: Products = 90.2 kJ, Reactants = 0 kJ -> Delta H = +90.2 kJ
        val endo = auditEngine.calculateReactionEnthalpy(
            productEnthalpiesKj = listOf(90.2),
            reactantEnthalpiesKj = listOf(0.0),
            reactionEquation = "N2 + O2 -> 2NO"
        )
        assertEquals(90.2, endo.deltaHReactionKj, 0.1)
        assertFalse(endo.isExothermic)
        assertTrue(endo.thermodynamicTypeArabic.contains("ماص للحرارة"))
    }

    @Test
    fun testTahsiliQuestionVerificationAudit() {
        // Compliant Qiyas Tahseeli question
        val validQuestion = TahsiliQuestionVerification(
            questionId = "Q-CH-101",
            topic = ChemistryTopic.ACIDS_AND_BASES,
            stemArabic = "ما هو الرقم الهيدروجيني pH لمحلول مائي تركيز أيونات الهيدرونيوم فيه 0.001 مولار؟",
            options = listOf("1.0", "3.0", "7.0", "11.0"),
            correctOptionIndex = 1,
            scientificExplanationArabic = "يتم حساب الرقم الهيدروجيني باستخدام العلاقة: pH = -log[H3O+] = -log(10^-3) = 3"
        )

        val auditedValid = auditEngine.auditTahsiliMultipleChoiceQuestion(validQuestion)
        assertTrue("Valid Tahseeli question should be compliant", auditedValid.isMinistryCurriculumCompliant)
        assertEquals(0, auditedValid.auditNotes.size)

        // Invalid question: only 3 options, duplicated choices, and short stem
        val invalidQuestion = TahsiliQuestionVerification(
            questionId = "Q-CH-102",
            topic = ChemistryTopic.STOICHIOMETRY,
            stemArabic = "ما الكتلة؟",
            options = listOf("18 g/mol", "18 g/mol", "100 g/mol"),
            correctOptionIndex = 5,
            scientificExplanationArabic = "قصير"
        )

        val auditedInvalid = auditEngine.auditTahsiliMultipleChoiceQuestion(invalidQuestion)
        assertFalse("Invalid Tahseeli question must fail audit", auditedInvalid.isMinistryCurriculumCompliant)
        assertTrue("Audit notes should capture multiple violations", auditedInvalid.auditNotes.size >= 3)
    }

    @Test
    fun testPhotometerDriverSerializationAndBeerLambertLaw() {
        val seq = 12
        val wavelength = 525 // nm (green complementary to red/copper)
        val transmittancePct = 10.0 // 10% T -> A = -log10(0.10) = 1.000
        val ph = 7.40
        val temp = 25.0
        val orp: Short = 350

        val packet = photometerDriver.serializeFrame(seq, wavelength, transmittancePct, ph, temp, orp)
        assertEquals(LabColorimeterPhotometerDriver.FRAME_SIZE_BYTES, packet.size)

        val calibration = BeerLambertCalibration("Copper Sulfate", molarAbsorptivityLPerMolCm = 2.0, pathLengthCm = 1.0)
        val frame = photometerDriver.parseFrame(packet, calibration)

        assertEquals(seq, frame.sequenceNumber)
        assertEquals(wavelength, frame.wavelengthNm)
        assertEquals(10.0, frame.transmittancePct, 0.01)
        assertEquals(1.000, frame.absorbance, 0.001)
        assertEquals(7.40, frame.solutionPh, 0.01)
        assertEquals(25.0, frame.temperatureCelsius, 0.1)
        assertEquals(350.toShort(), frame.redoxPotentialMv)
        assertTrue("CRC-16-CCITT should be verified as valid", frame.isValidCrc)

        // Concentration c = A / (epsilon * b) = 1.000 / (2.0 * 1.0) = 0.5000 M
        assertEquals(0.5000, frame.molarConcentrationM, 0.001)

        // Corrupted packet CRC test
        val corrupted = packet.copyOf()
        corrupted[6] = (corrupted[6].toInt() xor 0xFF).toByte()
        val corruptedFrame = photometerDriver.parseFrame(corrupted, calibration)
        assertFalse("Corrupted CRC should be flagged", corruptedFrame.isValidCrc)
    }
}
