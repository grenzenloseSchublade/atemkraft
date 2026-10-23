package app.atemkraft.data

import app.atemkraft.domain.Reference

/**
 * Verifizierter Quellen-Katalog (Stand der Recherche). Jede Quelle wurde gegen
 * PubMed/PMC/Verlag geprüft; Korrekturen aus der Vorlage docs/F3_Atmung.html sind hier
 * bereits eingearbeitet (Autor Resonanz-Paper, PLOS-Jahr, Nadi-Shodhana-Quellen,
 * Wim-Hof-Zuschreibung, Lippenbremse ohne „30 min").
 */
object Refs {

    val kox2014 = Reference(
        "Kox M. et al. (2014): Voluntary activation of the sympathetic nervous system " +
            "and attenuation of the innate immune response in humans. PNAS 111(20):7379–84.",
        "doi:10.1073/pnas.1322174111\u00A0· PMID 24799686",
    )
    val zwaag2022 = Reference(
        "Zwaag J. et al. (2022): The Effects of Cold Exposure Training and a Breathing " +
            "Exercise on the Inflammatory Response in Humans – A Pilot Study. " +
            "Psychosomatic Medicine 84(4):457–467. Isolierte die Atmung als Treiber (Pilot).",
        "doi:10.1097/PSY.0000000000001065\u00A0· PMID 35213875",
    )
    val whmReview2024 = Reference(
        "Almahayni O. & Hammond L. (2024): Wim Hof Method – systematische Übersicht. " +
            "PLOS ONE 19(3):e0286933. Mögliche Entzündungssenkung, aber niedrige Studienqualität.",
        "doi:10.1371/journal.pone.0286933\u00A0· PMID 38478473",
    )
    val balban2023 = Reference(
        "Balban M. Y. et al. (2023): Brief structured respiration practices enhance mood " +
            "and reduce physiological arousal. Cell Reports Medicine 4(1):100895. " +
            "RCT, 5 min/Tag × 28 Tage; Cyclic Sighing am wirksamsten für Stimmung.",
        "doi:10.1016/j.xcrm.2022.100895\u00A0· PMID 36630953",
    )
    val fincham2023 = Reference(
        "Fincham G. W. et al. (2023): Effect of breathwork on stress and mental health – " +
            "Meta-Analyse von RCTs. Scientific Reports 13:432.",
        "doi:10.1038/s41598-022-27247-y",
    )
    val respeRate = Reference(
        "RESPeRATE (FDA-zugelassener Atemtrainer): vertont die Atmung – die Tonhöhe steigt beim " +
            "Einatmen und fällt beim Ausatmen; verlangsamt so die Atmung zur Blutdrucksenkung.",
        "US-Patent 7,255,672 B2 (Elliott et al.)",
    )
    val lehrer2014 = Reference(
        "Lehrer P. M. & Gevirtz R. (2014): Heart rate variability biofeedback – how and " +
            "why does it work? Frontiers in Psychology 5:756. Mechanismus der " +
            "Resonanz bei etwa 0,1 Hz (Baroreflex/RSA).",
        "doi:10.3389/fpsyg.2014.00756\u00A0· PMID 25101026",
    )
    val li2016 = Reference(
        "Li P. et al. (2016): The peptidergic control circuit for sighing. Nature 530:293–297. " +
            "Identifiziert die Hirnstamm-Neurone (preBötzinger), die den Seufzer auslösen.",
        "doi:10.1038/nature16964\u00A0· PMID 26855425",
    )
    val ma2017 = Reference(
        "Ma X. et al. (2017): The effect of diaphragmatic breathing on attention, negative " +
            "affect and stress in healthy adults. Frontiers in Psychology 8:874. RCT (n=40, " +
            "8 Wochen): weniger negativer Affekt, niedrigeres Cortisol.",
        "doi:10.3389/fpsyg.2017.00874\u00A0· PMID 28626434",
    )
    val seppala2020 = Reference(
        "Seppälä E. M. et al. (2020): Promoting mental health in university students – RCT " +
            "(n=131) eines SKY-Programms vs. Achtsamkeit/Emotionale Intelligenz. " +
            "Frontiers in Psychiatry 11:590.",
        "doi:10.3389/fpsyt.2020.00590\u00A0· PMID 32760296",
    )
    val nam2024 = Reference(
        "Nam T. G. et al. (2024): Effekt der Wechselatmung auf den Blutdruck – Meta-Analyse " +
            "(6 RCTs, n=525). SBP −7,16 / DBP −5,16 mmHg, aber hohe Heterogenität, kaum Blindung.",
        "doi:10.1159/000539707\u00A0· PMID 39008954",
    )
    val shetty2019 = Reference(
        "Shetty P. et al.: Sheetali/Sheetkari pranayama bei Hypertonie – RCT (60 Pat., " +
            "10 min/Tag). Senkung von SBP und Herzfrequenz; kleine, unverblindete Studie.",
        "PMID 30936803",
    )
    val telles2020 = Reference(
        "Telles S. et al. (2020): Körpertemperatur und Energieumsatz bei Sheetali/Sheetkari. " +
            "Med Sci Monit Basic Res 26:e920107. Temperatur und Energieumsatz stiegen leicht – " +
            "widerlegt die wörtliche „Kühlung“.",
        "doi:10.12659/MSMBR.920107\u00A0· PMID 31907342",
    )
    val shaffer2020 = Reference(
        "Shaffer F. & Meehan Z. M. (2020): A Practical Guide to Resonance Frequency " +
            "Assessment for HRV Biofeedback. Frontiers in Neuroscience 14:570400. " +
            "Resonanzfrequenz meist um 0,1 Hz (etwa 6/min).",
        "doi:10.3389/fnins.2020.570400",
    )
    val goessl2017 = Reference(
        "Goessl V. C. et al. (2017): The effect of heart rate variability biofeedback training " +
            "on stress and anxiety: a meta-analysis. Psychological Medicine 47(15):2578–86. " +
            "24 Studien, 484 Teilnehmende: großer Rückgang von selbstberichtetem Stress und Angst " +
            "(Effektstärke g ≈ 0,8); es braucht mehr gut kontrollierte Studien.",
        "doi:10.1017/S0033291717001003\u00A0· PMID 28478782",
    )
    val zaccaro2018 = Reference(
        "Zaccaro A. et al. (2018): How breath-control can change your life – systematische " +
            "Übersicht zu langsamem Atmen. Frontiers in Human Neuroscience 12:353. " +
            "Langsames Atmen (<10/min) → parasympathische Dominanz, HRV/RSA ↑.",
        "doi:10.3389/fnhum.2018.00353\u00A0· PMID 30245619",
    )
    val vierra2022 = Reference(
        "Vierra J. et al. (2022): 4-7-8-Atmung – akute Effekte auf HRV und Blutdruck. " +
            "Physiological Reports 10(13):e15389.",
        "doi:10.14814/phy2.15389\u00A0· PMID 35822447",
    )
    val compare2025 = Reference(
        "Marchant J. et al. (2025): Comparing the Effects of Square, 4-7-8, and 6 " +
            "Breaths-per-Minute Breathing Conditions on Heart Rate Variability, CO2 Levels, and " +
            "Mood. Appl Psychophysiol Biofeedback 50(2):261–76. 84 Studierende: 6/min hebt die " +
            "HRV stärker als Box/4-7-8; keine nennenswerte Änderung von Blutdruck oder Stimmung.",
        "doi:10.1007/s10484-025-09688-z\u00A0· PMID 39864026",
    )
    val pramanik2010 = Reference(
        "Pramanik T. et al. (2010): Immediate effect of a slow pace breathing exercise " +
            "Bhramari pranayama on blood pressure and heart rate. " +
            "Nepal Med Coll J 12(3):154–7. Sofortige Senkung von diastolischem/mittlerem BP.",
        "PMID 21446363",
    )
    val weitzberg2002 = Reference(
        "Weitzberg E. & Lundberg J. O. (2002): Humming greatly increases nasal nitric oxide. " +
            "Am J Respir Crit Care Med 166(2):144–5. Summen erhöht nasales NO etwa 15-fach.",
        "doi:10.1164/rccm.200202-138BC\u00A0· PMID 12119224",
    )
    val nadiShodhana2024 = Reference(
        "Explorative RCT zu Nadi Shodhana bei Hypertonie: Senkung von SBP/DBP und HRV-Anstieg " +
            "nach 10 min und 6 Wochen (kleine Stichprobe, niedrige–moderate Qualität).",
        "PMID 40242728\u00A0· PMC11996816",
    )
    val nadiBhramari2023 = Reference(
        "RCT zu Nadi Shodhana + Bhramari auf kardiovaskuläre/autonome Parameter.",
        "PMID 37499590\u00A0· PMC10388195",
    )
    val sky2000 = Reference(
        "Janakiramaiah N. et al. (2000): Antidepressive Wirksamkeit von Sudarshan Kriya Yoga " +
            "bei Melancholie (RCT vs. EKT/Imipramin). J Affect Disord 57:255–9. " +
            "Remission: EKT 93 %, Imipramin 73 %, SKY 67 %.",
        "doi:10.1016/s0165-0327(99)00079-8\u00A0· PMID 10708840",
    )
    val buteykoPrem2013 = Reference(
        "Prem V. et al. (2013): Buteyko vs. Pranayama bei Asthma – RCT. " +
            "Clin Rehabil 27(2):133–41. Bessere Trends in Lebensqualität/Asthmakontrolle.",
        "doi:10.1177/0269215512450521\u00A0· PMID 22837543",
    )
    val buteykoCochrane2020 = Reference(
        "Santino T. A. et al. (2020): Breathing exercises for adults with asthma. " +
            "Cochrane Database Syst Rev 3:CD001277. 22 Studien, 2 mit Buteyko: Lebensqualität " +
            "besser (moderat), Asthma-Symptome und FEV1 unklar, FEV1 in % vom Soll besser.",
        "doi:10.1002/14651858.CD001277.pub4\u00A0· PMID 32212422",
    )
    val mayer2018 = Reference(
        "Mayer A. F. et al. (2018): Effects of acute use of pursed-lips breathing – " +
            "systematische Übersicht/Meta-Analyse. Physiotherapy 104(1):9–17. Senkt Atem- " +
            "frequenz/Minutenvolumen; kein sicherer Gewinn bei Gehstrecke oder SpO₂.",
        "doi:10.1016/j.physio.2017.08.007\u00A0· PMID 28969859",
    )
    val pursedLip2021 = Reference(
        "Mitsungnern T. et al. (2021): Lippenbremse + Mitzählen auf BP/HR bei hypertensiver " +
            "Dringlichkeit – RCT (110 Pat.). J Clin Hypertens 23(3):672–9. " +
            "Akut signifikante Senkung von SBP/DBP/HR.",
        "doi:10.1111/jch.14168\u00A0· PMID 33410589",
    )
    val nasalNo1996 = Reference(
        "Lundberg J. O. et al. (1996): Inhalation of nasally derived nitric oxide modulates " +
            "pulmonary function in humans. Acta Physiol Scand 158(4):343–7.",
        "PMID 8971255",
    )
    val kapalabhatiCase2004 = Reference(
        "Johnson D. B. et al. (2004): Kapalabhati pranayama – breath of fire or cause of " +
            "pneumothorax? A case report. Chest 125(5):1951–2. Spontan-Pneumothorax nach " +
            "kräftiger, langer Feueratmung.",
        "PMID 15136413",
    )

