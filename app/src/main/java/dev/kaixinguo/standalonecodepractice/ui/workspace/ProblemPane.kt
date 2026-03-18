package dev.kaixinguo.standalonecodepractice.ui.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import dev.kaixinguo.standalonecodepractice.ui.theme.AccentBlue
import dev.kaixinguo.standalonecodepractice.ui.theme.AccentGreen
import dev.kaixinguo.standalonecodepractice.ui.theme.AccentRed
import dev.kaixinguo.standalonecodepractice.ui.theme.CardBackground
import dev.kaixinguo.standalonecodepractice.ui.theme.CardBorder
import dev.kaixinguo.standalonecodepractice.ui.theme.DividerColor
import dev.kaixinguo.standalonecodepractice.ui.theme.PaneBackground
import dev.kaixinguo.standalonecodepractice.ui.theme.TextMuted
import dev.kaixinguo.standalonecodepractice.ui.theme.TextPrimary
import dev.kaixinguo.standalonecodepractice.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun ProblemPane(
    problem: ProblemListItem,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(SupportTab.Problem) }
    var activeJobLabel by remember { mutableStateOf<String?>(null) }
    var revealedHintCount by remember { mutableStateOf(0) }
    var customTestCode by remember(problem.id) { mutableStateOf(problem.customTests) }
    var resultTitle by remember { mutableStateOf("Ready") }
    var resultMessage by remember { mutableStateOf("Run or submit your solution to see validation details here.") }
    var resultAccent by remember { mutableStateOf(TextSecondary) }
    val scope = rememberCoroutineScope()
    val hints = remember(problem.id) { problem.hints }

    Surface(
        modifier = modifier,
        color = PaneBackground,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = CardBackground,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SupportTab.entries.forEach { tab ->
                            SupportTabChip(
                                label = tab.label,
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab }
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        ActionChip(
                            label = if (activeJobLabel == "Submit") "Submitting..." else "Submit",
                            active = activeJobLabel == "Submit",
                            emphasized = true,
                            enabled = activeJobLabel == null,
                            onClick = {
                                if (activeJobLabel != null) return@ActionChip
                                activeJobLabel = "Submit"
                                selectedTab = SupportTab.Results
                                resultTitle = "Submitting"
                                resultMessage = "Running the Python solution against the full local validation set."
                                resultAccent = AccentBlue
                                scope.launch {
                                    delay(1500)
                                    resultTitle = "Submission Accepted"
                                    resultMessage = "All local validation cases passed. This attempt is now saved as your latest submitted solution."
                                    resultAccent = AccentGreen
                                    activeJobLabel = null
                                }
                            }
                        )
                    }
                    HorizontalDivider(color = DividerColor)
                }
            }

            when (selectedTab) {
                SupportTab.Problem -> {
                    CardBlock(title = "Problem", modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(AccentGreen, AccentBlue)
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.height(0.dp))
                            Text(
                                text = problem.title,
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = problem.summary,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    CardBlock(title = "Examples", modifier = Modifier.fillMaxWidth()) {
                        ExampleValueBlock(
                            label = "Input",
                            value = problem.exampleInput,
                            labelColor = AccentRed,
                            valueColor = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        ExampleValueBlock(
                            label = "Output",
                            value = problem.exampleOutput,
                            labelColor = AccentGreen,
                            valueColor = TextPrimary
                        )
                    }

                    CardBlock(
                        title = "Hints",
                        modifier = Modifier.fillMaxWidth(),
                        trailing = {
                            PlainActionChip(
                                when {
                                    revealedHintCount == 0 -> "Show"
                                    revealedHintCount < hints.size -> "Next"
                                    else -> "Reset"
                                }
                            ) {
                                revealedHintCount = if (revealedHintCount < hints.size) {
                                    revealedHintCount + 1
                                } else {
                                    0
                                }
                            }
                        }
                    ) {
                        if (revealedHintCount > 0) {
                            hints.take(revealedHintCount).forEachIndexed { index, hint ->
                                HintLine(number = index + 1, text = hint)
                                if (index < revealedHintCount - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        color = DividerColor
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Hints are hidden until you reveal them.",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                SupportTab.Custom -> {
                    CardBlock(
                        title = "Custom Tests",
                        modifier = Modifier.fillMaxWidth(),
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PlainActionChip("Add")
                                ActionChip(
                                    label = if (activeJobLabel == "Custom") "Checking..." else "Run Custom",
                                    active = activeJobLabel == "Custom",
                                    emphasized = false,
                                    enabled = activeJobLabel == null,
                                    onClick = {
                                        if (activeJobLabel != null) return@ActionChip
                                        activeJobLabel = "Custom"
                                        selectedTab = SupportTab.Results
                                        resultTitle = "Checking Custom Cases"
                                        resultMessage = "Evaluating the current Python draft against your local unit tests."
                                        resultAccent = AccentBlue
                                        scope.launch {
                                            delay(1100)
                                            resultTitle = "Custom Run Passed"
                                            resultMessage = "2 local cases passed with no runtime errors."
                                            resultAccent = AccentGreen
                                            activeJobLabel = null
                                        }
                                    }
                                )
                            }
                        }
                    ) {
                        Text(
                            text = "Edit your local cases here. This stays offline and never touches submit validation.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        EditableSupportTextBlock(
                            value = customTestCode,
                            onValueChange = { customTestCode = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    CardBlock(title = "Case Notes", modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Use this space for custom edge cases, expected outputs, and quick assertions you want to revisit.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                SupportTab.Results -> {
                    CardBlock(
                        title = "Results",
                        modifier = Modifier.fillMaxWidth(),
                        trailing = {
                            StatusBadge(
                                label = activeJobLabel ?: resultTitle,
                                color = resultAccent
                            )
                        }
                    ) {
                        Text(
                            text = resultTitle,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = resultMessage,
                            color = if (activeJobLabel == null) TextPrimary else TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        ExampleValueBlock(
                            label = "Last Check",
                            value = if (activeJobLabel == "Submit") {
                                "Compiling local Python runtime\nExecuting hidden validation cases\nSaving submitted draft"
                            } else if (activeJobLabel == "Custom") {
                                "Loading custom inputs\nExecuting Python draft\nCollecting local output"
                            } else if (resultTitle == "Submission Accepted") {
                                "Status: Accepted\nCases: 24 / 24\nSaved: latest local submission"
                            } else if (resultTitle == "Custom Run Passed") {
                                "Status: Passed\nCases: 2 / 2\nScope: custom run"
                            } else {
                                "Status: Idle\nCases: --\nScope: none"
                            },
                            labelColor = AccentBlue,
                            valueColor = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

