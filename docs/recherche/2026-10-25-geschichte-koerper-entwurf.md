# Entwurf: Kapitel „Geschichte des Atmens“, Kapitel „Körper“, Karte „Grenzen einer App“

Stand: 2026-10-25. **Status: Entwurf zur Freigabe durch den Eigentümer.** Keine Kotlin-Umsetzung; die Texte sind so geschrieben, dass sie 1 : 1 in die Form von `PranayamaTopic` (`title` · `teaser` · 2–3 `paragraphs` · `source`) passen, ergänzt um optionale `exerciseIds` und Studien aus `Refs`.

**Grundlage:** `docs/recherche/2026-10-24-inhalte-geschichte.md` (Teil 3: Gliederung, Teaser-Ideen, Quellen) und `docs/recherche/2026-10-24-auswertung.md` Abschnitt E. Ton und Form wie `data/Pranayama.kt` und `data/Atemlehre.kt`: du-Anrede, Laiensprache, Lehre zuschreibend („gilt in der Lehre als“), Studien mit „(Name Jahr)“.

**Regeln, gegen die geschrieben und geprüft wurde** (STYLEGUIDE §11):
- TEXT-06/-13: Teaser = ein geschlossener Satz mit finitem Verb, endet auf „.“, ≤ 95 Zeichen (gezählt ohne die Sternchen der Sanskrit-Auszeichnung, wie `ContentRulesTest teaser`); Absätze zusammen SOLL ≤ 700 Zeichen; das Detail beginnt nicht mit dem Teaser.
- TEXT-09: Sperrliste `config/health-claims.txt` an Wortgrenzen, ohne Groß-/Kleinschreibung; keine Heil-, Behandlungs- oder Vorbeugeversprechen, auch nicht als Zitat ohne Einordnung. „Atemtherapie“ vermieden → „Atemarbeit“, „Atempädagogik“.
- TEXT-10: „…“ und ‚…‘, Gedankenstrich –, Dezimalkomma, geschütztes Leerzeichen (U+00A0) zwischen Zahl und Einheit, in „n = 30“ und in Verhältnissen „16 : 64 : 32“ (im Markdown unsichtbar; das Prüfskript unten verifiziert es).
- TEXT-14: Sanskrit klein in IAST zwischen Sternchen; Eigennamen (Personen, Texttitel, Techniken, Schulen) aufrecht. Neu nötige Wörter für die Liste in TEXT-14: *haṭha*, *nāḍīs*, *ānāpānasati* (Pāli), *xíngqì* (Chinesisch, Pinyin). Tibetisch „Tummo“ und eingedeutscht „Qigong“ aufrecht.
- Datierungen mit „etwa“ oder „um“; keine Legenden (Tibet-Jahre, Herzstillstand), keine „5000 Jahre“; Chakren und *nāḍīs* nur als Weltbild der Texte; kein Todesfall als Aufhänger.

**Block-Format je Karte:** `### Titel` · Zeitraum (optional, als Untertitel-Idee) · `Teaser:` mit Zeichenzahl · Absätze · `Quelle:` (Lehre/Literatur) · `In der App:` (Übungen als `exerciseIds`, Seiten als Verweis) · `Studien:` (Refs-Keys; „neu“ = noch nicht in `Refs.kt`, siehe „Neue Quellen“).

---

## Kapitel „Geschichte des Atmens“

Untertitel (Hint der `ChapterCard`): „Von den Upaniṣaden bis zur Resonanz-Atmung“

Intro: „So wurde der Atem zur Übung: in Texten, bei Lehrern und im Labor, so weit die Forschung es heute weiß. Jahreszahlen sind oft Schätzungen; was offen ist, steht dabei.“

Abschnitte (je `CollapsibleSection` mit Teaser):
- „Indien“ – Teaser: „Vom Atem als Lebenskraft zur Technik mit Halten und Maß: etwa 2500 Jahre in vier Karten.“
- „Andere Wege“ – Teaser: „Buddha, Zen und Dao schauen dem Atem zu; Tummo in Tibet lenkt ihn kräftig.“
- „Der Westen“ – Teaser: „Labor, Weniger-Atmen, Zulassen und schnelles Atmen: vier Wege seit 1924.“
- „Heute“ – Teaser: „Eine Zeitleiste der bekannten Muster und die Frage, wie alt Yoga wirklich ist.“

### Indien

### Atem als Leben: *prāṇa* in den Upaniṣaden

Zeitraum: etwa 7.–6. Jahrhundert v. Chr.

Teaser: „In den Upaniṣaden ist *prāṇa* der Atem, der alle Lebenskräfte trägt, aber noch keine Übung.“ (89 Zeichen)
Die ältesten Upaniṣaden entstanden etwa im 7. bis 6. Jahrhundert v. Chr. In der Chāndogya-Upaniṣad (5,1) streiten die Lebenskräfte, wer die wichtigste ist. Sprache, Auge, Ohr und Denken verlassen den Körper nacheinander, und er lebt weiter. Als der Atem gehen will, reißt er alle anderen mit. So wird *prāṇa* zum Sinnbild des Lebens selbst.

Die Praśna-Upaniṣad (3) teilt *prāṇa* in fünf Strömungen. Daher stammen die *vāyus*, die die Seite „Atemlehre“ beschreibt: ein Bild der Lehre, keine Anatomie.

Eine Technik ist das noch nicht. Die Texte deuten den Atem, sie leiten nicht an. Übungen mit Zählen, Halten und Verhältnissen kommen erst viele Jahrhunderte später.

Quelle: Chāndogya-Upaniṣad 5,1; Praśna-Upaniṣad 3; Olivelle: The Early Upaniṣads (1998)
In der App: Atemlehre › Die fünf *vāyus* (Seitenverweis, keine Übung)
Studien: –

### Patanjali: der Atem als viertes Glied

Zeitraum: etwa 4.–5. Jahrhundert n. Chr.

