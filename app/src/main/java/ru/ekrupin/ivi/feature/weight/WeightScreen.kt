package ru.ekrupin.ivi.feature.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.ekrupin.ivi.R
import ru.ekrupin.ivi.core.ui.DatePickerField
import ru.ekrupin.ivi.core.ui.InfoCard
import ru.ekrupin.ivi.core.ui.ScreenScaffold
import ru.ekrupin.ivi.core.util.parseWeightInputToGrams
import ru.ekrupin.ivi.core.util.toDisplayDate
import ru.ekrupin.ivi.core.util.toWeightLabel
import java.time.LocalDate

@Composable
fun WeightScreen(viewModel: WeightViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    ScreenScaffold(title = stringResource(R.string.weight_title)) {
        InfoCard(
            title = stringResource(R.string.home_current_weight),
            body = uiState.currentWeightLabel,
        )
        Button(onClick = { showDialog = true }) {
            Text(stringResource(R.string.weight_add))
        }
        if (uiState.history.isEmpty()) {
            Text(stringResource(R.string.weight_empty))
        }
        uiState.history.forEach { record ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "${record.date.toDisplayDate()} • ${record.weightGrams.toWeightLabel()}")
                    record.comment?.let { Text(text = it) }
                }
            }
        }
    }

    if (showDialog) {
        AddWeightDialog(
            onDismiss = { showDialog = false },
            onSave = { date, grams, comment ->
                viewModel.addWeight(date, grams, comment)
                showDialog = false
            },
        )
    }
}

@Composable
private fun AddWeightDialog(
    onDismiss: () -> Unit,
    onSave: (LocalDate, Int, String) -> Unit,
) {
    var date by remember { mutableStateOf(LocalDate.now()) }
    var weight by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var weightError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.weight_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DatePickerField(
                    label = stringResource(R.string.weight_date_label),
                    value = date,
                    onValueChange = { date = it },
                    supportingText = stringResource(R.string.common_pick_date),
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = {
                        weight = it
                        weightError = false
                    },
                    label = { Text(stringResource(R.string.weight_value_label)) },
                    supportingText = {
                        if (weightError) Text(stringResource(R.string.validation_weight_invalid))
                    },
                    isError = weightError,
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text(stringResource(R.string.weight_comment_label)) },
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedWeight = parseWeightInputToGrams(weight)
                weightError = parsedWeight == null
                if (parsedWeight != null) {
                    onSave(date, parsedWeight, comment.trim())
                }
            }) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
