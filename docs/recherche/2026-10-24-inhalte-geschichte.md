# Atemkraft – Recherche: Welche theoretischen Inhalte fehlen noch? Was ist mit Yoga-Geschichte?

Stand: 2026-10-24, Repo v2.7.0 (5af8193). Nur gelesen, nichts geändert. Websuche am 2026-10-24; Fundstellen als Links, Primärliteratur mit DOI/ISBN wo greifbar. Alles unten sind **Vorschläge**, keine Texte zur Übernahme; die Teaser-Ideen in Teil 3 sind Rohfassungen und müssten vor Einbau gegen TEXT-06/-07/-09/-13/-14 geprüft werden.

---

## 1 Bestandsaufnahme: Was der Tab „Lernen“ heute enthält

### 1.1 Aufbau des Tabs

`ui/learn/LearnScreen.kt`: vier gleich gebaute `ChapterCard`s (`NavCard`, Titelfarbe reihum über `headingColor(0…3)`, FARBE-07), rechts Zahnrad. Jede Karte öffnet eine Push-Seite:

| Kapitel | Datei (Daten) | Datei (UI) | Aufbau |
|---|---|---|---|
| Atemlehre („Phasen, vāyus, Regeln und Muster“) | `data/Atemlehre.kt` (346 Z.) | `ui/atemlehre/AtemlehreScreen.kt` (350 Z.) | Intro-Satz · „Die vier Phasen“ (4 `LorePhase`) · „Die fünf *vāyus*“ (Körperbild + Liste, aufklappbar) · „Die Grundregeln“ (4 Regeln, fetter Kern + Erläuterung) · „Die Muster“ (11 `LorePattern`-Karten je mit Rhythmusbild, Teaser, aufgeklappt Lehre/Typisch/Studien/In der App, teils `origin`, `warning`) · „Weitere Einflüsse“ (Atemweg, Tageszeit, Ayurveda-Typ) · „Quellen“ (Lehre = Texte ohne DOI, Studien = `Refs`) |
| Pranayama („Grundlagen und Techniken, mit Übungen zum Ausprobieren“) | `data/Pranayama.kt` (438 Z.) | `ui/learn/PranayamaScreen.kt` | Intro · „Grundlagen“ (7 `PranayamaTopic`: Was Pranayama ist, Haltung und Vorbereitung, Schrittweise aufbauen, Verhältnisse, Langsam und schnell, Übergang in die Stille, Sicherheit; je Teaser + 3 Absätze + Lehrquelle) · „Techniken“ (10 `PranayamaTechnique`: Ujjayi, Dirgha, Nadi Shodhana, Viloma, Bhramari, Sitali/Sitkari, Kapalabhati, Surya/Chandra Bhedana, Bhastrika, Bandhas; je Sanskrit + Aussprache, Etikett Studienlage, Tradition, So geht’s, Studien, Sicherheit, „Üben ›“) · Quellen |
| Programme & Gewohnheiten („SKY, Buteyko, Nasenatmung“) | `data/BuiltInExercises.kt` (`guided = false`) | `ui/learn/ProgramsScreen.kt` | nur eine Liste von `ExerciseCard`s → Detailseite der Übung |
| Begriffe erklärt („Studien, HRV und Yoga-Begriffe“) | `data/Glossary.kt` (185 Z.) | `ui/glossary/GlossaryScreen.kt` | zwei Gruppen: „Körper & Studien“ (18 Begriffe: Studienlage, Meta-Analyse, RCT, verblindet, HRV, Vagus, Para-/Sympathikus, Baroreflex, RSA, CO₂-Toleranz, Hyperventilation, Hypokapnie, Hormese, COPD, PEM, Valsalva, Pneumothorax, Glaukom, Hernie) · „Aus der Yoga-Lehre“ (12: *prāṇāyāma*, *pūraka/recaka/kumbhaka*, *bṛṃhaṇa/laṅghana/samāna*, *vāyu*, 5 *vāyus*, *iḍā/piṅgalā*, *sama/viṣama vṛtti*, *bandha*) |

Dazu auf jeder Detailseite yogischer Übungen der Abschnitt „Aus der Yoga-Lehre“ (`YogaLore`: Teaser, Detail, `patternId`, `source`) und `effectDetail`-Texte mit Studienlage. `docs/F3_Atmung.html` ist die Ur-Vorlage („Kontrolliertes Atmen – welche Technik wann“, 4 Familien); ihre Studienangaben sind laut `Refs.kt`-Kopf geprüft und korrigiert.

### 1.2 Was es thematisch schon gibt (Thema · Tiefe · Quellen)

| Thema | Wo | Tiefe | Quellen (Lehre / Studien) |
|---|---|---|---|
| Deutung von Ein/Aus/Halten, *bṛṃhaṇa/laṅghana/samāna* | Atemlehre: Phasen, Regeln | mittel (Viniyoga-Systematik, mit `origin`-Hinweis, dass andere Schulen anders gewichten) | Desikachar *Heart of Yoga*, Krishnamacharya; HYP 2,2 |
| Fünf *vāyus* | Atemlehre | kurz, als „Bild der Lehre, keine Anatomie“ | Praśna-Upaniṣad 3 |
| 11 Atemverhältnisse (Langhana, Brmhana, Samana, Feuer, Kapalabhati, Halten voll/leer, Sama Vritti, 1:4:2, Viloma I/II) | Atemlehre: Muster | tief: je Lehre, Typisch, Studien, Übungen | HYP 2,7–2,68; Gheranda 5; YS 1,34; 2,49–2,53; Iyengar; Kuvalayananda / Laborde 2022, Fincham 2023a, Van Diest 2014, Bae 2021, Meehan 2024, Birdee 2023, Balban 2023, Chetry 2026 |
| Sonne/Mond-Nasenlöcher, Tageszeit, Ayurveda-Typen | Atemlehre: Einflüsse | kurz | HYP 2,15 |
| Was *prāṇāyāma* ist (8 Glieder, YS 2,49–2,50) | Pranayama: Grundlagen | kurz, nur Patañjali | YS 2,29; 2,49–2,50 |
| Haltung, Vorbereitung, nüchtern, Nase frei | Pranayama: Grundlagen | kurz (3 Absätze) | Iyengar |
| Progression (*krama*), Prüfstein Ausatmen | Pranayama: Grundlagen | kurz | HYP 2,15–2,17; Desikachar |
| Langsam vs. schnell (Hyperventilation, Nebenwirkungen) | Pranayama: Grundlagen | kurz, gut belegt | Zaccaro 2018, Laborde 2022, Fincham 2023a/b, Mütze 2025 |
| Übergang in Meditation, *kevala kumbhaka* | Pranayama: Grundlagen | kurz | YS 2,52–2,53 |
| Sicherheit (Wasser, Fahren, Kontraindikationen) | Pranayama; `cautions` aller Übungen | gut | Boyd 2015 |
| 10 Pranayama-Techniken mit Herkunft (HYP-Verse) | Pranayama: Techniken | tief | HYP; Iyengar Kap. 20; Sivananda/Integral Yoga (Dirgha) |
| Moderne Methoden: Resonanz/HRV-Biofeedback, Cyclic Sighing, 4-7-8, Box, Wim Hof, Buteyko, Lippenbremse, Nasenatmung (NO), SKY | `BuiltInExercises` (`effectDetail`) | mittel, je Mechanismus + Studienlage; **Herkunft nur beiläufig** (Weil, Huberman, „Navy-SEAL-Folklore“, Art of Living 1980er, Buteyko ohne Datum) | Lehrer 2014, Shaffer 2020, Goessl 2017, Balban 2023, Kox 2014, Cochrane 2020, Bowler 1998, Mayer 2018, Lundberg 1996 u. a. |
| Physiologie-Begriffe | Glossar | Kurzdefinitionen | ohne Quellen (Glossar) |