Teaser: „Patanjali macht den Atem zur Stufe auf dem Weg zur Sammlung; fünf Sätze genügen ihm dafür.“ (90 Zeichen)
Das Yoga-Sutra beschreibt acht Glieder des Yoga; *prāṇāyāma* ist das vierte. Fünf Sätze widmet Patanjali dem Atem: Er wird nach Ort, Zeit und Zahl geregelt und „lang und fein“ (2,49–2,50). Yoga ist hier vor allem Schulung des Geistes; Körperhaltungen nehmen wenig Raum ein.

Wann der Text entstand, ist umstritten. Die Indologie datiert ihn heute meist auf etwa 325 bis 425 n. Chr. (Maas 2013); ältere Angaben reichen bis ins 2. Jahrhundert v. Chr. Sicher ist nur: Er ist viele Jahrhunderte jünger als die Upaniṣaden.

Auch die Bhagavadgītā kennt den Atem als Opfer: Manche gießen den Einatem in den Ausatem und umgekehrt und halten beide an (4,29). Eine Anleitung ist das ebenso wenig; wie geübt wird, bleibt dem Lehrer überlassen.

Quelle: Patanjali: Yoga-Sutra 2,29; 2,49–2,53; Bhagavadgītā 4,29; Maas: A Concise Historiography of Classical Yoga Philosophy (2013)
In der App: Pranayama › Was Pranayama ist (Seitenverweis)
Studien: –

### Haṭha: der Atem wird Körpertechnik

Zeitraum: etwa 11.–15. Jahrhundert

Teaser: „Erst im Haṭha-Yoga des Mittelalters werden Halten, Verschlüsse und Kanäle zur Methode.“ (86 Zeichen)
Der älteste bekannte Haṭha-Text ist die Amṛtasiddhi aus dem 11. Jahrhundert, entstanden in einem buddhistisch-tantrischen Umfeld (Mallinson & Szántó 2022). *haṭha* heißt „Kraft, Nachdruck“ (Birch 2011); die Deutung als „Sonne und Mond“ ist eine spätere Volksetymologie.

Die Hatha Yoga Pradipika, die diese App oft zitiert, sammelt ältere Lehren und entstand etwa in der Mitte des 15. Jahrhunderts. Sie beschreibt acht *kumbhakas*, drei *bandhas* und ein Netz von Kanälen (*nāḍīs*), durch die *prāṇa* fließen soll: ein Weltbild der Lehre, keine Anatomie.

Später folgen die Śivasaṃhitā (etwa 14.–15. Jahrhundert) und die Gheranda Samhita (17. oder 18. Jahrhundert), aus der das Maß 16 : 64 : 32 stammt. Was heute als „uralt“ gilt, ist also einige Jahrhunderte alt.

Quelle: Mallinson & Singleton: Roots of Yoga (2017); Mallinson & Szántó: The Amṛtasiddhi (2022); Birch: The Meaning of haṭha in Early Haṭhayoga (2011); Hatha Yoga Pradipika Kap. 2–3
In der App: Atemlehre › Das klassische Maß 1 : 4 : 2 (Seitenverweis); `exerciseIds`: nadi-shodhana, ujjayi, bhramari
Studien: –

### Lehrer des 20. Jahrhunderts

Zeitraum: 1920er–1980er

Teaser: „Krishnamacharya, Iyengar und Sivananda formten das Pranayama, das heute gelehrt wird.“ (85 Zeichen)
In den 1930ern unterrichtete Krishnamacharya am Palast von Mysore. Er verband Haṭha-Techniken mit Patanjali und passte die Übung dem Schüler an. Singleton (2010) zeigt, wie stark die Haltungen dieser Zeit von westlicher Gymnastik und Körperkultur mitgeprägt waren. Vieles in Krishnamacharyas Lebenslauf ist nicht belegt.

Seine Schüler trugen das weiter: B. K. S. Iyengar (Light on Pranayama, 1981) mit Viloma und *sama vṛtti*, T. K. V. Desikachar mit der Viniyoga-Lehre, aus der *bṛṃhaṇa* und *laṅghana* der Atemlehre stammen, Pattabhi Jois mit dem Ashtanga Yoga.

Unabhängig davon lehrte Swami Sivananda in Rishikesh (Science of Pranayama, 1935); seine Schule brachte Yoga in den 1960ern in den Westen, aus ihrem Umfeld stammt die dreiteilige Atmung (Dirgha). Kuvalayananda gründete 1924 das erste Yoga-Labor.

Quelle: Singleton: Yoga Body (2010); De Michelis: A History of Modern Yoga (2004)
In der App: `exerciseIds`: viloma-ein, viloma-aus, dirgha, ujjayi; Atemlehre › Die Grundregeln (Seitenverweis)
Studien: –

### Andere Wege

### Beobachten statt lenken: Buddha, Zen, Dao und Tibet

Teaser: „Andere Traditionen lenken den Atem nicht, sondern schauen ihm zu; Tummo ist die Ausnahme.“ (89 Zeichen)
Der Buddha lehrt in der Rede über die Achtsamkeit auf den Atem (*ānāpānasati*, Majjhima Nikāya 118) sechzehn Schritte. Der erste: Ist der Atem lang, weiß man „er ist lang“; ist er kurz, weiß man „er ist kurz“. Gelenkt wird nichts. Im Zen zählen Anfänger oft die Ausatemzüge bis zehn und beginnen von vorn.

Im Daoismus heißt es bei Zhuangzi, der wahre Mensch atme „bis in die Fersen“: ein Bild für tiefe Ruhe, keine Anleitung. Atemübungen (*xíngqì*) gab es in China seit der Antike; das Sammelwort „Qigong“ entstand erst nach 1949 (Palmer 2007).