    // ---- Atemlehre: Verhältnis von Ein- und Ausatmen (Karten „Studien“) ----

    val laborde2022 = Reference(
        "Laborde S. et al. (2022): Effects of voluntary slow breathing on heart rate and heart " +
            "rate variability: A systematic review and a meta-analysis. Neurosci Biobehav Rev " +
            "138:104711. 223 Studien; langsames Atmen erhöht die vagal vermittelte HRV.",
        "doi:10.1016/j.neubiorev.2022.104711\u00A0· PMID 35623448",
    )
    val bae2021 = Reference(
        "Bae D. et al. (2021): Increased exhalation to inhalation ratio during breathing " +
            "enhances high-frequency heart rate variability in healthy adults. Psychophysiology " +
            "58(11):e13905. Ausatmen doppelt so lang: HRV höher als bei 1 : 1 (n = 28).",
        "doi:10.1111/psyp.13905\u00A0· PMID 34289128",
    )
    val meehan2024 = Reference(
        "Meehan Z. M. & Shaffer F. (2024): Do Longer Exhalations Increase HRV During " +
            "Slow-Paced Breathing? Appl Psychophysiol Biofeedback 49(3):407–417. Bei 6/min kein " +
            "Unterschied zwischen 1 : 1 und 1 : 2, in einer zweiten Studie bestätigt.",
        "doi:10.1007/s10484-024-09637-2\u00A0· PMID 38507210",
    )
    val birdee2023 = Reference(
        "Birdee G. et al. (2023): Slow breathing for reducing stress: The effect of extending " +
            "exhale. Complement Ther Med 73:102937. RCT, 12 Wochen, n = 100: langsames Atmen " +
            "senkte Stress, das Verhältnis machte keinen signifikanten Unterschied.",
        "doi:10.1016/j.ctim.2023.102937\u00A0· PMID 36871835",
    )
    val chetry2026 = Reference(
        "Chetry D. et al. (2026): Heart Rate Variability and Mood State Changes in Three Yoga " +
            "Breathing Practices: A Randomized Crossover Trial. Int J Yoga Therap 35. Schnelles " +
            "Atmen senkte die HRV, Bhramari hob die Stimmung (Geübte, nur Sofortwirkung).",
        "doi:10.17761/2025-D-25-00006\u00A0· PMID 41580220",
    )