### 1.3 Wie die Texte gebaut sind (Regeln, die neue Inhalte erben)

- **Kartenstruktur** (`PranayamaTopic` als nächstliegendes Vorbild für Theorie ohne Übung): `title` · `teaser` (ein geschlossener Satz mit finitem Verb, endet auf „.“, ≤ 95 Zeichen; `ContentRulesTest teaser` prüft `lore-teaser` strikt) · 2–3 `paragraphs` à 2–4 Sätze (zusammen ~400–700 Zeichen, TEXT-06 SOLL ≤ 700 für Detail) · `source` als `HintText` (Lehrquelle, Vers deutsch „2,49“).
- **Ton**: Du-Anrede, einladend, Laiensprache (TEXT-04/-05); Fachwort nur im Detail und dann im Glossar; Lehre immer **zuschreibend** („gilt in der Yoga-Lehre als …“, TEXT-13), nie als Wirkung; Studien mit „(Name Jahr)“ und Eintrag in `Refs.kt` (TEXT-12), schwache Evidenz schon im Teaser (TEXT-07); „Ehrlichkeit zeigen, nicht ankündigen“.
- **Typografie** (TEXT-10): „…“, –, geschütztes Leerzeichen vor Einheit und „·“, Dezimalkomma.
- **Sanskrit** (TEXT-14): klein, IAST, zwischen Sternchen; Eigennamen (Techniken, Personen, Texttitel, Schulen) aufrecht. Für ein Geschichtskapitel wäre die Liste zu erweitern (*haṭha*, *rājayoga*, *kevala kumbhaka*, *amṛta*, *bindu*, *nāḍī*…) und eine Regel für Pāli/Tibetisch/Chinesisch nötig (Vorschlag: gleiche Auszeichnung, gängige wissenschaftliche Umschrift: *ānāpānasati*, *gtum-mo* bzw. aufrecht „Tummo“ als Eigenname, *qìgōng* bzw. aufrecht „Qigong“ als eingedeutscht).
- **Heilversprechen** (TEXT-09, `config/health-claims.txt`): Wortgrenzen-Regex, u. a. `heil…`, `lindert`, `therapie`, `therapeutisch`, `prävention`, `nachweislich`, `beugt … vor`, `senkt (den) blutdruck`. Für Geschichte relevant: „Atemtherapie“ als Wort ist an Wortgrenzen vermutlich erlaubt (Treffer nur bei alleinstehendem „Therapie“), aber „Holotrope Therapie“, „Rebirthing-Therapie“, „Heilpraktiker“ nicht; neutral referierte Studienlage ist erlaubt. Jede Ausnahme nur zeilengenau in `health-claims.allow`.
- **Quellen ohne DOI** (Bücher, Indologie) laufen heute als `textSources: List<String>` („Lehre“), nicht als `Reference`. Ein Geschichtskapitel bräuchte dieselbe Trennung: „Literatur“ (Bücher/Indologie, ohne Etikett) vs. „Studien“ (`Refs`).

---

## 2 Kandidaten: Was fehlt und passt

Legende: **Passung** zur App (hoch/mittel/niedrig) · **Umfang** in Karten (je Karte Teaser + 2–3 Absätze, ~500 Wörter) · **Belegbarkeit** · **Risiko** (Health-Claims H, Esoterik E, Aneignung A, Faktenstreit F) · Belege · Kapitelvorschlag.

### 2.1 Geschichte des Yoga und des Pranayama