Tibetisches Tummo ist der Gegenpol: kräftiges Atmen mit Halten und Vorstellungsbildern. Bei geübten Mönchen stieg die Körpertemperatur messbar, bis 38,3 °C, und nur bei der kraftvollen Form (Kozhevnikov 2013, kleine Studie). Wim Hof bezieht sich auf diese Praxis.

Quelle: Majjhima Nikāya 118 (Anālayo: Mindfulness of Breathing, 2019); Zhuangzi Kap. 6; Palmer: Qigong Fever (2007)
In der App: Tab „Meditation“ (stille Sitzung, Seitenverweis); `exerciseIds`: wim-hof
Studien: kozhevnikov2013 (neu)

### Der Westen

### Atem im Labor: von 1924 zur Resonanz-Atmung

Zeitraum: 1924–heute

Teaser: „Seit 1924 wird Pranayama gemessen; aus dem Labor stammt die Zahl 6 Atemzüge pro Minute.“ (87 Zeichen)
1924 gründete Swami Kuvalayananda in Lonavla das Kaivalyadhama und die Zeitschrift Yoga Mimamsa. Er röntgte Übende und maß den Druck im Bauch bei *uḍḍīyāna*: Yoga sollte sich mit den Mitteln der Wissenschaft prüfen lassen. 1970 ließ sich Swami Rama im Menninger-Institut in Kansas verkabeln; die Versuche gaben der Biofeedback-Forschung Schwung (Green & Green 1977). Einzelne Zahlen von damals sind nicht nachgeprüft.

2002 fanden Vaschillo, Lehrer und Kollegen, dass Puls und Blutdruck bei einem Atemtempo um 0,1 Hz, also etwa 6 Atemzügen pro Minute, besonders stark schwingen. Der Grund ist der Baroreflex, ein Regelkreis des Blutdrucks, der mit etwa 5 s Verzögerung antwortet; langsames Atmen trifft seinen Takt (Lehrer & Gevirtz 2014).

Daraus wurden das HRV-Biofeedback und die Resonanz-Atmung dieser App. Sie ist die Übung mit der besten Studienlage: Eine Auswertung von 223 Studien zeigt, dass langsames Atmen die Herzratenvariabilität hebt (Laborde 2022).

Quelle: Green & Green: Beyond Biofeedback (1977); Alter: Yoga in Modern India (2004)
In der App: `exerciseIds`: resonanz
Studien: vaschillo2002 (neu), lehrer2014, laborde2022

### Weniger atmen: Buteyko und Papworth

Zeitraum: 1950er–2000er

Teaser: „Weniger atmen: In Asthma-Studien besserte sich das Befinden, die Lungenfunktion nicht.“ (86 Zeichen)
Der Arzt Konstantin Buteyko entwickelte seine Methode in den 1950ern in der Sowjetunion. Seine Annahme: Viele Menschen atmen dauerhaft zu viel, und der niedrige CO₂-Wert mache krank. Also übte er reduziertes, ruhiges Nasenatmen mit kurzen Atempausen.

Etwa zur gleichen Zeit entstand in England die Papworth-Methode: Physiotherapeuten lehrten Asthma-Patienten Zwerchfell- und Nasenatmung samt Entspannung. In der ersten kontrollierten Studie (Holloway & West 2007, 85 Personen) berichteten sie weniger Beschwerden und weniger Angst; die Lungenfunktion änderte sich nicht.

Für Buteyko gilt Ähnliches: Lebensqualität besser, Lungenfunktion unklar (Santino 2020, Cochrane); das CO₂ stieg in einer verblindeten Studie nicht (Bowler 1998). Beide Wege ergänzen eine ärztliche Behandlung, sie ersetzen sie nicht.

Quelle: Holloway & West (2007); Santino et al. (2020, Cochrane)
In der App: `exerciseIds`: buteyko, zwerchfell, lippenbremse
Studien: holloway2007 (neu), buteykoCochrane2020, bowler1998

### Atem zulassen: Middendorf und die Atempädagogik

Zeitraum: 1920er–1980er

Teaser: „Ilse Middendorf lehrte, den Atem kommen zu lassen statt ihn zu lenken: ein dritter Weg.“ (87 Zeichen)
Neben dem Lenken der Yoga-Lehre und dem Messen des Labors steht ein dritter Weg: den Atem zulassen. Ilse Middendorf (1910–2009) lernte bei Cornelis Veening und gründete 1965 in Berlin ihr Institut; ihr Buch „Der Erfahrbare Atem“ erschien 1984.

Sie unterscheidet drei Weisen zu atmen: willentlich gelenkt, unbewusst und erfahrbar, also zugelassen und mit Aufmerksamkeit begleitet. Geübt wird sitzend oder in Bewegung, oft mit Dehnungen und Vokalen, ohne Zählen.

Im gleichen Umfeld entstanden die Stimm- und Atemarbeit von Schlaffhorst-Andersen und das Autogene Training von Schultz (1932). Studien zur Atempädagogik gibt es kaum; ihr Wert liegt im Spüren, nicht in Messwerten.

Quelle: Middendorf: Der Erfahrbare Atem (1984)
In der App: Tab „Meditation“ (stille Sitzung, Seitenverweis); `exerciseIds`: zwerchfell
Studien: –

### Schnell atmen als Erlebnis: Holotrop, Rebirthing, Wim Hof

Zeitraum: 1970er–heute

Teaser: „Seit den 1970ern nutzen Methoden schnelles Atmen für starke Erlebnisse; die Belege sind dünn.“ (93 Zeichen)
Stanislav Grof entwickelte in den 1970ern das Holotrope Atmen: lange, schnelle Atmung mit Musik, um veränderte Bewusstseinszustände zu erreichen. Etwa gleichzeitig entstand Leonard Orrs Rebirthing, das mit verbundenem Atmen Geburtserinnerungen wecken soll; dafür gibt es keinen Beleg.

