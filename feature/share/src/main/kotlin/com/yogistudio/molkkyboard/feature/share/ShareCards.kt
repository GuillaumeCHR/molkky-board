package com.yogistudio.molkkyboard.feature.share

import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogistudio.molkkyboard.core.designsystem.component.AvatarGlyph
import com.yogistudio.molkkyboard.core.designsystem.component.ImpactBurst
import com.yogistudio.molkkyboard.core.designsystem.component.MolkkyBoardLogotype
import com.yogistudio.molkkyboard.core.designsystem.theme.BricolageGrotesque
import com.yogistudio.molkkyboard.core.designsystem.theme.Inter
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.designsystem.theme.TeamPalette
import com.yogistudio.molkkyboard.core.stats.Moment

// Couleurs fixes des vignettes (identité « Nordic Bold », en clair quel que soit le thème du téléphone).
private val Birch = Color(0xFFF4EBDD)
private val Surface = Color(0xFFFFFAF2)
private val Forest = Color(0xFF1F4D3A)
private val Saffron = Color(0xFFF2A93B)
private val Coral = Color(0xFFE8604C)
private val Ink = Color(0xFF1B1B18)

/** Largeur de référence d'une vignette : 360 dp à 3 px/dp = 1 080 px, quel que soit le téléphone. */
private const val CARD_WIDTH_DP = 360
private const val CARD_DENSITY = 3f

/**
 * Une vignette prête à capturer, à sa taille réelle en pixels ([ShareFormat]). La densité est fixée (et la
 * taille de police système ignorée) : l'image produite est identique sur tous les téléphones.
 */
@Composable
fun ShareCard(content: ShareContent, template: ShareTemplate, format: ShareFormat, modifier: Modifier = Modifier) {
    val heightDp = CARD_WIDTH_DP * format.height / format.width
    CompositionLocalProvider(LocalDensity provides Density(CARD_DENSITY, 1f)) {
        MolkkyBoardTheme(darkTheme = false, reducedMotion = true) {
            Box(modifier.requiredSize(CARD_WIDTH_DP.dp, heightDp.dp)) {
                when (template) {
                    ShareTemplate.PODIUM -> PodiumCard(content, format)
                    ShareTemplate.SCOREBOARD -> ScoreboardCard(content, format)
                    ShareTemplate.HIGHLIGHT -> HighlightCard(content, format)
                    ShareTemplate.OUCH -> OuchCard(content, format)
                    ShareTemplate.PHOTO -> PhotoCard(content)
                }
            }
        }
    }
}

private fun display(size: TextUnit, color: Color = Ink) =
    TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.ExtraBold,
        fontSize = size,
        color = color,
        lineHeight = size,
    )

private fun body(size: TextUnit, color: Color = Ink, weight: FontWeight = FontWeight.Medium) =
    TextStyle(fontFamily = Inter, fontWeight = weight, fontSize = size, color = color)

private fun teamColor(index: Int): Color = TeamPalette.colors(dark = false).let { it[index % it.size].fill }

@Composable
private fun momentTitle(moment: Moment): String =
    stringResource(
        when (moment) {
            is Moment.Victory -> R.string.moment_victory
            is Moment.Perfect -> R.string.moment_perfect
            is Moment.Record -> R.string.moment_record
            is Moment.Comeback -> R.string.moment_comeback
            is Moment.Twelve -> if (moment.strike) R.string.moment_strike else R.string.moment_twelve
            is Moment.ResetFrom49 -> R.string.moment_reset
            is Moment.Eliminated -> R.string.moment_eliminated
        },
    )

/** Chiffre clé de la vignette (« 50 en 7 lancers · tour 6 »…). */
@Composable
internal fun keyFigure(content: ShareContent): String =
    when (val moment = content.moment) {
        is Moment.Victory -> stringResource(R.string.card_key_victory, content.target, content.throws, content.round)
        is Moment.Perfect -> stringResource(R.string.card_key_perfect)
        is Moment.Record -> stringResource(R.string.card_key_record, moment.throws)
        is Moment.Comeback -> stringResource(R.string.card_key_comeback, moment.deficit)
        is Moment.Twelve -> stringResource(if (moment.strike) R.string.card_key_strike else R.string.card_key_twelve)
        is Moment.ResetFrom49 -> stringResource(R.string.card_key_reset)
        is Moment.Eliminated -> stringResource(R.string.card_key_eliminated)
    }

/**
 * Photo du moment, façon Strava (N1, validé le 2026-10-28) : la photo plein cadre, un voile sombre en bas, le moment,
 * l'équipe et ses chiffres en blanc, puis la date et le logotype, discret. Sans photo, un aplat vert forêt.
 */