| # | Kandidat | Passung | Umfang | Belegbarkeit | Risiko | Kapitel |
|---|---|---|---|---|---|---|
| G1 | ***prāṇa* in Veden und Upaniṣaden**: Atem = Leben; Wettstreit der Lebenskräfte (Chāndogya 5,1 / Bṛhadāraṇyaka), Praśna 3 (fünf *vāyus*, in der App schon). Atem als Kraft, noch keine Technik. | hoch (erklärt, woher *prāṇa* und die *vāyus* kommen) | 1 | gut (Indologie, Textstellen) | E gering, wenn als Weltbild der Texte markiert; F: Datierung Upaniṣaden grob 7.–6. Jh. v. Chr. – mit „etwa“ | Neues Kapitel „Geschichte“ |
| G2 | **Patañjali: Pranayama als 4. Glied** – Yogasūtra wahrscheinlich 4./5. Jh. n. Chr. (Maas: ca. 325–425), nicht „2000 v. Chr.“; Yoga dort als Geistesübung, Atem nur 5 Sūtras; Bhagavadgītā 4,29 nennt Atemopfer. | hoch (die App zitiert YS 2,49–2,53 schon, ohne Einordnung) | 1 | gut (Maas 2013; Bryant als Gegenstimme) | F gering (Datierungsstreit nennen) | „Geschichte“; Querverweis aus Pranayama › „Was Pranayama ist“ |
| G3 | **Haṭha-Yoga: Atem als Körpertechnik** – Amṛtasiddhi (11. Jh., buddhistisch-tantrisch) als ältester Haṭha-Text; Haṭhapradīpikā um 1400–1450 als Kompilation (ältestes Manuskript 1496); Gheraṇḍa (17./18. Jh.), Śivasaṃhitā (nach Mallinson 1300–1500); *kumbhaka*, *bandha*, *nāḍī*, *bindu*; Wortbedeutung *haṭha* („Kraft“) nach Birch 2011 – nicht „Sonne-Mond“. | hoch (die App stützt sich massiv auf HYP; der Nutzer weiß nicht, dass das ein spätmittelalterlicher Sammeltext ist) | 1–2 | sehr gut (Mallinson/Singleton 2017; Mallinson & Szántó 2022; Birch 2011/2019; Haṭha Yoga Project SOAS) | E: *nāḍīs*/*bindu* nur als Weltbild; F: Datierungen mit „etwa“ | „Geschichte“ |
| G4 | **Moderner Yoga: Krishnamacharya, Iyengar, Sivananda, Kuvalayananda, Swami Rama** – Mysore 1930er (Singleton 2010: Körperkultur-Einfluss), Kuvalayananda 1924 erste Laborstudien (Röntgen, *uḍḍīyāna*), Sivananda „Science of Pranayama“ 1935, Swami Rama im Menninger-Labor 1970 (Biofeedback-Brücke). Erklärt, warum die App Viniyoga-Begriffe (*bṛṃhaṇa/laṅghana*) nutzt. | hoch | 1–2 | gut (Singleton 2010; De Michelis 2004; IJYT 2025 zu Yoga Mimamsa; Green & Green 1977) | F: Krishnamacharya-Biografie „von Mythen umgeben“ (Yoga Journal) – 7 Jahre Tibet nicht übernehmen; Swami-Rama-Zahlen (Herzstillstand 16 s) nicht als Fakt | „Geschichte“ |
| G5 | **Yoga im Westen / in Deutschland** – Vivekananda „Raja Yoga“ 1896 (Prāṇa als Weltenergie), Theosophie, Boris Sacharow Berlin 1920er–1943 (Lebensreform, Hauer/NS-Nähe), Nachkriegsboom, 1990er Yoga als Fitness, Breathwork-Welle 2020er. | mittel (interessant, aber Stoff weit weg vom Atmen) | 1 | gut (De Michelis 2004; Fuchs 1990; Hauser 2024 Hamburg) | F/A: NS-Kapitel nur knapp, sachlich | „Geschichte“ (eine Karte) oder weglassen |
| G6 | **Mythen-Karte: „Wie alt ist Yoga?“** – 5000 Jahre / Indus-Siegel nicht belegt (Flood: spekulativ); Haltungs-Yoga erst 20. Jh.; „uralte Übung“ als Marketing. | hoch (passt zur Evidenz-Ehrlichkeit) | 1 | gut (Singleton 2010; Mallinson & Singleton 2017) | F gering; A: respektvoll („jung heißt nicht wertlos“) | „Geschichte“, Schlusskarte |

### 2.2 Atem in anderen Traditionen (nur Einordnung)

| # | Kandidat | Passung | Umfang | Belegbarkeit | Risiko | Kapitel |
|---|---|---|---|---|---|---|
| T1 | **Buddhistisch: *ānāpānasati*** (MN 118, 16 Schritte: lang/kurz wahrnehmen, nicht lenken) und **Zen-Atemzählen** (*susokukan*; Dōgen skeptisch, Rinzai bis 10 zählen). Kontrast zu Pranayama: beobachten statt steuern – passt zum Meditations-Tab. | hoch | 1 | gut (Anālayo 2019; SuttaCentral) | E gering | „Geschichte“ › „Andere Wege“ oder Meditations-Tab-Info |
| T2 | **Daoistisch / Qigong**: Zhuangzi „der wahre Mensch atmet aus den Fersen“ (Bild, keine Technik); *xingqi* seit Zhanguo-Zeit; „Qigong“ als Sammelbegriff erst nach 1949 (Liu Guizhen; Palmer 2007). | mittel | ½–1 | gut (Palmer 2007; Wikipedia-Belege zu Xingqi mit Primärstellen) | E: *qì* nur als Begriff der Lehre | zusammen mit T1, T3 eine Karte „Andere Wege“ |
| T3 | **Tibetisch: Tummo** – Kozhevnikov 2013: Kerntemperatur bis 38,3 °C nur bei Geübten mit „kraftvollem Atem“; Wim Hof bezieht sich darauf (Kox 2014). | mittel (Brücke zur Wim-Hof-Übung) | ½ | gut (PLOS ONE 2013) | H gering; E: Visualisierung als Lehre | „Andere Wege“ + Verweis aus Wim-Hof-Detail |
| T4 | **Hesychasmus** (Nikephoros, 13. Jh.; Jesusgebet im Atemrhythmus, Philokalia) und **Sufi-*dhikr*** (Naqshbandi *hosh dar dam*, Atemanhalten *habs-i dam*). | niedrig–mittel (religiös, wenig Praxisbezug) | ½ (ein Absatz) | Hesychasmus gut (Britannica, Ware); Sufi nur devotional/eine russische Arbeit – **dünn** | A/F: Sufi-Attribution unsicher → weglassen oder ein Satz | eine Karte „Atem und Gebet“ oder weglassen |
| T5 | Stoa / „stoisches Atmen“ | – | – | **nicht belegt** (keine antike Atemtechnik überliefert) | F hoch | **weglassen** |

### 2.3 Geschichte der westlichen Atemtherapie und des Breathwork

| # | Kandidat | Passung | Umfang | Belegbarkeit | Risiko | Kapitel |
|---|---|---|---|---|---|---|
| W1 | **Atem im Labor: von Kuvalayananda zu HRV-Biofeedback** – 1924 Yoga Mimamsa; 1970 Swami Rama/Menninger; Vaschillo 2002 Resonanz 0,1 Hz (Verzögerung Baroreflex ~5 s → Periode 10 s); Lehrer & Gevirtz 2014; RESPeRATE. Warum „6 pro Minute“ die Zahl der App ist. | hoch (erklärt die „gut belegte“ Übung historisch) | 1 | sehr gut (Refs schon da: lehrer2014, shaffer2020; neu Vaschillo 2002) | H gering | „Geschichte“ |
| W2 | **Reduziertes Atmen: Buteyko und Papworth** – Buteyko 1950er UdSSR (Hyperventilationstheorie, 1968 erste Studie Leningrad), Papworth-Methode 1960er England (Holloway & West 2007 RCT, 85 Pat., Symptome besser, Lungenfunktion nicht). Zusammen mit dysfunktionaler Atmung (Jones 2013 Cochrane). | hoch (Buteyko ist in der App; Papworth fehlt; Kontext „weniger atmen“ fehlt) | 1 | gut (Cochrane 2020; Thorax 2007; Science-Based Medicine kritisch) | H: „gegen Asthma“ ist Sperrwort → „in Asthma-Studien“ | „Geschichte“ + Querverweis Buteyko-Detail |
| W3 | **Deutsche Atempädagogik: Middendorf** (1910–2009, „Der Erfahrbare Atem“ 1984, Berlin seit 1965; Veening), Schlaffhorst-Andersen (Stimme), Autogenes Training (Schultz 1932) als Kontext. Atem *zulassen* statt steuern – der dritte Weg neben Pranayama und Biofeedback. | mittel–hoch (deutschsprachiges Publikum, erklärt den Begriff „Atemtherapie“) | 1 | mittel (Fachbücher, Institutsseiten; kaum Studien – ehrlich sagen) | H: „Atemtherapie“ als Wort prüfen (Regex); keine Wirkaussagen | „Geschichte“ |
| W4 | **Hyperventilations-Breathwork: Holotropes Atmen und Rebirthing** – Grof 1970er (Hypothesenpapier 2007, keine RCTs; Findhorn stoppte 1993 wegen Krampf-/Psychoserisiko), Orr 1970er (Geburtserinnerung nicht belegt; Todesfall Candace Newmaker 2000 war eine „Rebirthing“-Sitzung ohne Atem-Methode). Einordnung: schnelles Atmen = CO₂-Abfall, starke Erlebnisse, eigenes Risiko (Fincham 2023b schon in Refs). | mittel (die App hat Wim Hof; Nutzer begegnen Breathwork-Studios) | 1 | mittel (Fincham 2023b; Rhinewine & Williams 2007; Wikipedia mit Belegen) | H: Wörter „Therapie“, „heilen“ vermeiden; F: nur Belegtes; Todesfall sachlich oder weglassen | „Geschichte“ oder Pranayama › „Langsam und schnell“ erweitern |
| W5 | **Populäre Muster und ihre Herkunft**: 4-7-8 (Weil, aus Pranayama, „natürliches Beruhigungsmittel“ unbelegt), Box (Divine benannte es nach den SEALs; Grossman „Combat Breathing“; Muster = *sama vṛtti*), Cyclic Sighing (Balban 2023), Wim Hof (Tummo + Kälte, Kox 2014), Physiological Sigh (Li 2016, Huberman). | hoch, aber **schon weitgehend in `effectDetail`** – Mehrwert nur als Zeitleiste | ½ (Zeitleiste) | gut | H gering | Zeitleisten-Karte in „Geschichte“ |

### 2.4 Physiologie vertiefen

| # | Kandidat | Passung | Umfang | Belegbarkeit | Risiko | Kapitel |
|---|---|---|---|---|---|---|
| P1 | **Atemmechanik**: Zwerchfell (N. phrenicus C3–C5), Zwischenrippenmuskeln, Hilfsmuskeln (Scaleni, Sternocleidomastoideus) bei Belastung; Ausatmen in Ruhe passiv; warum „Bauchatmung“ ein Bild ist (Lunge füllt sich als Ganzes – steht schon bei Dirgha). | hoch (Grundlage, fehlt komplett) | 1 | sehr gut (Lehrbuch; Pneumon 2001; Front Physiol 2022) | keins | neues Kapitel „Körper“ (oder Glossar-Ausbau) |
| P2 | **Gasaustausch, CO₂ und der Atemantrieb**: CO₂ steuert den Atemreiz, nicht O₂-Mangel; Bohr-Effekt (1904) ist Lehrbuchphysiologie, aber „CO₂-Toleranz trainieren verbessert O₂-Abgabe“ ist nicht gezeigt (Bowler 1998; Masterarbeit Bedfordshire: Tabellen unvalidiert). | hoch (räumt mit Buteyko-/Freediving-Marketing auf; passt zur Glossar-Definition) | 1 | gut (Physiologie) / für die Ehrlichkeit: Mangel an Belegen selbst benennen | H gering; F: Buteyko-Anhänger widersprechen | „Körper“ |
| P3 | **Nase**: NO aus den Nebenhöhlen (Lundberg 1996, schon), Zelano 2016 (Nasenatmung taktet Hirnrhythmen, Mund nicht; iEEG, Epilepsiepatienten – kleine Studie), Mouth-Taping-Review 2025 (10 Studien, 213 Pers., schwach, Risiko bei Nasenverstopfung). | hoch (Nasenatmung ist Programm der App) | ½–1 | gut | H: „verbessert Gedächtnis“ nicht behaupten | „Körper“ + Nasenatmung-Detail |
| P4 | **Vagus, Baroreflex, RSA** – wie langsames Atmen über Dehnungsrezeptoren der Lunge und den Baroreflex wirkt (Noble & Hochman 2019; Gerritsen & Band 2018 „respiratory vagal nerve stimulation“ als Modell). | mittel (Glossar hat die Begriffe, aber keinen Zusammenhang) | 1 | gut (Modelle; als Modelle kennzeichnen) | H: „stärkt den Vagus“ vermeiden | „Körper“ |
| P5 | **Atemzentrum und Seufzer**: preBötzinger-Komplex (Feldman; Ashhad 2022 Annu Rev Neurosci), Li 2016 (schon in Refs), Atem als „Taktgeber des Gehirns“ (Heck 2017). | mittel | ½ | gut | keins | „Körper“ |
| P6 | **Atem und Gefühl / Interozeption**: Gefühle haben Atemmuster (Bloch 1991; Philippot 2002: Atemmuster rückwärts erzeugen Gefühl – kleine Studien), Interozeption (Khalsa 2018 Roadmap), Angst und Hyperventilation (Meuret 2008/2010: bei Panik eher *weniger* tief atmen, CO₂ anheben). | hoch (erklärt, warum die App bei Panik Techniken ohne Halten empfiehlt) | 1 | gut | H: Panikstörung → „Fachpersonal“; kein „gegen Angst“ (Sperrwort) | „Körper“ |
| P7 | **Schlaf und Atmung** (knapp): Schnarchen/Apnoe als Grund, Mundatmung nachts, warum die App nichts diagnostiziert; Mouth-Taping-Evidenz. | niedrig–mittel | ½ | gut | H/Medizinprodukt: nur Hinweis „ärztlich abklären“ | „Körper“ (ein Absatz in P3) |

### 2.5 Praxis-Theorie

| # | Kandidat | Passung | Umfang | Belegbarkeit | Risiko | Kapitel |
|---|---|---|---|---|---|---|
| X1 | Sitzhaltung, Zeitpunkt (nüchtern), Progression | **schon da** (Pranayama › Haltung, Aufbau) | – | – | – | ggf. um „Stuhl ist in Ordnung“, „Dauer: 5 min reichen für Studien-Effekte (Balban)“ ergänzen |
| X2 | **Typische Fehler**: zu tief atmen (Hypokapnie), Pressen, Schultern heben, Zählen statt Spüren, zu lange Halte, Technik-Hopping. | hoch (fehlt als Sammelkarte) | 1 | gut (aus bestehenden Quellen ableitbar) | keins | Pranayama › Grundlagen (neue Karte) |
| X3 | **Atem und Sport: Nasenatmung beim Laufen** – Dallam 2018 (10 adaptierte Läufer: VO₂max gleich, Ökonomie besser; sehr klein), Dallam & Kies 2020 Review („selten untersucht“); bei harter Belastung Mundatmung normal (steht schon bei Nasenatmung). | mittel | ½–1 | dünn – ehrlich „kaum untersucht“ | H gering | Nasenatmung-Detail erweitern oder „Körper“ |
| X4 | **Atem und Stimme/Sprechen** (Schlaffhorst-Andersen, Zwerchfell beim Sprechen) | niedrig | – | gut | – | weglassen (nicht Kern der App) |

### 2.6 Ethik und Kontext

| # | Kandidat | Passung | Umfang | Belegbarkeit | Risiko | Kapitel |
|---|---|---|---|---|---|---|
| E1 | **Woher die Begriffe kommen – Respekt statt Aneignung**: Sanskrit korrekt (TEXT-14 macht das schon), Lehrer und Texte nennen (tut die App), „Yoga gehört niemandem exklusiv“ (Jain 2015) vs. Kritik an Kommerzialisierung; Haltung der App erklären: Lehre zuschreibend, Studien getrennt. | mittel–hoch (passt als Schlusskarte des Geschichtskapitels, erklärt TEXT-13 dem Nutzer) | 1 | gut (Jain 2015; Singleton 2010; De Michelis 2004) | A: nicht belehren; F: Debatte offen halten | „Geschichte“, Schlusskarte oder „Über“-Seite |
| E2 | **Grenzen einer App, wann Fachpersonal**: keine Diagnose, keine Behandlung; dysfunktionale Atmung/Hyperventilationssyndrom (Nijmegen-Fragebogen, Physiotherapie), Panikstörung (CART/KVT), Asthma/COPD (Atemphysiotherapie), Schlafapnoe (Schlaflabor); Nebenwirkungen schneller Techniken (Mütze 2025). | hoch (Pflicht bei „kein Medizinprodukt“; heute verstreut in `cautions` und Willkommenskarte) | 1 | gut (Cochrane 2013; Meuret; Mütze 2025) | H: Sperrwörter „Therapie“ → „Behandlung bei Fachleuten“, „Atemphysiotherapie“ prüfen | Pranayama › Sicherheit erweitern **oder** eigene Karte im Kapitel „Körper“ |

---

## 3 Empfehlung

### 3.1 Top 8 (priorisiert)

| Prio | Inhalt | Warum zuerst | Aufwand |
|---|---|---|---|
| 1 | **G3 Haṭha-Yoga: Woher die Hatha Yoga Pradipika kommt** | Die App zitiert HYP auf fast jeder Yoga-Karte; ohne Einordnung wirkt ein Sammeltext um 1450 wie „uralte Weisheit“. Beste Quellenlage (Mallinson, Birch, SOAS-Projekt). | 1–2 Karten |
| 2 | **G2 Patañjali: Pranayama als viertes Glied, ehrlich datiert** | Korrigiert die verbreitete Frühdatierung; verankert YS 2,49–2,53, die schon zitiert werden. | 1 Karte |
| 3 | **W1 Atem im Labor: von 1924 bis zur Resonanz-Atmung** | Erzählt die Herkunft der einzigen „gut belegten“ Übung; verbindet Yoga-Geschichte (Kuvalayananda, Swami Rama) mit Biofeedback (Vaschillo, Lehrer). Refs größtenteils vorhanden. | 1 Karte + 1 Ref (Vaschillo 2002) |
| 4 | **P1+P2 Atemmechanik und CO₂** | Einzige echte Lücke im Grundwissen: Wie Atmen körperlich funktioniert und warum CO₂ (nicht O₂) den Atemreiz setzt. Macht Glossar-Begriffe (Hypokapnie, CO₂-Toleranz) erst verständlich; entlarvt „Bohr-Effekt“-Marketing sauber. | 2 Karten, neues Kapitel „Körper“ |
| 5 | **G4 Moderner Yoga: Krishnamacharya, Iyengar, Sivananda** | Erklärt, warum die Atemlehre Viniyoga-Begriffe nutzt und Dirgha „moderne Lehre“ ist; Singletons These (Körperkultur) sachlich referieren. | 1 Karte |
| 6 | **P6 Atem und Gefühl, Panik und Hyperventilation** | Begründet die App-Regel „bei Panik ohne Halten“ und „Fachpersonal“; Meuret zeigt, dass „tief durchatmen“ bei Panik kontraproduktiv sein kann – nützlich und wenig bekannt. | 1 Karte |
| 7 | **E2 Grenzen einer App, wann Fachleute** | Rechtlich und ethisch wichtig (MDR/HWG, Store-Policy); bündelt, was heute in `cautions` verstreut ist. | 1 Karte (Pranayama › Sicherheit ausbauen) |
| 8 | **G6 „Wie alt ist Yoga?“ + E1 Respekt** als Schlusskarte | Setzt den Ton: ehrlich zur Geschichte wie zur Studienlage; kurz, gut belegt, und beantwortet die Nutzerfrage nach „Yoga-Geschichte“ mit dem größten Mythos. | 1 Karte |

Nachrang (gut, aber später): W2 Buteyko/Papworth, W3 Middendorf, T1 *ānāpānasati*/Zen (Brücke zum Meditations-Tab), W4 Holotrop/Rebirthing (eher als Warnung), T2/T3 Daoismus/Tummo, P3 Nase/Zelano, X2 Typische Fehler, X3 Sport. Weglassen: T5 Stoa (unbelegt), T4 Sufi-Details (dünn belegt), X4 Stimme.