Körperlich ist beides Hyperventilation: Das CO₂ im Blut sinkt, Kribbeln, Schwindel, starke Gefühle und Krämpfe können folgen (Fincham 2023b). Zur Wirkung gibt es vor allem Hypothesen, keine kontrollierten Studien (Rhinewine & Williams 2007).

Wim Hof verband ab den 2000ern schnelles Atmen, Halten und Kälte. Eine Studie zeigte eine gedämpfte Entzündungsreaktion (Kox 2014, 24 Männer); eine Übersicht stuft die Belege insgesamt als sehr niedrig ein (Almahayni 2024). Für alle gilt: nie im oder am Wasser, nicht allein bei Vorerkrankung.

Quelle: Grof & Grof: Holotropic Breathwork (2010); Fincham et al. (2023b)
In der App: `exerciseIds`: wim-hof; Pranayama › Langsam und schnell (Seitenverweis)
Studien: rhinewine2007 (neu), fincham2023b, kox2014, whmReview2024

### Heute

### Zeitleiste

Als Liste (kein Kartenformat), nur Jahre, die sicher sind. Vorschlag für die UI: Jahr fett, dann ein Halbsatz.

Teaser (für den Abschnitt): „Was wann kam, in einer Zeile pro Jahr; nur sichere Jahreszahlen.“ (0 Zeichen)

- **1896** Vivekananda veröffentlicht „Raja Yoga“ und bringt *prāṇa* als Weltenergie in den Westen.
- **1924** Kuvalayananda gründet Kaivalyadhama und die Zeitschrift Yoga Mimamsa: Yoga im Labor.
- **1932** Schultz: Autogenes Training.
- **1935** Sivananda: Science of Pranayama.
- **1950er** Buteyko entwickelt seine Methode in der Sowjetunion; **1960er** Papworth-Methode in England.
- **1965** Middendorf gründet ihr Institut in Berlin.
- **1970** Swami Rama im Menninger-Institut: Anstoß für das Biofeedback.
- **1981** Iyengar: Light on Pranayama.
- **1980er** Sudarshan Kriya (Art of Living).
- **1984** Middendorf: Der Erfahrbare Atem.
- **1998** Bowler: Buteyko erstmals verblindet geprüft.
- **2002** Vaschillo: Resonanz um 0,1 Hz, der Grund für „6 pro Minute“.
- **2007** Holloway & West: erste kontrollierte Studie zur Papworth-Methode.
- **2014** Kox: Wim-Hof-Methode im Labor.
- **2016** Li: der Schaltkreis des Seufzers im Hirnstamm (bei Mäusen).
- **2023** Balban: Cyclic Sighing in einer kontrollierten Studie.

Ohne sicheres Jahr, deshalb nicht in der Leiste: 4-7-8 (Andrew Weil, 1990er oder 2000er), Box-Atmung (Mark Divine, 2010er; das Muster ist *sama vṛtti*).

### Wie alt ist Yoga wirklich, und wem gehört er?

Teaser: „‚5000 Jahre‘ ist nicht belegt: Haltungs-Yoga ist jung, Atemtechniken sind mittelalterlich.“ (90 Zeichen)
Ein Siegel aus der Indus-Kultur (um 2000 v. Chr.) zeigt eine sitzende Figur; ob das Yoga ist, lässt sich nicht sagen, Fachleute nennen die Deutung spekulativ. Die „5000 Jahre“ der Werbung haben keine Quelle. Atem als Lebenskraft ist etwa 2500 Jahre alt, Atemtechniken mit Halten und Maß etwa 1000, Haltungs-Yoga in heutiger Form knapp 100 (Singleton 2010; Mallinson & Singleton 2017).

Jung heißt nicht wertlos, alt heißt nicht wahr. Ob eine Übung etwas bringt, zeigen Erfahrung und Studien, nicht ihr Alter. Deshalb trennt die App Lehre und Studienlage.

Yoga gehört keiner Gruppe allein, meint die Religionswissenschaftlerin Andrea Jain (2015); Kritik an Kommerz und an erfundenen Herkunftsgeschichten ist trotzdem berechtigt. Die App versucht Respekt im Kleinen: Sanskrit richtig schreiben, Lehrer und Texte nennen, nichts behaupten, was die Texte nicht sagen.

Quelle: Singleton: Yoga Body (2010); Mallinson & Singleton: Roots of Yoga (2017); Jain: Selling Yoga (2015)
In der App: Über die App (Seitenverweis)
Studien: –

Quellen des Kapitels (unten auf der Seite, wie bei Atemlehre): „Literatur“ = die Bücher und Aufsätze aus „Neue Quellen › Literatur“, „Studien“ = kozhevnikov2013, vaschillo2002, lehrer2014, laborde2022, holloway2007, buteykoCochrane2020, bowler1998, rhinewine2007, fincham2023b, kox2014, whmReview2024.

---

## Kapitel „Körper“

Untertitel (Hint der `ChapterCard`): „Wie Atmen im Körper funktioniert“

Intro: „Was beim Atmen im Körper geschieht, in Laiensprache und mit Quellen. Vieles ist Lehrbuchwissen; wo es Modell oder Vermutung ist, steht es dabei.“

### Wie die Luft in den Körper kommt

Teaser: „Das Zwerchfell zieht die Luft hinein; ausatmen geschieht in Ruhe von selbst.“ (76 Zeichen)
Die Lunge hat keine eigenen Muskeln. Beim Einatmen spannt sich das Zwerchfell, eine Muskelkuppel unter den Lungen, und senkt sich; die Zwischenrippenmuskeln heben den Brustkorb. Der Raum wird größer, und Luft strömt nach. In Ruhe leistet das Zwerchfell den größten Teil der Arbeit.

Ausatmen braucht in Ruhe keine Kraft: Lunge und Brustkorb federn zurück wie ein losgelassener Luftballon. Erst bei Anstrengung, beim Singen oder bei Übungen wie Kapalabhati pressen die Bauchmuskeln die Luft aktiv hinaus, und Hilfsmuskeln an Hals und Schultern helfen beim Einatmen.

