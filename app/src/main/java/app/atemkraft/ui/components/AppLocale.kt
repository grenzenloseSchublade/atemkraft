package app.atemkraft.ui.components

import java.util.Locale

/**
 * Die eine Locale der App (TEXT-03): Atemkraft ist einsprachig Deutsch. Wochentage, Monate und
 * Datumsmuster werden immer deutsch formatiert, auch wenn das Gerät auf eine andere Sprache
 * eingestellt ist – sonst stünde „Monday“ neben deutschem Text. Ein Ort statt verstreuter
 * `Locale.GERMAN`; der Build liefert dazu nur deutsche Ressourcen (`localeFilters`, `locales_config.xml`).
 */
val AppLocale: Locale = Locale.forLanguageTag("de")