### 3.2 Vorschlag: Kapitel „Geschichte des Atmens“ (fünfte `ChapterCard`, Hint „Von den Upaniṣaden bis zur Resonanz-Atmung“)

Technisch wie `Pranayama.topics`: `data/Geschichte.kt` mit `object Geschichte { intro; eras: List<HistoryTopic> }`, `HistoryTopic` = `PranayamaTopic`-Form plus optional `exerciseIds`/`patternId` für „In der App“-Chips; UI nach `PranayamaScreen.TopicCard`; Abschnitte „Indien“, „Andere Wege“, „Der Westen“, „Heute“ als `CollapsibleSection`; unten „Literatur“ (Bücher, ohne Etikett) und „Studien“ (`Refs`). Kopffarbe `headingColor(4)` – prüfen, ob der Kopf-Ring (FARBE-07) fünf Stufen hergibt. Zeitangaben konsequent mit „etwa“ oder „um“; Jahrhunderte statt Jahreszahlen, wo die Indologie streitet.

**Intro-Satz (Idee):** „Wie der Atem zur Übung wurde: Texte, Lehrer und Labore, so gut die Forschung es heute weiß. Was offen ist, steht dabei.“

**Abschnitt „Indien“**

1. **Atem als Leben – *prāṇa* in den Upaniṣaden** (etwa 7.–6. Jh. v. Chr.)
   Teaser: „In den Upaniṣaden ist *prāṇa* der Atem, der alle Lebenskräfte trägt – noch keine Übung.“ Absätze: Wettstreit der Sinne (Chāndogya 5,1): Wer geht, ohne dass der Körper stirbt? Nur der Atem nicht. Praśna 3 teilt *prāṇa* in fünf Strömungen – daher die *vāyus* der Atemlehre (Verweis). Technik ist das noch nicht: Die Texte deuten, sie leiten nicht an. Quelle: Chāndogya-Upaniṣad 5,1; Praśna-Upaniṣad 3; Wiese (Cambridge).

