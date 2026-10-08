package app.atemkraft.ui.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import app.atemkraft.BuildConfig
import app.atemkraft.R
import app.atemkraft.cue.tts.VoiceCatalog
import app.atemkraft.data.ThirdParty
import app.atemkraft.ui.components.ExpanderSection
import app.atemkraft.ui.components.PushHeader
import app.atemkraft.ui.components.SubLabel
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

// Konstante https-Ziele, die der Browser öffnet (SEC-PRIV-02); kein App-Traffic.
private const val REPO_URL = "https://github.com/grenzenloseSchublade/atemkraft"
private const val PRIVACY_URL = "https://github.com/grenzenloseSchublade/atemkraft/blob/main/docs/PRIVACY.md"
private const val LICENSES_URL = "https://github.com/grenzenloseSchublade/atemkraft/blob/main/THIRD_PARTY_LICENSES.md"

/**
 * Über-Seite: App-Name, Version, Kurzbeschreibung, ehrlicher Haftungshinweis, Lizenzen der
 * Fremdbausteine und Stimmen sowie Links zu Quellcode und Datenschutzerklärung.
 * [licensesExpanded] öffnet die Lizenzliste von Anfang an (Screenshot-Test).
 */
@Composable
fun AboutScreen(onBack: () -> Unit, licensesExpanded: Boolean = false) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Nur der Pfeil steht fest; der Markenkopf (App-Name, Version) gehört zum Inhalt.
            PushHeader(onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ScreenPadding),
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                )
                Spacer(Modifier.height(Dimens.SectionGap))
                Text(
                    text = stringResource(R.string.about_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(Dimens.SectionGap))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.about_disclaimer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(Dimens.CardPadding),
                    )
                }

                Spacer(Modifier.height(Dimens.ListGap))
                ExpanderSection(
                    title = stringResource(R.string.about_licenses_title),
                    appetizer = stringResource(R.string.about_licenses_teaser),
                    accent = MaterialTheme.colorScheme.primary,
                    initiallyExpanded = licensesExpanded,
                ) { LicenseList() }

                Spacer(Modifier.height(Dimens.SectionGap))
                // Autor + Lizenz + offizielle Quelle: gerade bei manuell geteilten APKs die einzige
                // Stelle, an der Empfänger Herkunft und Original-Repo der App sehen.
                Text(
                    text = stringResource(R.string.about_author),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                )
                LinkRow(
                    prefix = stringResource(R.string.about_source_prefix),
                    label = REPO_URL.removePrefix("https://"),
                    url = REPO_URL,
                )
                LinkRow(label = stringResource(R.string.about_privacy_link), url = PRIVACY_URL)
                Spacer(Modifier.height(Dimens.ScreenBottom))
            }
        }
    }
}

/** Inhalt des Abschnitts „Lizenzen“: Bausteine in der App, Stimmen, Link auf die volle Liste. */
@Composable
private fun LicenseList() {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
        SubLabel(stringResource(R.string.about_licenses_app), color = MaterialTheme.colorScheme.onBackground)
        ThirdParty.components.forEach { LicenseEntry(it.name, it.license, it.holder) }
        Note(stringResource(R.string.about_licenses_gpl))

        Spacer(Modifier.height(Dimens.GapTiny))
        SubLabel(stringResource(R.string.about_licenses_voices), color = MaterialTheme.colorScheme.onBackground)
        val nonCommercial = stringResource(R.string.voice_non_commercial)
        VoiceCatalog.all.forEach { spec ->
            val license = if (spec.nonCommercialOnly) "${spec.license}, $nonCommercial" else spec.license
            LicenseEntry(spec.displayName, license, spec.rightsHolder)
        }
        Note(stringResource(R.string.about_licenses_voices_note))

        LinkRow(label = stringResource(R.string.about_licenses_link), url = LICENSES_URL)
    }
}

/** Ein Baustein: Name, darunter Lizenz · Rechteinhaber. */
@Composable
private fun LicenseEntry(name: String, license: String, holder: String) {
    Column {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Note(stringResource(R.string.about_license_line, license, holder))
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
    )
}

/**
 * Externer Link als eigene Zeile: Die ganze Zeile ist die Tippfläche, mindestens 48 dp hoch
 * (A11Y-05), die Optik bleibt ein unterstrichener Text. Öffnet [url] im Browser.
 */
@Composable
private fun LinkRow(label: String, url: String, prefix: String? = null) {
    val uriHandler = LocalUriHandler.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.about_open_link)) { uriHandler.openUri(url) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = buildAnnotatedString {
                if (prefix != null) append(prefix)
                withStyle(
                    SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline),
                ) { append(label) }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
        )
    }
}