„Bauchatmung“ ist deshalb ein Bild: Luft gelangt nie in den Bauch. Senkt sich das Zwerchfell, weichen die Organe nach vorn, und die Bauchdecke hebt sich. Die Lunge füllt sich dabei als Ganzes; Bauch, Flanken und Brust bei Dirgha lenken die Aufmerksamkeit, nicht die Luft.

Quelle: Lehrbuch der Physiologie (z. B. Silbernagl & Despopoulos: Taschenatlas Physiologie); De Troyer & Boriek: Mechanics of the Respiratory Muscles (2011)
In der App: `exerciseIds`: zwerchfell, dirgha, feueratmung
Studien: –

### CO₂ ist der Atemantrieb

Teaser: „Nicht Sauerstoffmangel treibt den Atem, sondern steigendes CO₂; daher kommt der Lufthunger.“ (91 Zeichen)
Fühler im Hirnstamm messen laufend das Kohlendioxid im Blut, genauer den Säuregrad, den es erzeugt. Steigt CO₂, wächst der Drang zu atmen. Sauerstoff spielt erst eine Rolle, wenn er deutlich fällt. Deshalb kommt der Lufthunger beim Halten vom CO₂, lange bevor Sauerstoff knapp wird.

Wer schneller atmet, als der Körper braucht, atmet CO₂ ab: Hypokapnie. Gefäße im Gehirn verengen sich, Kribbeln, Schwindel und Benommenheit folgen. Das ist der Kern schneller Techniken und der Grund, warum sie nicht ins Wasser gehören: Nach Hyperventilation setzt der Atemreiz zu spät ein (Boyd 2015).

Oft wird der Bohr-Effekt (1904) bemüht: Mehr CO₂ im Gewebe lässt das Blut Sauerstoff leichter abgeben. Das ist Lehrbuch. Dass sich diese Abgabe durch „CO₂-Toleranz-Training“ im Alltag verbessert, ist nicht gezeigt; in Ruhe ist das Blut ohnehin fast voll gesättigt, und unter Buteyko änderte sich das CO₂ nicht (Bowler 1998).

Quelle: Lehrbuch der Physiologie; Bohr, Hasselbalch & Krogh (1904)
In der App: `exerciseIds`: buteyko, wim-hof; Begriffe › CO₂-Toleranz, Hypokapnie (Seitenverweis)
Studien: boyd2015, bowler1998

### Die Nase

Teaser: „Die Nase wärmt, filtert und liefert Stickstoffmonoxid; vieles andere ist kaum untersucht.“ (89 Zeichen)
Die Nase erwärmt und befeuchtet die Luft und filtert Staub. In den Nebenhöhlen entsteht laufend Stickstoffmonoxid (NO), das mit der Luft in die Lunge gelangt und dort Gefäße weitet; bei sechs von acht Gesunden lag der Sauerstoffwert mit Nasenatmung etwa 10 % höher (Lundberg 1996). Summen vervielfacht das NO (Weitzberg 2002).

Hirnstrom-Messungen bei sieben Epilepsie-Patienten zeigten, dass Nasenatmung Rhythmen in Riechhirn, Mandelkern und Hippocampus taktet, Mundatmung nicht; in Aufgaben erkannten Gesunde ängstliche Gesichter beim Einatmen durch die Nase etwas schneller (Zelano 2016). Das ist spannend und klein; „besseres Gedächtnis durch Nasenatmung“ folgt daraus nicht.

Mund zukleben im Schlaf ist ein Trend aus sozialen Medien. Eine Übersicht fand 10 kleine Studien mit 213 Personen: Zwei zeigten weniger Atemaussetzer bei leichter Schlafapnoe, andere nichts; bei verstopfter Nase droht Erstickung (Rhee 2025). Schnarchen mit Atemaussetzern gehört ins Schlaflabor, nicht unter Klebeband.

Quelle: –
In der App: `exerciseIds`: nasenatmung, bhramari
Studien: nasalNo1996, weitzberg2002, zelano2016 (neu), rhee2025 (neu)

### Vagus, Baroreflex, RSA: Modelle für langsames Atmen

Teaser: „Drei Modelle erklären, wie langsames Atmen beruhigt; keines ist abschließend bewiesen.“ (86 Zeichen)
Beim Einatmen schlägt das Herz etwas schneller, beim Ausatmen langsamer. Dieses Schwingen heißt respiratorische Sinusarrhythmie (RSA) und läuft über den Vagusnerv, den Hauptnerv des Ruhe-Systems. Bei langsamem Atmen wird die Schwingung groß; genau das misst die Herzratenvariabilität (HRV).

Das zweite Modell ist der Baroreflex, der Regelkreis des Blutdrucks. Er antwortet mit etwa 5 s Verzögerung; bei etwa 6 Atemzügen pro Minute geraten Atem, Blutdruck und Puls in Resonanz und schwingen gemeinsam (Vaschillo 2002, Lehrer & Gevirtz 2014). Das dritte: Dehnungsfühler der Lunge melden langes Einatmen an den Hirnstamm und dämpfen dort (Noble & Hochman 2019; Gerritsen & Band 2018).

Alle drei sind Modelle: plausibel, in Teilen gemessen, als Ganzes nicht bewiesen. Sicher ist, dass langsames Atmen die HRV hebt (Laborde 2022) und viele sich danach ruhiger fühlen (Zaccaro 2018). „Den Vagus stärken“ ist dagegen eine Redensart: Die HRV zeigt den Zustand von jetzt, keinen trainierten Nerv.

Quelle: –
In der App: `exerciseIds`: resonanz; Begriffe › Vagus, Baroreflex, RSA, HRV (Seitenverweis)
Studien: vaschillo2002 (neu), lehrer2014, noble2019 (neu), gerritsen2018 (neu), laborde2022, zaccaro2018