2. **Patañjali – Pranayama als viertes Glied** (etwa 4.–5. Jh. n. Chr.)
   Teaser: „Patañjali macht den Atem zur Stufe auf dem Weg zur Sammlung; fünf Sätze genügen ihm dafür.“ Absätze: Acht Glieder, Atem nach Ort, Zeit, Zahl geregelt, „lang und fein“ (2,49–2,50). Datierung: nach Maas um 325–425 n. Chr., andere datieren früher – die Spanne nennen. Yoga ist hier Geistesschulung; Körperübungen spielen kaum eine Rolle. Quelle: YS 2,29; 2,49–2,53; Maas 2013.

3. **Haṭha – der Atem wird Körpertechnik** (etwa 11.–15. Jh.)
   Teaser: „Erst im Haṭha-Yoga des Mittelalters werden Halten, Verschlüsse und Kanäle zur Methode.“ Absätze: Ältester Text Amṛtasiddhi (11. Jh.) aus buddhistisch-tantrischem Umfeld; *haṭha* heißt „Kraft, Gewalt“ (Birch 2011), nicht „Sonne-Mond“. Die Hatha Yoga Pradipika (um 1450, ältestes Manuskript 1496) sammelt ältere Lehren: acht *kumbhakas*, drei *bandhas*, *nāḍīs* als Kanäle – ein Weltbild der Lehre, keine Anatomie. Später Gheranda Samhita (17./18. Jh.) mit dem Maß 16 : 64 : 32 (Verweis Karte „1 : 4 : 2“). Quelle: Mallinson & Singleton 2017; Mallinson & Szántó 2022; Birch 2011; Haṭha Yoga Project.

