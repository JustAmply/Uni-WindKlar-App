package app.feature.faq

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import app.core.ui.theme.WindklarTheme
import app.core.ui.components.WindklarHeader
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import windklar.composeapp.generated.resources.Res
import windklar.composeapp.generated.resources.header_background_quiet

private val ScreenBackground @Composable get() = WindklarTheme.colors.screenBackground
private val PrimaryGreen @Composable get() = WindklarTheme.colors.primaryGreen
private val HeaderEndGreen @Composable get() = WindklarTheme.colors.headerEndGreen
private val DarkGreen @Composable get() = WindklarTheme.colors.darkGreen
private val MutedGreen @Composable get() = WindklarTheme.colors.mutedGreen
private val PaleGreen @Composable get() = WindklarTheme.colors.paleGreen
private val ContactCardEndGreen @Composable get() = WindklarTheme.colors.contactCardEndGreen

@Composable
fun FaqScreen(
    modifier: Modifier = Modifier,
    uiState: FaqUiState = FaqUiState(),
) {
    var expandedQuestionId by rememberSaveable { mutableStateOf(uiState.initialExpandedQuestionId) }
    val groupedQuestions = remember(uiState.questions) {
        uiState.questions.groupBy { it.category }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        FaqHeader()

        Column(
            modifier = Modifier
                .padding(start = 20.dp, top = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            groupedQuestions.forEach { (category, questions) ->
                    FaqCategoryHeader(category = category)

                    questions.forEach { question ->
                        FaqAccordionItem(
                            question = question,
                            expanded = expandedQuestionId == question.id,
                            onClick = {
                                expandedQuestionId = if (expandedQuestionId == question.id) {
                                    null
                                } else {
                                    question.id
                                }
                            },
                        )
                    }
                }

            FaqLimitsCard(
                modifier = Modifier.padding(top = 12.dp),
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun FaqHeader() {
    WindklarHeader(
        title = "Häufige Fragen",
        subtitle = "Hintergründe zu Datenquellen und Berechnungen",
        showDecorativeCircles = false,
        backgroundPainter = painterResource(Res.drawable.header_background_quiet),
        bottomPadding = 24.dp
    )
}

@Composable
private fun FaqCategoryHeader(
    category: FaqCategory,
) {
    Text(
        text = category.title(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 4.dp),
        color = MutedGreen,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun FaqAccordionItem(
    question: FaqQuestionUiModel,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val itemShape = RoundedCornerShape(16.dp)

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .animateContentSize(),
        shape = itemShape,
        color = WindklarTheme.colors.cardBackground,
        shadowElevation = 6.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 72.dp)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FaqIconBubble(icon = question.icon.imageVector())

                Text(
                    text = question.question,
                    modifier = Modifier.weight(1f),
                    color = DarkGreen,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                )

                Icon(
                    imageVector = if (expanded) {
                        Icons.Outlined.KeyboardArrowUp
                    } else {
                        Icons.Outlined.KeyboardArrowDown
                    },
                    contentDescription = if (expanded) "Einklappen" else "Ausklappen",
                    tint = PrimaryGreen,
                    modifier = Modifier.size(20.dp),
                )
            }

            if (expanded) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = PaleGreen,
                ) {
                    Text(
                        text = question.answer,
                        modifier = Modifier.padding(16.dp),
                        color = DarkGreen,
                        fontSize = 14.sp,
                        lineHeight = 23.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun FaqIconBubble(
    icon: ImageVector,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(PaleGreen, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryGreen,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun FaqLimitsCard(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(PaleGreen, ContactCardEndGreen),
                    ),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Grenzen der App",
                color = DarkGreen,
                fontSize = 18.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "WindKlar erklärt öffentliche Stammdaten und geschätzte Wirkungswerte. Der MVP zeigt keine Live-Betriebsursachen, keine bestätigten kommunalen Auszahlungen und keine rechtsverbindlichen Aussagen zu Schall, Schattenwurf oder Artenschutz.",
                modifier = Modifier.padding(top = 8.dp),
                color = MutedGreen,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun FaqCategory.title(): String = when (this) {
    FaqCategory.Basics -> "Grundlagen"
    FaqCategory.LocalBenefit -> "Nutzen vor Ort"
    FaqCategory.Concerns -> "Sorgen & Auswirkungen"
    FaqCategory.DataTrust -> "Daten & Grenzen"
}

private fun FaqQuestionIcon.imageVector(): ImageVector = when (this) {
    FaqQuestionIcon.Wind -> Icons.Outlined.Air
    FaqQuestionIcon.Co2 -> Icons.Outlined.Eco
    FaqQuestionIcon.Operator -> Icons.Outlined.Business
    FaqQuestionIcon.Participation -> Icons.Outlined.Groups
    FaqQuestionIcon.Money -> Icons.Outlined.Payments
    FaqQuestionIcon.Noise -> Icons.Outlined.Hearing
    FaqQuestionIcon.Wildlife -> Icons.Outlined.Pets
    FaqQuestionIcon.Agriculture -> Icons.Outlined.Agriculture
    FaqQuestionIcon.Data -> Icons.Outlined.Storage
    FaqQuestionIcon.Limits -> Icons.Outlined.Info
    FaqQuestionIcon.Warning -> Icons.Outlined.Warning
}