### Atem und Gefühl: Panik und Hyperventilation

Teaser: „Jedes Gefühl hat sein Atemmuster; bei Panik ist tiefes Durchatmen oft der falsche Weg.“ (86 Zeichen)
Angst atmet schnell und flach, Trauer in Seufzern, Ärger kräftig: Bei 36 Schauspielern ließen sich sechs Grundgefühle an ihrem Atemmuster unterscheiden (Bloch 1991, klein). Umgekehrt kann ein Atemmuster ein Gefühl anstoßen; auch das ist nur in kleinen Studien gezeigt.

Bei Panik atmen viele zu viel: Das CO₂ sinkt, Kribbeln, Schwindel und Herzklopfen folgen und wirken wie Beweise für die Gefahr. Der Rat „tief durchatmen“ verstärkt das oft. In zwei Studien lernten Menschen mit Panikstörung mit einem CO₂-Messgerät, langsamer und flacher zu atmen; die Beschwerden gingen zurück, und der CO₂-Anstieg ging der Besserung voraus (Meuret 2009, 2010; je etwa 40 Personen).

Daraus folgt die Regel der App: Bei akutem Stress langsames Atmen ohne Halten und ohne tiefe Züge, zum Beispiel ein langes Ausatmen. Eine Panikstörung ist eine Erkrankung und gehört in die Hände von Fachleuten; die App begleitet, sie ersetzt keine Behandlung.

Quelle: –
In der App: Anlass „Akuter Stress“ (Seitenverweis); `exerciseIds`: cyclic-sighing, resonanz; Atemlehre › Ein kurz, Aus lang
Studien: bloch1991 (neu), meuret2009 (neu), meuret2010 (neu)

Quellen des Kapitels: „Literatur“ = Silbernagl & Despopoulos; De Troyer & Boriek 2011; Bohr 1904. „Studien“ = boyd2015, bowler1998, nasalNo1996, weitzberg2002, zelano2016, rhee2025, vaschillo2002, lehrer2014, noble2019, gerritsen2018, laborde2022, zaccaro2018, bloch1991, meuret2009, meuret2010 (+ jones2013, muetze2025, falls die Grenzen-Karte hier steht).

---

## Karte „Grenzen einer App: wann Fachleute“

Ort: Pranayama › Grundlagen (nach „Sicherheit“) **oder** Kapitel „Körper“ als Schlusskarte; siehe offene Frage 3.

### Grenzen einer App: wann Fachleute

Teaser: „Die App zeigt Atemübungen; sie stellt keine Diagnose und ersetzt keine Behandlung.“ (82 Zeichen)
Atemkraft ist kein Medizinprodukt. Sie misst nichts, erkennt keine Erkrankung und sagt dir nicht, was dir fehlt. Was sie kann: Übungen zeigen, anleiten und ehrlich einordnen, was Studien dazu wissen.

Manche Beschwerden brauchen Fachleute. Dauerhaft zu schnelles oder unruhiges Atmen mit Kurzatmigkeit, Seufzen und Schwindel (dysfunktionale Atmung): ärztlich abklären, dann Atemphysiotherapie; Atemübungen allein sind dafür kaum untersucht (Jones 2013). Wiederkehrende Panikattacken: Psychotherapie, dort auch Atemtraining mit CO₂-Messung (Meuret 2010). Asthma oder COPD: ärztliche Behandlung und Atemphysiotherapie; Buteyko oder Lippenbremse kommen dazu, nicht an die Stelle. Schnarchen mit Atemaussetzern oder Tagesmüdigkeit: Schlaflabor.

Schnelle Techniken haben eigene Nebenwirkungen: Schwindel, Kribbeln, starke Gefühle, selten Ohnmacht. In Studien mit psychisch Erkrankten traten sie häufiger auf als bei langsamen Techniken (Mütze 2025). Bei Schwindel, Druck oder Atemnot: aufhören, normal weiteratmen; hält es an, ärztlich abklären.

Quelle: –
In der App: Pranayama › Sicherheit (Seitenverweis); `exerciseIds`: buteyko, lippenbremse
Studien: jones2013 (neu), meuret2010 (neu), muetze2025

---

## Neue Quellen

### Refs (Studien, Format wie `Refs.kt`; alle am 2026-10-25 gegen PubMed geprüft)