4. **Lehrer des 20. Jahrhunderts** (1920er–1980er)
   Teaser: „Krishnamacharya, Iyengar und Sivananda formten das Pranayama, das heute gelehrt wird.“ Absätze: Mysore 1930er: Krishnamacharya verbindet Haṭha mit Patañjali; Singleton zeigt Einflüsse westlicher Körperkultur auf die Haltungen. Seine Schüler: Iyengar (*Light on Pranayama* 1981, Viloma, *sama/viṣama vṛtti*), Desikachar (Viniyoga: *bṛṃhaṇa/laṅghana* – die Begriffe der Atemlehre), Pattabhi Jois. Sivananda in Rishikesh (*Science of Pranayama* 1935), später Vishnudevananda; daher Dirgha. Hinweis: Lebensläufe sind teils legendenhaft (Yoga Journal). Quelle: Singleton 2010; De Michelis 2004.

**Abschnitt „Andere Wege“** (eine oder zwei Karten)

5. **Beobachten statt lenken – Buddha, Zen, Dao**
   Teaser: „Andere Traditionen lenken den Atem nicht, sie schauen ihm zu – ein Weg, den der Meditations-Tab geht.“ Absätze: *ānāpānasati* (MN 118): lang oder kurz, einfach wissen; 16 Schritte. Zen zählt Ausatemzüge bis zehn (Rinzai), Dōgen hielt Zählen für Anfängerstufe. Zhuangzi: „der wahre Mensch atmet aus den Fersen“ – ein Bild; „Qigong“ als Sammelwort erst nach 1949 (Palmer 2007). Tibetisches Tummo: Kerntemperatur stieg bei Geübten mit kraftvollem Atem (Kozhevnikov 2013) – Wim Hof bezieht sich darauf (Verweis). Quelle: Anālayo 2019; Palmer 2007; Kozhevnikov 2013.

6. optional **Atem und Gebet** – Hesychasmus (Nikephoros, 13. Jh.: Jesusgebet im Atemtakt, Philokalia). Nur, wenn sauber kurz; Sufi-Details weglassen (dünn belegt).

**Abschnitt „Der Westen“**

7. **Atem im Labor** (1924–heute)
   Teaser: „Seit 1924 wird Pranayama gemessen; aus dem Labor stammt die Zahl 6 Atemzüge pro Minute.“ Absätze: Kuvalayananda, Yoga Mimamsa 1924 (Röntgen, Druckmessung bei *uḍḍīyāna*). 1970 Swami Rama im Menninger-Labor (Green & Green 1977) – Startpunkt des Biofeedbacks; Zahlenwunder nicht übernehmen. Vaschillo 2002: Baroreflex antwortet mit ~5 s Verzögerung → Resonanz bei 0,1 Hz = 6/min; Lehrer & Gevirtz 2014 (Verweis Resonanz-Atmung). Quelle: IJYT 2025; Green 1977; Vaschillo 2002; Lehrer 2014.

8. **Weniger atmen – Buteyko und Papworth** (1950er–2000er)
   Teaser: „Zwei Methoden setzten aufs Weniger: in Asthma-Studien besserte sich das Befinden, die Lungenfunktion nicht.“ Absätze: Buteyko, UdSSR 1950er, CO₂-Theorie; erste Studie 1968; Bowler 1998: CO₂ änderte sich nicht. Papworth-Methode, England 1960er; Holloway & West 2007: Symptome und Angst besser, Lungenfunktion nicht, unverblindet. Beide ergänzen eine Behandlung, ersetzen sie nicht (Verweis Buteyko-Detail). Quelle: Cochrane 2020; Thorax 2007.

9. **Atem zulassen – deutsche Atempädagogik** (1920er–1980er)
   Teaser: „Ilse Middendorf lehrte, den Atem kommen zu lassen statt ihn zu lenken – ein dritter Weg.“ Absätze: Middendorf (1910–2009), Veening, „Der Erfahrbare Atem“ 1984; drei Atemweisen (willentlich, unbewusst, erfahrbar). Kontext: Autogenes Training (Schultz 1932), Schlaffhorst-Andersen (Stimme). Studien dazu gibt es kaum; der Wert liegt im Spüren. Quelle: Middendorf 1984; Institutsgeschichte.

10. **Schnell atmen als Erlebnis – Holotrop, Rebirthing, Wim Hof** (1970er–heute)
    Teaser: „Seit den 1970ern nutzen Methoden schnelles Atmen für starke Erlebnisse; Studien dazu sind dünn, Risiken nicht.“ Absätze: Grof, Orr; Mechanik = CO₂-Abfall (Fincham 2023b); Findhorn stoppte 1993; „Geburtserinnerung“ nicht belegt. Wim Hof verbindet Tummo-Atmung mit Kälte (Kox 2014; Review 2024 „sehr niedrige“ Evidenz – schon in der App). Regel der App: nie im Wasser, nie allein bei Vorerkrankung (Verweis). Quelle: Fincham 2023b; Rhinewine & Williams 2007; Kox 2014; Almahayni 2024.

**Abschnitt „Heute“**

11. **Zeitleiste der populären Muster** (kompakt, evtl. als Liste statt Karte)
    1896 Vivekananda „Raja Yoga“ · 1924 Yoga Mimamsa · 1935 Sivananda · 1950er Buteyko · 1970 Menninger · 1981 Iyengar · 1980er SKY · 1984 Middendorf · 1990er Weil 4-7-8 · 2002 Vaschillo · 2010er Divine „Box Breathing“ · 2014 Kox (Wim Hof) · 2016 Li (Seufzer) · 2023 Balban (Cyclic Sighing). Jahreszahlen nur, wo sicher; 4-7-8 ohne Jahr („1990er/2000er, Weil“).

