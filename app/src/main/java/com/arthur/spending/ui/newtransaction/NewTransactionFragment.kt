package com.arthur.spending.ui.newtransaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import java.text.SimpleDateFormat
import java.util.*

class NewTransactionFragment : Fragment() {

    private lateinit var newTransactionViewModel: NewTransactionViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        newTransactionViewModel = ViewModelProvider(this).get(NewTransactionViewModel::class.java)

        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    NewTransactionScreen()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun NewTransactionScreen() {
        var valueText by remember { mutableStateOf("0.00") }
        var category by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        var date by remember { mutableStateOf("") }
        var time by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "New Transaction",
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Value Input with currency behavior
            OutlinedTextField(
                value = "$$valueText",
                onValueChange = { },
                label = { Text("Value") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Number pad for value input
            CurrencyKeypad(
                currentValue = valueText,
                onValueChange = { valueText = it }
            )

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Date & Time",
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { },
                    label = { Text("Date") },
                    readOnly = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = time,
                    onValueChange = { },
                    label = { Text("Time") },
                    readOnly = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedButton(
                onClick = {
                    val now = Calendar.getInstance()
                    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    date = dateFormat.format(now.time)
                    time = timeFormat.format(now.time)
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Use Now")
            }

            Button(
                onClick = {
                    // TODO: Save transaction
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Transaction")
            }
        }
    }

    @Composable
    private fun CurrencyKeypad(
        currentValue: String,
        onValueChange: (String) -> Unit
    ) {
        val buttons = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "⌫")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            buttons.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { buttonText ->
                        Button(
                            onClick = {
                                when (buttonText) {
                                    "C" -> onValueChange("0.00")
                                    "⌫" -> {
                                        val current = currentValue.replace(".", "")
                                        if (current.length > 1) {
                                            val newValue = current.dropLast(1)
                                            onValueChange(formatCurrency(newValue))
                                        } else {
                                            onValueChange("0.00")
                                        }
                                    }
                                    else -> {
                                        val current = currentValue.replace(".", "")
                                        val newValue = current + buttonText
                                        onValueChange(formatCurrency(newValue))
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = if (buttonText in listOf("C", "⌫")) {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            } else {
                                ButtonDefaults.buttonColors()
                            }
                        ) {
                            Text(buttonText)
                        }
                    }
                }
            }
        }
    }

    private fun formatCurrency(value: String): String {
        if (value.isEmpty()) return "0.00"
        
        val intValue = value.toLongOrNull() ?: 0L
        val dollars = intValue / 100
        val cents = intValue % 100
        
        return String.format("%d.%02d", dollars, cents)
    }
}