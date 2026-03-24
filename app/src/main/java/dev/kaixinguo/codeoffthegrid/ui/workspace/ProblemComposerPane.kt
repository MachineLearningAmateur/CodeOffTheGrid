package dev.kaixinguo.codeoffthegrid.ui.workspace

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.kaixinguo.codeoffthegrid.R
import dev.kaixinguo.codeoffthegrid.data.ProblemShareCodec
import dev.kaixinguo.codeoffthegrid.ui.theme.AccentBlue
import dev.kaixinguo.codeoffthegrid.ui.theme.AccentGreen
import dev.kaixinguo.codeoffthegrid.ui.theme.AccentRed
import dev.kaixinguo.codeoffthegrid.ui.theme.CardBackgroundAlt
import dev.kaixinguo.codeoffthegrid.ui.theme.CardBorder
import dev.kaixinguo.codeoffthegrid.ui.theme.InsetSurface
import dev.kaixinguo.codeoffthegrid.ui.theme.PaneBackground
import dev.kaixinguo.codeoffthegrid.ui.theme.TextMuted
import dev.kaixinguo.codeoffthegrid.ui.theme.TextPrimary
import dev.kaixinguo.codeoffthegrid.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ProblemComposerPane(
    session: ProblemComposerSession,
    destinations: List<ProblemComposerDestination>,
    onSessionChange: (ProblemComposerSession) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf(false) }
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        importing = true
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val fileName = context.resolveDisplayName(uri)
                    val serialized = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: throw IllegalArgumentException("Unable to read the selected problem file.")
                    ImportedComposerFile(
                        fileName = fileName,
                        draft = session.draft.withImportedProblem(
                            ProblemShareCodec.decodeFromString(serialized)
                        )
                    )
                }
            }
            importing = false
            result.onSuccess { imported ->
                onSessionChange(
                    session.copy(
                        tab = ProblemComposerTab.Import,
                        draft = imported.draft,
                        importedFileName = imported.fileName,
                        errorMessage = null
                    )
                )
            }.onFailure { throwable ->
                onSessionChange(
                    session.copy(
                        tab = ProblemComposerTab.Import,
                        errorMessage = throwable.message ?: "Problem import failed."
                    )
                )
            }
        }
    }

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
            CardBlock(
                title = "Problem Composer",
                modifier = Modifier.fillMaxWidth(),
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlainActionChip("Cancel", iconRes = R.drawable.ic_close, accentColor = AccentRed) {
                            onCancel()
                        }
                        PlainActionChip("Save", iconRes = R.drawable.ic_plus, accentColor = AccentGreen) {
                            onSave()
                        }
                    }
                }
            ) {
                Text(
                    text = "Create a new problem or import one from a shared app JSON file. The preview updates live on the right.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (session.errorMessage != null) {
                    CardBlock(title = "Needs Attention", modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = session.errorMessage,
                            color = AccentRed,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                CardBlock(title = "Mode", modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProblemComposerTab.entries.forEach { tab ->
                            SupportTabChip(
                                label = tab.label,
                                selected = session.tab == tab,
                                onClick = {
                                    onSessionChange(
                                        session.copy(
                                            tab = tab,
                                            errorMessage = null
                                        )
                                    )
                                }
                            )
                        }
                    }
                }

                if (session.tab == ProblemComposerTab.Import) {
                    CardBlock(
                        title = "Import",
                        modifier = Modifier.fillMaxWidth(),
                        trailing = {
                            ActionChip(
                                label = if (importing) "Importing..." else "Choose File",
                                active = importing,
                                emphasized = false,
                                enabled = !importing,
                                onClick = {
                                    runCatching {
                                        filePicker.launch(arrayOf("application/json", "text/plain"))
                                    }.onFailure { throwable ->
                                        onSessionChange(
                                            session.copy(
                                                errorMessage = throwable.message
                                                    ?: "Unable to open the system file picker on this device."
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    ) {
                        Text(
                            text = "Imports load into this form first. Review and save before they join the catalog.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (!session.importedFileName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            StatusBadge(
                                label = session.importedFileName,
                                color = AccentBlue
                            )
                        }
                    }
                }

                CardBlock(title = "Essential Fields", modifier = Modifier.fillMaxWidth()) {
                    if (destinations.isEmpty()) {
                        Text(
                            text = "No destination sets are available yet. Create a set in the sidebar before saving.",
                            color = AccentRed,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    DestinationField(
                        label = "Destination Set",
                        selectedSetId = session.draft.destinationSetId,
                        destinations = destinations,
                        onSelect = { setId ->
                            onSessionChange(
                                session.copy(
                                    draft = session.draft.copy(destinationSetId = setId),
                                    errorMessage = null
                                )
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ComposerTextField(
                        label = "Title",
                        value = session.draft.title,
                        placeholder = "Contains Duplicate",
                        onValueChange = { value ->
                            onSessionChange(
                                session.copy(
                                    draft = session.draft.copy(title = value),
                                    errorMessage = null
                                )
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DifficultySelector(
                        label = "Difficulty",
                        value = session.draft.difficulty,
                        onValueChange = { value ->
                            onSessionChange(
                                session.copy(
                                    draft = session.draft.copy(difficulty = value),
                                    errorMessage = null
                                )
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ComposerTextField(
                        label = "About This Problem",
                        value = session.draft.summary,
                        placeholder = "Optional short note, vibe check, or funny comment about the problem",
                        singleLine = false,
                        minHeight = 92.dp,
                        onValueChange = { value ->
                            onSessionChange(
                                session.copy(
                                    draft = session.draft.copy(summary = value),
                                    errorMessage = null
                                )
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ComposerTextField(
                        label = "Problem Details",
                        value = session.draft.statementMarkdown,
                        placeholder = "Full problem statement, examples, constraints, and notes",
                        singleLine = false,
                        minHeight = 220.dp,
                        onValueChange = { value ->
                            onSessionChange(
                                session.copy(
                                    draft = session.draft.copy(statementMarkdown = value),
                                    errorMessage = null
                                )
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ComposerTextField(
                        label = "Example Input",
                        value = session.draft.exampleInput,
                        placeholder = "nums = [1,2,3,1]",
                        singleLine = false,
                        minHeight = 86.dp,
                        onValueChange = { value ->
                            onSessionChange(
                                session.copy(
                                    draft = session.draft.copy(exampleInput = value),
                                    errorMessage = null
                                )
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ComposerTextField(
                        label = "Example Output",
                        value = session.draft.exampleOutput,
                        placeholder = "true",
                        singleLine = false,
                        minHeight = 86.dp,
                        onValueChange = { value ->
                            onSessionChange(
                                session.copy(
                                    draft = session.draft.copy(exampleOutput = value),
                                    errorMessage = null
                                )
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StarterCodeField(
                        label = "Starter Code",
                        draft = session.draft,
                        placeholder = "class Solution:\n    def containsDuplicate(self, nums):\n        ...",
                        onDraftChange = { updatedDraft ->
                            onSessionChange(
                                session.copy(
                                    draft = updatedDraft,
                                    errorMessage = null
                                )
                            )
                        }
                    )
                }

                CardBlock(title = "Advanced", modifier = Modifier.fillMaxWidth()) {
                    PipelineField(
                        selectedValue = session.draft.executionPipelineOverride,
                        onSelect = { value ->
                            onSessionChange(
                                session.copy(
                                    draft = session.draft.copy(executionPipelineOverride = value),
                                    errorMessage = null
                                )
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HintListField(
                        label = "Hints",
                        hintsText = session.draft.hintsText,
                        onValueChange = { value ->
                            onSessionChange(
                                session.copy(
                                    draft = session.draft.copy(hintsText = value),
                                    errorMessage = null
                                )
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Submission suite JSON auto-fills from the current examples. Edit it to override, or clear it to go back to auto-generated cases.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ComposerTextField(
                        label = "Submission Suite JSON",
                        value = session.draft.displaySubmissionTestSuiteJson(),
                        placeholder = "",
                        singleLine = false,
                        minHeight = 180.dp,
                        onValueChange = { value ->
                            onSessionChange(
                                session.copy(
                                    draft = session.draft.copy(submissionTestSuiteJson = value),
                                    errorMessage = null
                                )
                            )
                        }
                    )
                    session.draft.submissionTestSuiteValidationError()?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = AccentRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun HintListField(
    label: String,
    hintsText: String,
    onValueChange: (String) -> Unit
) {
    val hints = remember(hintsText) {
        parseComposerHintsText(
            raw = hintsText,
            preserveBlankEntries = true
        )
    }
    var newHintDraft by remember { mutableStateOf("") }
    var scrollToLatestHint by remember { mutableStateOf(false) }
    val latestHintRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(hints.size, scrollToLatestHint) {
        if (scrollToLatestHint && hints.isNotEmpty()) {
            latestHintRequester.bringIntoView()
            scrollToLatestHint = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall
            )
            Surface(
                color = CardBackgroundAlt,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                ) {
                    BasicTextField(
                        value = newHintDraft,
                        onValueChange = { newHintDraft = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                        cursorBrush = SolidColor(AccentBlue),
                        singleLine = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 11.dp, end = 12.dp, bottom = 52.dp),
                        decorationBox = { innerTextField ->
                            if (newHintDraft.isBlank()) {
                                Text(
                                    text = "Add a new hint",
                                    color = TextMuted,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            innerTextField()
                        }
                    )
                    PlainActionChip(
                        label = "Add Hint",
                        iconRes = R.drawable.ic_plus,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    ) {
                        val normalizedHint = newHintDraft.trim()
                        if (normalizedHint.isBlank()) return@PlainActionChip
                        scrollToLatestHint = true
                        onValueChange(serializeComposerHints(hints + normalizedHint))
                        newHintDraft = ""
                    }
                }
            }
        }

        if (hints.isEmpty()) {
            Text(
                text = "No hints added yet.",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            hints.forEachIndexed { index, hint ->
                Row(
                    modifier = if (index == hints.lastIndex) {
                        Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(latestHintRequester)
                    } else {
                        Modifier.fillMaxWidth()
                    },
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = AccentBlue.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.34f))
                    ) {
                        Text(
                            text = "Hint ${index + 1}",
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)
                        )
                    }
                    Surface(
                        color = InsetSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        BasicTextField(
                            value = hint,
                            onValueChange = { updatedHint ->
                                onValueChange(
                                    serializeComposerHints(
                                        hints.toMutableList()
                                            .apply { this[index] = updatedHint }
                                    )
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                            cursorBrush = SolidColor(AccentBlue),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            decorationBox = { innerTextField ->
                                if (hint.isBlank()) {
                                    Text(
                                        text = "Hint text",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                    Surface(
                        color = AccentRed.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.58f)),
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                onValueChange(
                                    serializeComposerHints(
                                        hints.toMutableList()
                                            .apply { removeAt(index) }
                                    )
                                )
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "-",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DifficultySelector(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    val normalizedValue = value.trim().lowercase().ifBlank { "easy" }
    val options = listOf("Easy", "Medium", "Hard")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = TextMuted,
            style = MaterialTheme.typography.labelSmall
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                SupportTabChip(
                    label = option,
                    selected = normalizedValue == option.lowercase(),
                    onClick = { onValueChange(option) }
                )
            }
        }
    }
}

@Composable
private fun StarterCodeField(
    label: String,
    draft: ProblemComposerDraft,
    placeholder: String,
    onDraftChange: (ProblemComposerDraft) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f)
            )
            SupportTabChip(
                label = "Auto",
                selected = draft.starterCodeMode == ProblemStarterCodeMode.Auto,
                onClick = {
                    onDraftChange(
                        draft.copy(starterCodeMode = ProblemStarterCodeMode.Auto)
                    )
                }
            )
            SupportTabChip(
                label = "Manual",
                selected = draft.starterCodeMode == ProblemStarterCodeMode.Manual,
                onClick = {
                    onDraftChange(
                        draft.copy(
                            starterCodeMode = ProblemStarterCodeMode.Manual,
                            starterCode = draft.effectiveStarterCode()
                        )
                    )
                }
            )
        }
        if (draft.starterCodeMode == ProblemStarterCodeMode.Auto) {
            Text(
                text = "Auto mode rebuilds the starter from the title, statement, and example inputs.",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
        ComposerTextField(
            label = null,
            value = draft.effectiveStarterCode(),
            placeholder = placeholder,
            singleLine = false,
            minHeight = 180.dp,
            maxHeight = 320.dp,
            readOnly = draft.starterCodeMode == ProblemStarterCodeMode.Auto,
            asPythonEditor = true,
            onValueChange = { value ->
                onDraftChange(
                    draft.copy(
                        starterCodeMode = ProblemStarterCodeMode.Manual,
                        starterCode = value
                    )
                )
            }
        )
    }
}

@Composable
private fun DestinationField(
    label: String,
    selectedSetId: String,
    destinations: List<ProblemComposerDestination>,
    onSelect: (String) -> Unit
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val selectedLabel = destinations.firstOrNull { it.setId == selectedSetId }?.label ?: "Choose a destination set"

    ComposerDropdownField(
        label = label,
        value = selectedLabel,
        expanded = false,
        onExpandedChange = { pickerOpen = true }
    ) {
        if (pickerOpen) {
            Unit
        }
    }

    if (pickerOpen) {
        AlertDialog(
            onDismissRequest = { pickerOpen = false },
            title = {
                Text(
                    text = "Choose Destination Set",
                    color = TextPrimary
                )
            },
            text = {
                if (destinations.isEmpty()) {
                    Text(
                        text = "No destination sets are available yet.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(destinations, key = { it.setId }) { destination ->
                            Surface(
                                color = if (destination.setId == selectedSetId) AccentBlue.copy(alpha = 0.12f) else CardBackgroundAlt,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (destination.setId == selectedSetId) AccentBlue.copy(alpha = 0.34f) else CardBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = destination.label,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PlainActionChip("Select") {
                                        pickerOpen = false
                                        onSelect(destination.setId)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { pickerOpen = false }
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun PipelineField(
    selectedValue: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = when (selectedValue) {
        ProblemExecutionPipeline.SingleMethod.storageValue -> "Single Method"
        ProblemExecutionPipeline.EncodeDecodeRoundTrip.storageValue -> "String Codec"
        ProblemExecutionPipeline.SerializeDeserializeRoundTrip.storageValue -> "Tree Codec"
        ProblemExecutionPipeline.OperationSequence.storageValue -> "Operation Sequence"
        else -> "Auto Detect"
    }

    ComposerDropdownField(
        label = "Execution Pipeline",
        value = selectedLabel,
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        DropdownMenuItem(
            text = { Text("Auto Detect") },
            onClick = {
                expanded = false
                onSelect("")
            }
        )
        ProblemExecutionPipeline.entries.forEach { pipeline ->
            DropdownMenuItem(
                text = { Text(pipeline.displayLabel()) },
                onClick = {
                    expanded = false
                    onSelect(pipeline.storageValue)
                }
            )
        }
    }
}

@Composable
private fun ComposerDropdownField(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    menuContent: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = TextMuted,
            style = MaterialTheme.typography.labelSmall
        )
        Surface(
            color = CardBackgroundAlt,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    color = if (value.startsWith("Choose ")) TextMuted else TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                PlainActionChip("Pick") {
                    onExpandedChange(true)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) }
                ) {
                    menuContent()
                }
            }
        }
    }
}

@Composable
private fun ComposerTextField(
    label: String?,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp,
    maxHeight: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Unspecified,
    readOnly: Boolean = false,
    asPythonEditor: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) {
            Text(
                text = label,
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Surface(
            color = CardBackgroundAlt,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = modifier.fillMaxWidth()
        ) {
            if (asPythonEditor) {
                EditablePythonSurface(
                    code = value,
                    onCodeChange = onValueChange,
                    readOnly = readOnly,
                    placeholder = placeholder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            min = minHeight,
                            max = if (maxHeight == androidx.compose.ui.unit.Dp.Unspecified) androidx.compose.ui.unit.Dp.Infinity else maxHeight
                        )
                )
            } else {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                    cursorBrush = SolidColor(AccentBlue),
                    singleLine = singleLine,
                    readOnly = readOnly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            min = minHeight,
                            max = if (maxHeight == androidx.compose.ui.unit.Dp.Unspecified) androidx.compose.ui.unit.Dp.Infinity else maxHeight
                        )
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    decorationBox = { innerTextField ->
                        if (value.isBlank()) {
                            Text(
                                text = placeholder,
                                color = TextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

private fun ProblemExecutionPipeline.displayLabel(): String = when (this) {
    ProblemExecutionPipeline.SingleMethod -> "Single Method"
    ProblemExecutionPipeline.EncodeDecodeRoundTrip -> "String Codec"
    ProblemExecutionPipeline.SerializeDeserializeRoundTrip -> "Tree Codec"
    ProblemExecutionPipeline.OperationSequence -> "Operation Sequence"
}

private fun Context.resolveDisplayName(uri: Uri): String {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (columnIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(columnIndex)
            }
        }
    return uri.lastPathSegment ?: "problem.json"
}

private data class ImportedComposerFile(
    val fileName: String,
    val draft: ProblemComposerDraft
)