| Key | Eintrag | Kennung |
|---|---|---|
| `vaschillo2002` | Vaschillo E. et al. (2002): Heart rate variability biofeedback as a method for assessing baroreflex function: a preliminary study of resonance in the cardiovascular system. Appl Psychophysiol Biofeedback 27(1):1–27. Fünf Männer: stärkste Schwingung von Puls und Blutdruck um 0,1 Hz, erklärt als Resonanz im Baroreflex. | doi:10.1023/A:1014587304314 · PMID 12001882 |
| `holloway2007` | Holloway E. A. & West R. J. (2007): Integrated breathing and relaxation training (the Papworth method) for adults with asthma in primary care: a randomised controlled trial. Thorax 62(12):1039–42. 85 Personen: weniger Beschwerden und Angst nach 6 und 12 Monaten, Lungenfunktion unverändert; unverblindet. | doi:10.1136/thx.2006.076430 · PMID 17573445 |
| `jones2013` | Jones M. et al. (2013): Breathing exercises for dysfunctional breathing/hyperventilation syndrome in adults. Cochrane Database Syst Rev 2013(5):CD009041. Nur eine kleine, schlecht berichtete Studie: keine belastbare Aussage zum Nutzen. | doi:10.1002/14651858.CD009041.pub2 · PMID 23728685 |
| `kozhevnikov2013` | Kozhevnikov M. et al. (2013): Neurocognitive and somatic components of temperature increases during g-tummo meditation: legend and reality. PLoS ONE 8(3):e58244. Geübte in Tibet: Achseltemperatur bis 38,3 °C nur bei der kraftvollen Atemform; Westler ohne Visualisierung nur kurz und im Normalbereich. | doi:10.1371/journal.pone.0058244 · PMID 23555572 |
| `zelano2016` | Zelano C. et al. (2016): Nasal Respiration Entrains Human Limbic Oscillations and Modulates Cognitive Function. J Neurosci 36(49):12448–67. Hirnstrom-Messungen bei sieben Epilepsie-Patienten; Nasenatmung taktet Riechhirn, Mandelkern und Hippocampus, Mundatmung nicht; kleine Verhaltenseffekte bei Gesunden. | doi:10.1523/JNEUROSCI.2586-16.2016 · PMID 27927961 |
| `rhee2025` | Rhee J. et al. (2025): Breaking social media fads and uncovering the safety and efficacy of mouth taping in patients with mouth breathing, sleep disordered breathing, or obstructive sleep apnea: A systematic review. PLoS ONE 20(5):e0323643. 10 Studien, 213 Personen: zwei mit weniger Atemaussetzern, sonst kein Unterschied; Erstickungsrisiko bei verstopfter Nase. | doi:10.1371/journal.pone.0323643 · PMID 40397877 |
| `rhinewine2007` | Rhinewine J. P. & Williams O. J. (2007): Holotropic Breathwork: the potential role of a prolonged, voluntary hyperventilation procedure as an adjunct to psychotherapy. J Altern Complement Med 13(7):771–6. Hypothesenpapier, keine kontrollierten Studien. | doi:10.1089/acm.2006.6203 · PMID 17931070 |
| `gerritsen2018` | Gerritsen R. J. S. & Band G. P. H. (2018): Breath of Life: The Respiratory Vagal Stimulation Model of Contemplative Activity. Front Hum Neurosci 12:397. Theoretisches Modell: geregeltes Atmen als „Vagusreizung durch Atmung“. | doi:10.3389/fnhum.2018.00397 · PMID 30356789 |
| `noble2019` | Noble D. J. & Hochman S. (2019): Hypothesis: Pulmonary Afferent Activity Patterns During Slow, Deep Breathing Contribute to the Neural Induction of Physiological Relaxation. Front Physiol 10:1176. Modell: Dehnungsfühler der Lunge und Baroreflex takten bei 0,1 Hz zusammen. | doi:10.3389/fphys.2019.01176 · PMID 31572221 |
| `meuret2009` | Meuret A. E. et al. (2009): Changes in respiration mediate changes in fear of bodily sensations in panic disorder. J Psychiatr Res 43(6):634–41. 35 Personen mit Panikstörung, 4 Wochen Atemtraining mit CO₂-Messung: Der CO₂-Anstieg ging der geringeren Angst vor Körpersymptomen voraus. (Online 2008.) | doi:10.1016/j.jpsychires.2008.08.003 · PMID 18835608 |
| `meuret2010` | Meuret A. E. et al. (2010): Respiratory and cognitive mediators of treatment change in panic disorder: evidence for intervention specificity. J Consult Clin Psychol 78(5):691–704. 41 Personen: Atemtraining mit CO₂-Messung hob das CO₂ von zu niedrig auf normal; Besserung über diesen Weg, kognitives Training über einen anderen. | doi:10.1037/a0019552 · PMID 20873904 |
| `bloch1991` | Bloch S. et al. (1991): Specific respiratory patterns distinguish among human basic emotions. Int J Psychophysiol 11(2):141–54. 36 Schauspieler: sechs Grundgefühle mit unterscheidbaren Atemmustern; kleine Studie. | doi:10.1016/0167-8760(91)90006-J · PMID 1748589 |

Hinweis zu Meuret: Die Recherche nannte „Meuret 2008“; PubMed führt den Artikel in Band 43(6) von 2009 (online Oktober 2008). Vorschlag: im Text „Meuret 2009“.

Bereits in `Refs.kt` und hier genutzt: lehrer2014, laborde2022, zaccaro2018, boyd2015, bowler1998, buteykoCochrane2020, nasalNo1996, weitzberg2002, fincham2023b, kox2014, whmReview2024, muetze2025, li2016 (Zeitleiste), balban2023 (Zeitleiste).

### Literatur (Bücher und Aufsätze ohne DOI, als `textSources`)

- Olivelle P.: The Early Upaniṣads. Annotated Text and Translation. Oxford University Press (1998).
- Maas P. A.: A Concise Historiography of Classical Yoga Philosophy. In: Franco E. (Hg.), Periodization and Historiography of Indian Philosophy. Wien (2013).
- Mallinson J. & Singleton M.: Roots of Yoga. Penguin (2017).
- Mallinson J. & Szántó P.-D.: The Amṛtasiddhi and Amṛtasiddhimūla. EFEO/IFP (2022).
- Birch J.: The Meaning of haṭha in Early Haṭhayoga. Journal of the American Oriental Society 131(4):527–554 (2011).
- Singleton M.: Yoga Body. The Origins of Modern Posture Practice. Oxford University Press (2010).
- De Michelis E.: A History of Modern Yoga. Continuum (2004).
- Alter J. S.: Yoga in Modern India. The Body between Science and Philosophy. Princeton University Press (2004). (Kuvalayananda, Yoga Mimamsa 1924)
- Jain A. R.: Selling Yoga. From Counterculture to Pop Culture. Oxford University Press (2015).
- Anālayo: Mindfulness of Breathing. A Practice Guide and Translations. Windhorse (2019). (Majjhima Nikāya 118)
- Palmer D. A.: Qigong Fever. Body, Science, and Utopia in China. Columbia University Press (2007).
- Green E. & Green A.: Beyond Biofeedback. Delacorte (1977). (Swami Rama, Menninger 1970)
- Middendorf I.: Der Erfahrbare Atem. Eine Atemlehre. Junfermann (1984).
- Grof S. & Grof C.: Holotropic Breathwork. A New Approach to Self-Exploration and Therapy. SUNY Press (2010). (Titel enthält ein Sperrwort – nur als Literaturangabe, nicht im Fließtext; falls der Test auch `textSources` prüft, Titel kürzen auf „Holotropic Breathwork (2010)“.)
- Silbernagl S. & Despopoulos A.: Taschenatlas Physiologie. Thieme (aktuelle Auflage). (Atemmechanik, Atemantrieb, Bohr-Effekt)
- De Troyer A. & Boriek A. M.: Mechanics of the Respiratory Muscles. Comprehensive Physiology 1:1273–1300 (2011). doi:10.1002/cphy.c100009 (DOI aus dem Gedächtnis, vor Einbau prüfen)
- Bohr C., Hasselbalch K. & Krogh A.: Ueber einen in biologischer Beziehung wichtigen Einfluss, den die Kohlensäurespannung des Blutes auf dessen Sauerstoffbindung übt. Skandinavisches Archiv für Physiologie 16:402–412 (1904). (Bohr-Effekt; DOI vor Einbau prüfen)