12. **Wie alt ist Yoga wirklich? – und wem er gehört**
    Teaser: „„5000 Jahre alt“ ist nicht belegt; Haltungs-Yoga ist jung, Pranayama mittelalterlich – alt sein macht nichts wahr.“ (Teaser kürzen auf ≤ 95 Zeichen.) Absätze: Indus-Siegel (um 2000 v. Chr.) zeigt vielleicht Sitzen, mehr nicht (Flood: spekulativ). Älteste Atemtechniken: Haṭha-Texte ab 11. Jh.; moderne Formen 20. Jh. Respekt: Sanskrit richtig schreiben, Lehrer und Texte nennen, Lehre und Studien trennen – so hält es die App (TEXT-13). Jain 2015: Yoga gehört keiner Gruppe allein, Kommerz ist trotzdem Kritik wert. Quelle: Singleton 2010; Mallinson & Singleton 2017; Jain 2015.

### 3.3 Was bewusst wegbleibt

- **Nicht belegte Altersangaben** („5000 Jahre“, „Patañjali 200 v. Chr.“ als Fakt, Indus-Siegel als Yoga-Beweis); stattdessen Spannen mit „etwa“.
- **Chakren, *kuṇḍalinī*, *nāḍīs*, *prāṇa* als Fakt** – nur als Weltbild der Texte, zuschreibend (TEXT-13), kein Körperbild.
- **Heil- und Reinigungsversprechen aus den Texten** (HYP: „vertreibt Krankheiten“, Kapalabhati „reinigt“) – zitieren nur mit „der Text verspricht“, oder weglassen; Sperrwortliste beachten (`heil…`, `therapie`, `gegen asthma`, `senkt den blutdruck`).
- **Legenden um Lehrer** (7 Jahre Tibet, Herzstillstand 16 s, Kuvalayanandas „Beweise“) – nicht als Fakt.
- **Stoisches Atmen** – nicht überliefert. **Sufi-Atemtechnik** – nur devotional belegt. **Stimme/Sprechatmung** – nicht Kern der App.
- **Tragödien als Aufhänger** (Newmaker 2000) – höchstens ein nüchterner Satz, besser weglassen; die Sicherheitsaussage trägt Fincham 2023b allein.
- **Evidenzstufen für Geschichte** – Geschichtskarten tragen kein Etikett „gut belegt“ (das gilt Übungen); Belegbarkeit zeigt sich in Quellen und Formulierung.
- **Keine Wertung von Traditionen** gegeneinander („Buddhismus ist ehrlicher als Yoga“); unterschiedliche Ziele benennen, fertig.

### 3.4 Offene Punkte für die Umsetzung

- `headingColor`-Ring: reicht er für ein fünftes Kapitel (FARBE-07)? Alternativ „Geschichte“ als Abschnitt in „Atemlehre“ – aber die Seite ist schon lang.
- TEXT-14 um nicht-sanskritische Fachwörter ergänzen (Pāli *ānāpānasati*, Tibetisch, Chinesisch) und neue IAST-Wörter listen (*haṭha*, *rājayoga*, *bindu*, *amṛta*, *kevala kumbhaka*).
- Neue `Refs`: Vaschillo 2002 (doi:10.1023/A:1014587304314), Holloway & West 2007 (doi:10.1136/thx.2006.076430), Kozhevnikov 2013 (doi:10.1371/journal.pone.0058244), Zelano 2016 (doi:10.1523/JNEUROSCI.2586-16.2016), Rhee 2025 (PMC12094774), Meuret 2008 (J Psychiatr Res), Jones 2013 (CD009041), Ashhad 2022 (doi:10.1146/annurev-neuro-090121-014424), Gerritsen & Band 2018 (doi:10.3389/fnhum.2018.00397), Dallam 2018 (IJKSS). Bücher (Mallinson & Singleton 2017, Singleton 2010, De Michelis 2004, Jain 2015, Fuchs 1990, Palmer 2007, Green 1977, Middendorf 1984, Anālayo 2019) als `textSources`.
- `health-claims`-Lauf vor Commit: Wörter „Atemtherapie/Atemtherapeutin“ (Middendorf) testen; falls Treffer, „Atempädagogik/Atemarbeit“ verwenden.
- Jede Karte mit Teaser ≤ 95 Zeichen (Tests `lore-teaser` strikt) – mehrere Ideen oben sind noch zu lang.

---

## 4 Quellen (Links, Stand 2026-10-24)

