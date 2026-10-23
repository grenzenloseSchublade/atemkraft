package app.atemkraft.ui.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import app.atemkraft.BuildConfig
import app.atemkraft.R
import app.atemkraft.cue.tts.VoiceCatalog
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.data.ThirdParty
import app.atemkraft.ui.components.BodyText
import app.atemkraft.ui.components.ExpanderSection
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.NavCard
import app.atemkraft.ui.components.PushScaffold
import app.atemkraft.ui.components.ScreenSubtitle
import app.atemkraft.ui.components.SubLabel
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.DividerSubtle
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes

// Konstante https-Ziele, die der Browser öffnet (SEC-PRIV-02); kein App-Traffic.
private const val REPO_URL = "https://github.com/grenzenloseSchublade/atemkraft"
private const val PRIVACY_URL = "https://github.com/grenzenloseSchublade/atemkraft/blob/main/docs/PRIVACY.md"
private const val LICENSES_URL = "https://github.com/grenzenloseSchublade/atemkraft/blob/main/THIRD_PARTY_LICENSES.md"

/**
 * Über-Seite: App-Name, Version, Kurzbeschreibung, ehrlicher Haftungshinweis, Lizenzen der
 * Fremdbausteine und Stimmen sowie Links zu Quellcode und Datenschutzerklärung.
 * [licensesExpanded] öffnet die Lizenzliste von Anfang an (Screenshot-Test). [lastError] ist die Art
 * eines abgefangenen Hintergrundfehlers ([app.atemkraft.domain.UncaughtErrors]); dann steht unter der
 * Version ein stiller Hinweis, damit der Fehler nicht unbemerkt bleibt.
 */
@Composable
fun AboutScreen(onBack: () -> Unit, licensesExpanded: Boolean = false, lastError: String? = null) {
    // Titel fest im Kopf wie auf allen Unterseiten (Nutzer 2026-10-21). Nur „Über“: Der
    // Markenkopf darunter ergänzt „Atemkraft“, sonst stünde der Name zweimal übereinander.
    PushScaffold(onBack = onBack, title = stringResource(R.string.about_title)) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        // Autor gleich bei der Version: gerade bei manuell geteilten APKs sieht man hier die Herkunft.
        Text(
            text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
        )
        lastError?.let { HintText(stringResource(R.string.about_last_error, it)) }
        Spacer(Modifier.height(Dimens.SectionGap))
        // Zahl aus dem Katalog: Eine neue Übung zählt mit, ohne dass der Text veraltet.
        val count = BuiltInExercises.all.size
        ScreenSubtitle(pluralStringResource(R.plurals.about_body, count, count))
        Spacer(Modifier.height(Dimens.SectionGap))
        Card(modifier = Modifier.fillMaxWidth()) {
            BodyText(stringResource(R.string.about_disclaimer), modifier = Modifier.padding(Dimens.CardPadding))
        }

        // Ohne Browser wirft openUri; dann meldet das angetippte Element es selbst (MUSTER-04).
        var failedUrl by rememberSaveable { mutableStateOf<String?>(null) }
        val noBrowser = stringResource(R.string.about_no_browser)
        fun hintFor(url: String, hint: String) = if (failedUrl == url) "$hint\n$noBrowser" else hint
        val uriHandler = LocalUriHandler.current
        val open: (String) -> Unit = { url -> failedUrl = if (uriHandler.opens(url)) null else url }

        Spacer(Modifier.height(Dimens.ListGap))
        ExpanderSection(
            title = stringResource(R.string.about_licenses_title),
            appetizer = stringResource(R.string.about_licenses_teaser),
            // Einziger Abschnittskopf der Seite: erste Farbe des Rings (FARBE-07).
            accent = headingColor(0),
            initiallyExpanded = licensesExpanded,
        ) { LicenseList(linkLabel = hintFor(LICENSES_URL, stringResource(R.string.about_licenses_link)), onOpen = open) }

        Spacer(Modifier.height(Dimens.SectionGap))
        // Quellcode und Datenschutz als Verweis-Karten wie die Kapitel im Tab „Lernen“: Titel,
        // Untertitel, Symbol „öffnet im Browser“ an der Kante; die ganze Karte öffnet den Browser.
        NavCard(
            title = stringResource(R.string.about_source_title),
            // Die Adresse darf nur nach „/“ umbrechen; sie ist ein einziges langes Wort (LAYOUT-03).
            hint = hintFor(REPO_URL, breakAfterSlashes(REPO_URL.removePrefix(HTTPS))),
            onClick = { open(REPO_URL) },
            external = true,
        )
        Spacer(Modifier.height(Dimens.ListGap))
        NavCard(
            title = stringResource(R.string.about_privacy_link),
            hint = hintFor(PRIVACY_URL, stringResource(R.string.about_privacy_hint)),
            onClick = { open(PRIVACY_URL) },
            external = true,
        )
    }
}