Zitiert, aber nur in der Recherche belegt (Websuche 2026-10-24, keine Primärquelle gelesen): Zhuangzi Kap. 6 („atmet bis in die Fersen“), Bhagavadgītā 4,29, Veening als Lehrer Middendorfs, Gründungsjahr 1965 des Middendorf-Instituts, Datierung der Śivasaṃhitā. Vor Einbau gegen eine Übersetzung bzw. die Institutsgeschichte prüfen.

---

## Offene Fragen an den Eigentümer

1. **Umfang.** 10 Karten plus Zeitleiste für „Geschichte“ sind viel für eine Seite. Kürzen? Kandidaten zum Zusammenlegen oder Streichen: „Atem zulassen“ (Middendorf) und „Schnell atmen als Erlebnis“ (Holotrop) – beide waren in der Recherche Nachrang. Alternativ „Der Westen“ auf zwei Karten (Labor; Weniger/Zulassen/Schnell in einer).
2. **Kapitel „Körper“ eigenständig** (sechste `ChapterCard`) oder als Abschnitt in „Atemlehre“ oder „Pranayama“? Mit „Geschichte“ und „Körper“ hätte der Tab „Lernen“ sechs Kapitel; der Kopffarben-Ring (`headingColor`, FARBE-07) müsste das hergeben – Frage an den Code-Agenten.
3. **Ort der Grenzen-Karte:** Pranayama › Grundlagen hinter „Sicherheit“ (dort steht die Zielgruppe schon) oder Schlusskarte in „Körper“ (dort stehen Panik, Nase/Schlafapnoe, CO₂ im Kontext)? Vorschlag: Körper, mit Verweis aus „Sicherheit“.
4. **Wörter „Psychotherapie“ und „Atemphysiotherapie“** (Grenzen-Karte): Beide passieren die Sperrliste, weil „therapie“ nur als eigenes Wort gesperrt ist. Sind sie trotzdem erwünscht, oder lieber „Behandlung bei Fachleuten“ und „Atemphysiotherapeutinnen und -therapeuten“ meiden? Die Karte nennt sonst nur Berufsgruppen, keine Methoden.
5. **„Atemkraft ist kein Medizinprodukt“** in der Grenzen-Karte: den Store-Disclaimer (SEC-LEGAL-02) wörtlich übernehmen? Der Wortlaut „diagnostiziert, behandelt, heilt oder verhindert keine Erkrankung“ steht in `health-claims.allow` und wäre so prüfbar; der Entwurf umschreibt ihn bisher.
6. **Zeitleiste:** als Liste im Abschnitt „Heute“ (wie oben) oder als eigene Karte mit Teaser? Einträge ohne sicheres Jahr (4-7-8, Box) nur im Nachsatz, wie vorgeschlagen?
7. **Seitenverweise.** Viele „In der App“-Zeilen zeigen auf Seiten (Atemlehre › *vāyus*, Begriffe › CO₂-Toleranz, Tab „Meditation“), nicht auf Übungen. `PranayamaTopic` kennt nur Text; `LinkChip` nur Übungen. Reicht ein Satz im Text („siehe Seite ‚Atemlehre‘“) oder soll der Code-Agent Seitenlinks ergänzen? Für den Entwurf sind nur die `exerciseIds` sicher umsetzbar.
8. **Teaser der Schlusskarte** nutzt halbe Anführungszeichen ‚5000 Jahre‘ am Satzanfang. In Ordnung, oder lieber ohne Zitat: „Die 5000 Jahre sind nicht belegt: …“?
9. **„mache krank“ (Buteyko-Karte):** Buteykos Annahme ist referiert („Seine Annahme: …“). Reicht die Einordnung, oder soll der Satz neutraler werden („… und das sei die Ursache vieler Beschwerden“)?
10. **Krishnamacharya-Karte** nennt Pattabhi Jois und Ashtanga Yoga, die in der App sonst nicht vorkommen. Streichen (kürzer) oder lassen (vollständiger)?

---

## Selbstprüfung

Geprüft mit einem Skript im Scratchpad (nicht im Repo), das die Logik von `ContentRulesTest` nachbildet: Teaser ohne Sternchen gezählt, Sperrliste als Regex an Wortgrenzen ohne Groß-/Kleinschreibung, Ausnahmen aus `health-claims.allow`; Zahl + Einheit („s“, „min“, „Hz“, „mmHg“, „%“, „°C“) sowie „n = “ nur mit U+00A0.

Ergebnis (nachgeprüft 2026-10-25):

- **Teaser:** 16 Teaser, längster 93 Zeichen (Grenze 95), alle geschlossene Sätze.
- **Sperrliste:** in den Kartentexten 0 Treffer; Treffer gibt es nur in Kopf und offenen Fragen (Regelbeschreibung, Disclaimer-Wortlaut).
- **Zahl + Einheit:** 0 Stellen ohne geschütztes Leerzeichen.

