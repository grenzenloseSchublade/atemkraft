# Spezifikation: Tabs, Zurück und Tab-Leiste

Regel: [STYLEGUIDE MUSTER-08](../STYLEGUIDE.md#muster); Anatomie der Tab-Leiste in [§4.5](../STYLEGUIDE.md#anatomie). Entscheidungen in [DECISIONS.md](../DECISIONS.md). Code: `ui/navigation/`, `MainActivity`, `AppNavGraph.kt`.

## Tabs und Stapel

- Vier Tabs (`TopTab`, daraus Startseiten `TAB_ROOTS`, Markierung und Leiste): Atmen (`ic_breathe`, Material „Air“), Eigene (`ic_bookmark_filled`), Meditation (`ic_meditation`), Lernen (`ic_learn`). Push-Seiten haben einen festen Kopf (`PushHeader`), Tabs ebenso (`ScreenHeader`); Zurück-Pfeil und Kopf-Icons scrollen nie aus dem Bild.
- Ein Tipp auf den markierten Tab führt zu dessen Startseite (`tapTab`: `popBackStack(route, inclusive = false)`, sonst `switchTab`). Ein anderer Tab stellt seinen gemerkten Stapel wieder her (`switchTab`, `restoreState`). Markiert ist immer der Tab, zu dessen Stapel die Seite gehört (`tabOnStack`), auch auf Push-Seiten und wenn ein wiederhergestellter Stapel gleich mit einer Push-Seite beginnt (Eigene › Editor, Meditation, zurück zu Eigene: „Eigene“ bleibt markiert).
- Die Einstellungen bleiben eine zentrale Route; das Logbuch ist eine Push-Seite im Stapel des Tabs, aus dem es geöffnet wurde ([eigene-tab.md](eigene-tab.md)).
- Navigation nur über `@Serializable`-Routen in `ui/navigation/Routes.kt`; unauflösbare Route → `popBackStack()` (CODE-05). Übergänge: `Motion.NavPush` 300 ms Slide + Fade (einmal in `pushComposable<T>()`), `Motion.NavTab` 180 ms Fade.

## Tab-Leiste: auf den vier Tabs immer, in der Tiefe eingeklappt

Zentral im App-Rahmen (`MainActivity`, `TabBarState`, keine Seite tut dafür etwas): Wer von einem Tab in die Tiefe geht (Detailseite, Muster-Editor, Einstellungen, Glossar, Atemlehre, Pranayama, Programme, Logbuch, Über), sieht die Leiste eingeklappt (`HideableTabBar`, Höhe gleitet, MOTION-04; die Mini-Leiste einer laufenden Session bleibt). Klappt der Nutzer sie über den Knopf auf, bleibt sie auf den nächsten Seiten der Tiefe offen – vor und zurück –, bis er wieder auf einem Tab steht; ebenso bleibt sie zu, wenn er sie zuklappt. `enter` erfährt bei jedem Seitenwechsel, ob die neue Seite ein Tab ist (`isTabRoot`) und zu welchem Tab ihr Stapel gehört (`tabOnStack`): Ein Tab oder ein Tab-Wechsel, der in einem wiederhergestellten Stapel in der Tiefe landet, gilt als neuer Anfang. Keine Scroll-Messung: Scrollen ändert an der Leiste nichts, nur der Knopf.

**Knopf `TabBarToggle`:** keine eigene Zeile, eingeklappt gewinnt der Inhalt die ganze Leiste. Ein kleiner Reiter („Lasche“, `TabBarHandleWidth` 56 × `TabBarHandleHeight` 18, `TabBarHandleShape` oben gerundet) in der Farbe der Leiste, aus der er wächst – Startleiste bzw. Tab-Leiste (`TabBarContainer`) –, mittig auf der Oberkante der untersten Leiste: der Startleiste, wenn die Seite eine hat (`PinnedActionBar` meldet ihre Oberkante über `LocalTabBarState` an, der einzige eigene CompositionLocal), sonst der unteren Leisten. Er bleibt in der Tiefe immer da: ⌃ „Tab-Leiste zeigen“, offen ⌄ „Tab-Leiste ausblenden“. Tippfläche so breit wie der Reiter und `MinTouchTarget` hoch, drei Viertel über der Kante, nur `ListGap` (das Polster der Startleiste) darunter – der Start-Button bleibt frei.

## Tab-Leiste selbst (`AppNavigationBar`, `AppTabBar`)

M3 `NavigationBar` (Höhe 80, Indikator 64 × 32 fest in M3 1.3.1, S-37), Trennlinie darüber (`BorderThin`, `primary` mit `TAB_DIVIDER_ALPHA`; Abweichung KOMP-03, Marken-Trenner). Auswahl tonal (`secondaryContainer`, Icon und Beschriftung `primary`, PRIN-02). Eine Beschriftungsgröße für alle Tabs, gleiches Gewicht in jedem Zustand (TYPO-01); verkleinert höchstens auf 85 % der Rolle (`NAV_LABEL_MIN_SCALE`), passt es dann nicht, nur Icons mit dem Namen als `contentDescription`. Auch im `ScreenshotHarness` (`TabFrame`).

## Prüfung

`TabNavigationTest` (Re-Tap aus Einstellungen › Glossar zur Tab-Startseite für alle vier Tabs, auf der Startseite ändert er nichts; markiert bleibt der Tab des Stapels; fest steht die Leiste nur auf den Startseiten), `ScreenshotTest` (Tabs mit `TabFrame`, Seiten der Tiefe mit `tabBar = false`), manuell R-VISUAL.