    // ---- Pranayama: Seite im Tab „Lernen“ und die Übungen Ujjayi und Dirgha ----
    // Geprüft gegen PubMed und Crossref (2026-10-23).

    val mason2013 = Reference(
        "Mason H. et al. (2013): Cardiovascular and respiratory effect of yogic slow breathing in " +
            "the yoga beginner: what is the best approach? Evid Based Complement Alternat Med " +
            "2013:743504. 17 Yoga-Anfänger: Langsames Atmen hob die Baroreflex-Empfindlichkeit; mit " +
            "Ujjayi beim Ausatmen etwas weniger, mit Ujjayi beim Ein- und Ausatmen nicht deutlich.",
        "doi:10.1155/2013/743504\u00A0· PMID 23710236",
    )
    val mazur2024 = Reference(
        "Mazur M. D. et al. (2024): Cardiorespiratory Effects of Yogic Versus Slow Breathing in " +
            "Individuals with a Spinal Cord Injury: An Exploratory Cohort Study. J Integr Complement " +
            "Med 30(9):878–85. 12 Menschen mit Querschnittlähmung: Ujjayi hob die Sauerstoffsättigung " +
            "leicht (97,6 statt 96,1 % bei freiem Atmen).",
        "doi:10.1089/jicm.2023.0641\u00A0· PMID 38507692",
    )
    val vanDiest2014 = Reference(
        "Van Diest I. et al. (2014): Inhalation/Exhalation Ratio Modulates the Effect of Slow " +
            "Breathing on Heart Rate Variability and Relaxation. Appl Psychophysiol Biofeedback " +
            "39(3–4):171–80. 30 Teilnehmende: Längeres Ausatmen fühlte sich entspannender an und hob " +
            "bei 6/min die HRV.",
        "doi:10.1007/s10484-014-9253-x\u00A0· PMID 25156003",
    )
    val finchamHvb2023 = Reference(
        "Fincham G. W. et al. (2023): High ventilation breathwork practices: An overview of their " +
            "effects, mechanisms, and considerations for clinical applications. Neurosci Biobehav Rev " +
            "155:105453. Übersicht zu schnellem, tiefem Atmen (u. a. Wim Hof, Sudarshan Kriya): starke " +
            "Wirkung auf Nervensystem und Erleben, eigene Sicherheitsfragen.",
        "doi:10.1016/j.neubiorev.2023.105453\u00A0· PMID 37923236",
    )
    val muetze2025 = Reference(
        "Mütze C. et al. (2025): Effectiveness of pranayama for mental disorders: a systematic " +
            "review and meta-analysis of randomized controlled trials. Front Psychiatry 16:1616996. " +
            "6 RCTs, 517 Erkrankte: kurzfristig etwas weniger Symptome, hohes Verzerrungsrisiko; " +
            "Nebenwirkungen häufiger bei schnellen als bei langsamen Techniken.",
        "doi:10.3389/fpsyt.2025.1616996\u00A0· PMID 40896223",
    )
    val boyd2015 = Reference(
        "Boyd C. et al. (2015): Fatal and nonfatal drowning outcomes related to dangerous " +
            "underwater breath-holding behaviors – New York State, 1988–2011. MMWR Morb Mortal Wkly " +
            "Rep 64(19):518–21. 16 Fälle, darunter tödliche: Wer vor dem Tauchen hyperventiliert und " +
            "die Luft anhält, kann unter Wasser bewusstlos werden.",
        "PMID 25996093\u00A0· PMC4584570",
    )
    val chetry2022 = Reference(
        "Chetry D. et al. (2022): A Comparison of Practice Guidelines for Yoga Breathing from the " +
            "Traditional Texts and PubMed-Indexed Research. Int J Yoga Therap 32:Artikel 17. 73 Studien " +
            "zu Bhastrika, Bhramari und Kapalabhati: Atemtiefe, Verhältnis und Tempo weichen oft von " +
            "Hatha Yoga Pradipika und Gheranda Samhita ab.",
        "doi:10.17761/2022-D-22-00024\u00A0· PMID 36669770",
    )
    val telles2024 = Reference(
        "Telles S. et al. (2024): Heart Rate Variability during Nostril-Regulated Yoga Breathing: " +
            "A Randomized Crossover Study. Int J Yoga 17(3):203–10. 47 geübte Männer: Einatmen rechts " +
            "(Surya Bhedana) und links (Chandra Bhedana) veränderten die HRV gleichartig.",
        "doi:10.4103/ijoy.ijoy_119_24\u00A0· PMID 39959514",
    )
    val singh2024 = Reference(
        "Singh A. et al. (2024): Traditional Nostril Yoga Breathing Practices and Oxygen " +
            "Consumption: A Randomized, Cross-over Study. Int J Yoga 17(1):53–60. 47 Männer: Der " +
            "Sauerstoffverbrauch stieg rechts, links und bei der Wechselatmung gleich stark.",
        "doi:10.4103/ijoy.ijoy_248_23\u00A0· PMID 38899139",
    )
}