/**
 * Öffnet [url] im Browser; false, wenn das nicht geht. Ohne Browser wirft Compose eine Ausnahme;
 * die fängt die Seite hier ab, statt abzustürzen.
 */
private fun UriHandler.opens(url: String): Boolean = runCatching { openUri(url) }.isSuccess

/**
 * Inhalt des Abschnitts „Lizenzen“: Bausteine in der App, Stimmen, Link auf die volle Liste
 * ([linkLabel], [onOpen] öffnet ihn).
 */
@Composable
private fun LicenseList(linkLabel: String, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
        SubLabel(stringResource(R.string.about_licenses_app), color = MaterialTheme.colorScheme.onBackground)
        ThirdParty.components.forEach { LicenseEntry(it.name, it.license, it.holder) }
        HintText(stringResource(R.string.about_licenses_gpl))

        Spacer(Modifier.height(Dimens.GapTiny))
        SubLabel(stringResource(R.string.about_licenses_voices), color = MaterialTheme.colorScheme.onBackground)
        val nonCommercial = stringResource(R.string.voice_non_commercial)
        VoiceCatalog.all.forEach { spec ->
            val license = if (spec.nonCommercialOnly) "${spec.license}, $nonCommercial" else spec.license
            LicenseEntry(spec.displayName, license, spec.rightsHolder)
        }
        HintText(stringResource(R.string.about_licenses_voices_note))

        LinkRow(label = linkLabel, onClick = { onOpen(LICENSES_URL) })
    }
}

/** Ein Baustein: Name, darunter Lizenz · Rechteinhaber. */
@Composable
private fun LicenseEntry(name: String, license: String, holder: String) {
    Column {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        HintText(stringResource(R.string.about_license_line, license, holder))
    }
}

/**
 * Externer Link als eigene Zeile: Die ganze Zeile ist die Tippfläche, mindestens 48 dp hoch
 * (A11Y-05). Das Ziel steht in `secondary` wie jeder Verweis im Inhalt (FARBE-04), dahinter das
 * Symbol „öffnet im Browser“ – ohne Unterstreichung, wie die übrigen Verweise der App. [onClick]
 * öffnet den Link im Browser.
 */
@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.about_open_link), onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        LinkText(label, MaterialTheme.colorScheme.secondary)
    }
}

/** Beschriftung einer [LinkRow]: [label] in [linkColor], dahinter das Symbol. */
@Composable
private fun LinkText(label: String, linkColor: Color) {
    // Platzhalter = Abstand + Symbol; das Symbol sitzt an seinem Ende.
    val iconWidthSp = with(LocalDensity.current) { (Dimens.GapTiny + Sizes.IconSmall).toSp() }
    val iconHeightSp = with(LocalDensity.current) { Sizes.IconSmall.toSp() }
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = linkColor)) { append(label) }
            // Das Symbol ersetzt ein geschütztes Leerzeichen: Es bleibt beim letzten Wort (kein
            // Umbruch davor), und TalkBack liest nichts dafür.
            appendInlineContent(EXTERNAL_ICON, NBSP)
        },
        inlineContent = mapOf(
            EXTERNAL_ICON to InlineTextContent(Placeholder(iconWidthSp, iconHeightSp, PlaceholderVerticalAlign.TextCenter)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                    Icon(
                        painter = painterResource(R.drawable.ic_open_in_new),
                        contentDescription = null, // dekorativ: Name und Hinweis „im Browser öffnen“ trägt die Zeile
                        tint = linkColor,
                        modifier = Modifier.size(Sizes.IconSmall),
                    )
                }
            },
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
    )
}

/**
 * Adresse, die nur nach einem „/“ umbrechen darf: Ein Leerzeichen ohne Breite hinter jedem „/“
 * erlaubt dort den Umbruch, Wörter wie „grenzenloseSchublade“ bleiben ganz (LAYOUT-03).
 */
private fun breakAfterSlashes(address: String): String = address.replace(SLASH, SLASH + ZWSP)

private const val EXTERNAL_ICON = "extern"
private const val NBSP = "\u00A0"
private const val ZWSP = "\u200B"
private const val SLASH = "/"
private const val HTTPS = "https://"