**Indologie / Yoga-Geschichte**
- Mallinson J. & Singleton M. (2017): *Roots of Yoga*, Penguin. [Wikipedia](https://en.wikipedia.org/wiki/Roots_of_Yoga) · [SOAS Hatha Yoga Project](https://hyp.soas.ac.uk/roots-of-yoga/)
- Mallinson J. & Szántó P.-D. (2022): *The Amṛtasiddhi and Amṛtasiddhimūla*, EFEO. [Verlag](https://publications.efeo.fr/en/livres/966_the-am-tasiddhi-and-am-tasiddhim-la) · [Hatha yoga, Wikipedia](https://en.wikipedia.org/wiki/Hatha_yoga)
- Birch J. (2011): The Meaning of haṭha in Early Haṭhayoga. JAOS 131(4):527–554. [SOAS eprints](https://eprints.soas.ac.uk/23237) · [PDF](https://www.themathesontrust.org/papers/hinduism/Birch-Meaning_of_hatha_in_Early_Hathayoga.pdf)
- Birch J. (2019): The Amaraughaprabodha. J Indian Philos. [Springer](https://link.springer.com/article/10.1007/s10781-019-09401-5)
- Haṭhapradīpikā-Projekt (Oxford/SOAS, krit. Edition 2024). [Oxford](https://yoga.ames.ox.ac.uk/hp/)
- Maas P. A. (2013): A Concise Historiography of Classical Yoga Philosophy. In Franco (Hg.), *Periodization and Historiography of Indian Philosophy*. [Academia](https://www.academia.edu/3520571) · [Interview-Transkript](https://historyofphilosophy.net/yoga-maas/transcript) · [Yoga Sutras, Wikipedia](https://en.wikipedia.org/wiki/Yoga_Sutras_of_Patanjali)
- Singleton M. (2010): *Yoga Body*, OUP. [Wikipedia](https://en.wikipedia.org/wiki/Yoga_Body) · [Rezension](https://acampbell.uk/bookreviews/r/singleton.html)
- De Michelis E. (2004): *A History of Modern Yoga*; Vivekananda *Raja Yoga* 1896. [Wikipedia](https://en.wikipedia.org/wiki/Raja_Yoga_(book))
- Jain A. (2015): *Selling Yoga*, OUP. [OUP](https://academic.oup.com/book/7406) · [Wikipedia](https://en.wikipedia.org/wiki/Selling_Yoga)
- Mallinson J. (2007): *The Shiva Samhita*, YogaVidya; Datierung. [Wikipedia](https://en.wikipedia.org/wiki/Shiva_Samhita)
- Chāndogya 5,1 (Wettstreit der Lebenskräfte): [Wiese, Cambridge](https://www.cambridge.org/core/product/EAE7BA7F5C733D8174D69B71D8DA5C1E/core-reader) · [Übersetzung](https://www.miscellanie.com/allthetexts/hindu/Others/khandogya05.html)
- Indus-Siegel / „5000 Jahre“: [Tales of Times Forgotten](https://talesoftimesforgotten.com/2021/06/01/is-yoga-really-ancient/) (populär, mit Flood/Possehl-Zitaten)
- Krishnamacharya: [Wikipedia](https://en.wikipedia.org/wiki/Tirumalai_Krishnamacharya) · [Yoga Journal](https://www.yogajournal.com/yoga-101/philosophy/krishnamacharya/) (Legenden-Hinweis)
- Sivananda, *Science of Pranayama* 1935: [archive.org](https://ia902307.us.archive.org/21/items/science-of-pranayama/science-of-pranayama.pdf) · [Sivananda yoga, Wikipedia](https://en.wikipedia.org/wiki/Sivananda_yoga)
- Kuvalayananda / Yoga Mimamsa 1924: [Wikipedia](https://en.wikipedia.org/wiki/Swami_Kuvalayananda) · [IJYT 2025 Review](https://ijyt.kglmeridian.com/view/journals/ijyt/35/2025/article-Article2.xml)
- Swami Rama / Menninger 1970: Green E. & A. (1977) *Beyond Biofeedback*. [PDF](https://ashramsofindia.com/wp-content/uploads/2024/07/Beyond-Biofeedback-Green-Green-Searchable.pdf) (Primärquelle; Devotional-Seiten nicht zitieren)
- Yoga in Deutschland: Fuchs C. (1990) *Yoga in Deutschland*, Kohlhammer; Hauser B. (2024, Hamburg). [Wikipedia](https://en.wikipedia.org/wiki/Yoga_in_Germany) · [Hauser Abstract](https://www.conferences.uni-hamburg.de/event/350/contributions/1733) · [Sacharow](https://en.wikipedia.org/wiki/Boris_Sacharow)

**Andere Traditionen**
- Anālayo (2019): *Mindfulness of Breathing*, Windhorse; MN 118. [Wikipedia](https://en.wikipedia.org/wiki/%C4%80n%C4%81p%C4%81nasati_Sutta)
- Zen/Dōgen, Fukanzazengi: [Wikipedia](https://en.wikipedia.org/wiki/Fukan_zazengi)
- Daoismus, *xingqi*: [Wikipedia](https://en.wikipedia.org/wiki/Xingqi_(circulating_breath)) · Palmer D. (2007) *Qigong Fever*, Columbia UP: [History of qigong](https://en.wikipedia.org/wiki/History_of_qigong)
- Kozhevnikov M. et al. (2013): g-Tummo. PLoS ONE 8(3):e58244. [DOAJ](https://doaj.org/article/23f173ec5032446eabde02f5c3da66c8)
- Hesychasmus: [Britannica](https://www.britannica.com/topic/Hesychasm) · [bogoslov.ru (Ware)](https://bogoslov.ru/article/2671134)
- Naqshbandi-Prinzipien (nur Überblick): [Wikipedia](https://en.wikipedia.org/wiki/Eleven_Naqshbandi_principles)

**Westliche Atemarbeit**
- Buteyko: [Wikipedia](https://en.wikipedia.org/wiki/Buteyko_method) · [Science-Based Medicine](https://sciencebasedmedicine.org/buteyko-breathing-technique-nothing-to-hyperventilate-about/)
- Holloway E. & West R. (2007): Papworth method RCT. Thorax 62:1039–42. [UCL PDF](https://discovery-pp.ucl.ac.uk/4490/1/4490.pdf) · [Wikipedia](https://en.wikipedia.org/wiki/Papworth_method)
- Middendorf I. (1984): *Der Erfahrbare Atem*. [Leseprobe/Geschichte](https://content.e-bookshelf.de/media/reading/L-10420325-7c748e1240.pdf) · [Atem-Tonus-Ton](https://www.atem-tonus-ton.com/files/medien/documents/publikationen/fiorenza-letizia-atem-bewegung-stimme.pdf)
- Rhinewine J. & Williams O. (2007): Holotropic Breathwork. J Altern Complement Med. [PubMed 17931070](https://pubmed.ncbi.nlm.nih.gov/17931070/) · [Breathwork (New Age), Wikipedia](https://en.wikipedia.org/wiki/Breathwork_(New_Age))
- Vaschillo E. et al. (2002): HRV biofeedback / Baroreflex. Appl Psychophysiol Biofeedback 27:1–27, doi:10.1023/A:1014587304314. [Resonances review](https://scitepress.org/PublishedPapers/2010/26912/pdf/index.html)
- Box Breathing Herkunft: [TIME (Divine)](https://www.time.com/4316151/breathing-technique-navy-seal-calm-focused) · [Police Magazine (Grossman)](https://www.policemag.com/342171/box-breathing)
- 4-7-8 (Weil): nur Sekundärquellen, Ursprungsjahr unklar. [Sleep Review](https://sleepreviewmag.com/curated/cant-sleep-try-4-7-8-breathing-technique-claims-help-you-nod-off-60-seconds/)

**Physiologie**
- Atemmuskeln: [Pneumon 2001](https://www.pneumon.org/Structure-and-function-of-the-respiratory-muscles,136640,0,2.html) · [Front Physiol 2022](https://www.frontiersin.org/journals/physiology/articles/10.3389/fphys.2022.838414/pdf)
- Zelano C. et al. (2016): J Neurosci 36:12448. [PMC5148230](https://pmc.ncbi.nlm.nih.gov/articles/PMC5148230)
- Ashhad S. et al. (2022): Annu Rev Neurosci 45:223. [PMC9840384](https://pmc.ncbi.nlm.nih.gov/articles/PMC9840384)
- Gerritsen R. & Band G. (2018): Front Hum Neurosci 12:397. [PMC6189422](https://pmc.ncbi.nlm.nih.gov/articles/PMC6189422/)
- Khalsa S. et al. (2018): Interoception roadmap. [PubMed 29884281](https://pubmed.ncbi.nlm.nih.gov/29884281/)
- Bloch S. et al. (1991): Atemmuster und Emotionen. [PubMed 1748589](https://pubmed.ncbi.nlm.nih.gov/1748589/)
- Meuret A. et al. (2008/2010): CART bei Panik. [PMC2982256](https://pmc.ncbi.nlm.nih.gov/articles/PMC2982256) · [PMC3327292](https://pmc.ncbi.nlm.nih.gov/articles/PMC3327292)
- Jones M. et al. (2013): Cochrane CD009041, dysfunktionale Atmung. [Cochrane](https://cochranelibrary.com/cdsr/doi/10.1002/14651858.CD009041)
- Rhee et al. (2025): Mouth taping review, PLOS ONE. [PMC12094774](https://pmc.ncbi.nlm.nih.gov/articles/PMC12094774)
- CO₂-Toleranz-Tabellen unvalidiert (Masterarbeit Bedfordshire): [Research Portal](https://researchportal.beds.ac.uk/en/studentTheses/validity-of-claims-for-efficacy-of-the-o2-and-co2-tolerance-train-3/)
- Dallam G. et al. (2018): Nasal vs oral breathing runners, IJKSS; Dallam & Kies (2020) Review. [Review](https://archive.conscientiabeam.com/index.php/90/article/view/2805)