@Composable
private fun PhotoCard(content: ShareContent) {
    val context = LocalContext.current
    val date =
        DateUtils.formatDateTime(
            context,
            content.playedAt,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR,
        )
    Box(Modifier.fillMaxSize().background(Forest)) {
        content.photo?.let {
            Image(
                it,
                contentDescription = null,
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    PHOTO_VEIL_START to Color.Transparent,
                    1f to Color.Black.copy(alpha = PHOTO_VEIL),
                ),
            ),
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(momentTitle(content.moment), style = display(32.sp, Color.White).copy(shadow = PhotoShadow))
            content.hero?.let {
                Text(
                    it.name,
                    style = body(18.sp, Color.White, FontWeight.SemiBold).copy(shadow = PhotoShadow),
                    maxLines = 1,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                content.hero?.let { PhotoStat(stringResource(R.string.photo_stat_score), it.score) }
                if (content.throws > 0) PhotoStat(stringResource(R.string.photo_stat_throws), content.throws)
                PhotoStat(stringResource(R.string.photo_stat_round), content.round)
            }
            Text(
                listOfNotNull(date, content.place).joinToString(" · "),
                style = body(12.sp, Color.White.copy(alpha = PHOTO_SOFT_TEXT)).copy(shadow = PhotoShadow),
            )
            Box(
                Modifier
                    .background(
                        Color.White.copy(alpha = PHOTO_LOGO_BACK),
                        RoundedCornerShape(10.dp),
                    ).padding(horizontal = 8.dp),
            ) {
                MolkkyBoardLogotype(fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun PhotoStat(label: String, value: Int) {
    Column {
        Text(
            label,
            style =
                body(
                    10.sp,
                    Color.White.copy(alpha = PHOTO_SOFT_TEXT),
                    FontWeight.SemiBold,
                ).copy(shadow = PhotoShadow),
        )
        Text(value.toString(), style = display(30.sp, Color.White).copy(shadow = PhotoShadow))
    }
}

private const val PHOTO_VEIL_START = 0.3f
private const val PHOTO_VEIL = 0.7f
private const val PHOTO_SOFT_TEXT = 0.85f
private const val PHOTO_LOGO_BACK = 0.92f

// Ombre douce sous les textes blancs : lisibles même sur une photo claire (ciel, sable).
private val PhotoShadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2f), blurRadius = 8f)

/** Signature systématique et discrète : « Comptez vos parties avec », logotype, date (et lieu s'il existe). */
@Composable
private fun Signature(content: ShareContent, dark: Boolean, compact: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val date =
        DateUtils.formatDateTime(
            context,
            content.playedAt,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR,
        )
    val text = if (dark) Birch else Ink
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(listOfNotNull(date, content.place).joinToString(" · "), style = body(11.sp, text.copy(alpha = 0.8f)))
            if (!compact) Text(stringResource(R.string.card_scan), style = body(10.sp, text.copy(alpha = 0.7f)))
            Box(
                Modifier
                    .background(
                        if (dark) Birch else Color.Transparent,
                        RoundedCornerShape(8.dp),
                    ).padding(horizontal = 4.dp),
            ) {
                MolkkyBoardLogotype(fontSize = if (compact) 16.sp else 20.sp)
            }
            if (!compact) {
                Text(
                    stringResource(R.string.card_hashtag),
                    style = body(10.sp, text.copy(alpha = 0.7f), FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun Avatar(team: ShareTeam, size: Dp) {
    Box(Modifier.size(size).background(teamColor(team.colorIndex), CircleShape), contentAlignment = Alignment.Center) {
        AvatarGlyph(team.avatar, team.name, Birch, size)
    }
}

/** Podium : le vainqueur en grand, sa couleur, le chiffre clé, puis les trois premiers sur des marches. */
@Composable
private fun PodiumCard(content: ShareContent, format: ShareFormat) {
    val hero = content.hero
    val accent = hero?.let { teamColor(it.colorIndex) } ?: Forest
    Column(
        Modifier.fillMaxSize().background(Birch).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(momentTitle(content.moment), style = display(if (format == ShareFormat.SQUARE) 40.sp else 52.sp, Forest))
        Spacer(Modifier.height(8.dp))
        val compact = format == ShareFormat.SQUARE
        if (hero != null) {
            if (!compact) Avatar(hero, if (format == ShareFormat.STORY) 96.dp else 64.dp)
            Text(
                hero.name,
                style = display(if (compact) 28.sp else 36.sp, accent),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(keyFigure(content), style = body(16.sp, Ink, FontWeight.SemiBold), textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        Podium(content.teams.take(PODIUM_PLACES), tall = !compact)
        Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
        Signature(content, dark = false, compact = format == ShareFormat.SQUARE)
    }
}

@Composable
private fun Podium(teams: List<ShareTeam>, tall: Boolean) {
    // Ordre visuel 2 – 1 – 3, marches de hauteurs décroissantes.
    val order = listOfNotNull(teams.getOrNull(1), teams.getOrNull(0), teams.getOrNull(2))
    val heights = if (tall) mapOf(0 to 1f, 1 to 0.72f, 2 to 0.5f) else mapOf(0 to 1f, 1 to 0.8f, 2 to 0.65f)
    val max = if (tall) 150.dp else 72.dp
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
    ) {
        order.forEach { team ->
            val rank = teams.indexOf(team)
            Column(Modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    team.name,
                    style = body(13.sp, Ink, FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(team.score.toString(), style = display(22.sp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(max * (heights[rank] ?: 0.5f))
                        .background(teamColor(team.colorIndex), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Text(
                        (rank + 1).toString(),
                        style = display(if (tall) 32.sp else 24.sp, Birch),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

/** Tableau de score : chaque équipe, son score et une jauge vers la cible. */
@Composable
private fun ScoreboardCard(content: ShareContent, format: ShareFormat) {
    val rows = content.teams.take(if (format == ShareFormat.SQUARE) SQUARE_ROWS else MAX_ROWS)
    Column(Modifier.fillMaxSize().background(Forest).padding(24.dp)) {
        Text(momentTitle(content.moment), style = display(44.sp, Saffron))
        Text(keyFigure(content), style = body(15.sp, Birch, FontWeight.SemiBold))
        Spacer(Modifier.height(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
            rows.forEachIndexed { index, team ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text((index + 1).toString(), style = display(20.sp, Birch), modifier = Modifier.width(22.dp))
                    Avatar(team, 32.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            team.name,
                            style = body(14.sp, Birch, FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    8.dp,
                                ).background(Birch.copy(alpha = 0.25f), RoundedCornerShape(4.dp)),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth((team.score.toFloat() / content.target).coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(teamColor(team.colorIndex), RoundedCornerShape(4.dp)),
                            )
                        }
                    }
                    Text(team.score.toString(), style = display(26.sp, if (index == 0) Saffron else Birch))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Signature(content, dark = true, compact = format == ShareFormat.SQUARE)
    }
}

/** Moment fort (record, partie parfaite, « 12 ! ») : un chiffre énorme ou la bulle « 12 ! ». */
@Composable
private fun HighlightCard(content: ShareContent, format: ShareFormat) {
    val moment = content.moment
    // La bulle « 12 ! » est safran : elle se détache sur le vert forêt ; les records restent sur le safran.
    val twelve = moment is Moment.Twelve
    val text = if (twelve) Birch else Ink
    Column(
        Modifier.fillMaxSize().background(if (twelve) Forest else Saffron).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        if (moment is Moment.Twelve) {
            ImpactBurst(
                text = stringResource(R.string.moment_twelve),
                caption = if (moment.strike) stringResource(R.string.moment_strike) else null,
                size = if (format == ShareFormat.SQUARE) 150.dp else 200.dp,
                modifier = Modifier.rotate(-6f),
            )
        } else {
            val compact = format == ShareFormat.SQUARE
            Text(
                momentTitle(moment),
                style = display(if (compact) 30.sp else 40.sp, Forest),
                textAlign = TextAlign.Center,
            )
            val number = (moment as? Moment.Record)?.throws ?: content.throws.takeIf { it > 0 } ?: content.target
            Text(number.toString(), style = display(if (compact) 96.sp else 170.sp, Forest))
        }
        Text(keyFigure(content), style = body(18.sp, text, FontWeight.Bold), textAlign = TextAlign.Center)
        content.hero?.let {
            Spacer(Modifier.height(if (format == ShareFormat.SQUARE) 4.dp else 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (format != ShareFormat.SQUARE) Avatar(it, 44.dp)
                Text(
                    it.name,
                    style =
                        display(
                            if (format ==
                                ShareFormat.SQUARE
                            ) {
                                22.sp
                            } else {
                                28.sp
                            },
                            text,
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.background(Birch, RoundedCornerShape(20.dp)).padding(12.dp)) {
            Signature(content, dark = false, compact = format == ShareFormat.SQUARE)
        }
    }
}

/** Moment malheureux, ton humoristique sans moquerie : autocollant corail penché. */
@Composable
private fun OuchCard(content: ShareContent, format: ShareFormat) {
    Column(
        Modifier.fillMaxSize().background(Birch).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .rotate(-8f)
                .background(Coral, RoundedCornerShape(32.dp))
                .padding(horizontal = 36.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                momentTitle(content.moment),
                style =
                    display(
                        if (format ==
                            ShareFormat.SQUARE
                        ) {
                            64.sp
                        } else {
                            84.sp
                        },
                        Surface,
                    ),
            )
        }
        Spacer(Modifier.height(24.dp))
        content.hero?.let {
            if (format != ShareFormat.SQUARE) Avatar(it, 72.dp)
            Text(
                it.name,
                style = display(32.sp, Ink),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(keyFigure(content), style = body(18.sp, Ink, FontWeight.Bold), textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        Signature(content, dark = false, compact = format == ShareFormat.SQUARE)
    }
}

private const val PODIUM_PLACES = 3
private const val MAX_ROWS = 6
private const val SQUARE_ROWS = 3
