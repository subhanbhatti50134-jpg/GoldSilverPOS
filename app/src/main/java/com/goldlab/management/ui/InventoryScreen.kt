package com.goldlab.management.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goldlab.management.data.InventoryEntity
import com.goldlab.management.data.InventoryMovementEntity
import com.goldlab.management.data.InventoryRepository
import com.goldlab.management.data.LabDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

@Composable
fun InventoryScreen() {
    val context = LocalContext.current
    val database = remember(context) { LabDatabase.getInstance(context) }
    val repository = remember(database) { InventoryRepository(database) }
    val scope = rememberCoroutineScope()

    val inventory by repository.observeItems().collectAsState(initial = emptyList())

    var showAdd by remember { mutableStateOf(false) }
    var actionItem by remember { mutableStateOf<InventoryEntity?>(null) }
    var actionDirection by remember { mutableStateOf("IN") }
    var historyItem by remember { mutableStateOf<InventoryEntity?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    val movementFlow = remember(historyItem?.id) {
        historyItem?.let { repository.observeMovements(it.id) }
            ?: flowOf(emptyList<InventoryMovementEntity>())
    }
    val movements by movementFlow.collectAsState(initial = emptyList())

    val totalWeight = inventory.fold(BigDecimal.ZERO) { total, item ->
        total.add(item.totalWeight())
    }
    val totalValue = inventory.fold(BigDecimal.ZERO) { total, item ->
        total.add(
            item.totalWeight().multiply(
                item.ratePerGram.toBigDecimalOrNull() ?: BigDecimal.ZERO
            )
        )
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Inventory Management",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InventorySummary(
                        "Stock weight",
                        "${totalWeight.displayDecimal()} g",
                        Modifier.weight(1f)
                    )
                    InventorySummary(
                        "Estimated value",
                        "PKR ${totalValue.displayDecimal()}",
                        Modifier.weight(1f)
                    )
                }
            }
            item {
                Text(
                    "Loose metal is tracked in grams. Biscuit stock uses " +
                        "piece quantity and per-biscuit weight.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            item {
                Button(
                    onClick = { showAdd = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("＋ Add inventory item / opening stock")
                }
            }
            if (inventory.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("No inventory items yet")
                            Text(
                                "Add your first loose gold, loose silver, " +
                                    "gold biscuit or silver biscuit item."
                            )
                        }
                    }
                }
            }
            items(inventory, key = { it.id }) { item ->
                InventoryItemCard(
                    item = item,
                    onStockIn = {
                        actionDirection = "IN"
                        actionItem = item
                    },
                    onStockOut = {
                        actionDirection = "OUT"
                        actionItem = item
                    },
                    onHistory = { historyItem = item }
                )
            }
        }

        status?.let {
            Text(
                it,
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showAdd) {
        AddInventoryDialog(
            onDismiss = { showAdd = false },
            onSave = { name, metal, type, purity, quantity, weight, rate, notes ->
                showAdd = false
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            repository.addItem(
                                name, metal, type, purity,
                                quantity, weight, rate, notes
                            )
                        }
                    }.onSuccess {
                        status = "Inventory item and opening stock saved."
                    }.onFailure {
                        status = "Could not save: ${it.message ?: "Unknown error"}"
                    }
                }
            }
        )
    }

    val selectedActionItem = actionItem
    if (selectedActionItem != null) {
        StockMovementDialog(
            item = selectedActionItem,
            direction = actionDirection,
            onDismiss = { actionItem = null },
            onConfirm = { amount, note ->
                actionItem = null
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            repository.changeStock(
                                selectedActionItem.id,
                                actionDirection,
                                amount,
                                note
                            )
                        }
                    }.onSuccess {
                        status = if (actionDirection == "IN") {
                            "Stock-in recorded."
                        } else {
                            "Stock-out recorded."
                        }
                    }.onFailure {
                        status = "Stock update failed: " +
                            (it.message ?: "Unknown error")
                    }
                }
            }
        )
    }

    val selectedHistoryItem = historyItem
    if (selectedHistoryItem != null) {
        AlertDialog(
            onDismissRequest = { historyItem = null },
            title = { Text("Movement history: ${selectedHistoryItem.name}") },
            text = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (movements.isEmpty()) {
                        Text("No movements recorded.")
                    }
                    movements.forEach { movement ->
                        MovementRow(movement)
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { historyItem = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun InventorySummary(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InventoryItemCard(
    item: InventoryEntity,
    onStockIn: () -> Unit,
    onStockOut: () -> Unit,
    onHistory: () -> Unit
) {
    val weight = item.totalWeight()
    val rate = item.ratePerGram.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val value = weight.multiply(rate)

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(item.name, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Text("${item.metal} • ${item.productType} • ${item.purity}")
            if (item.productType == "Biscuit") {
                Text("Quantity: ${item.quantity} biscuits")
                Text("Weight per biscuit: ${item.unitWeightGrams} g")
            }
            Text("Available weight: ${weight.displayDecimal()} g")
            Text("Rate: PKR ${rate.displayDecimal()} / gram")
            Text("Estimated stock value: PKR ${value.displayDecimal()}")
            if (item.notes.isNotBlank()) {
                Text("Notes: ${item.notes}", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onStockIn) { Text("+ Stock in") }
                TextButton(onClick = onStockOut) { Text("− Stock out") }
                TextButton(onClick = onHistory) { Text("History") }
            }
        }
    }
}

@Composable
private fun AddInventoryDialog(
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        metal: String,
        type: String,
        purity: String,
        quantity: Int,
        weight: String,
        rate: String,
        notes: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var metal by remember { mutableStateOf("Gold") }
    var type by remember { mutableStateOf("Loose") }
    var purity by remember { mutableStateOf("24K") }
    var quantity by remember { mutableStateOf("1") }
    var weight by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add inventory item") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    name, { name = it },
                    label = { Text("Item / lot name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("Metal")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = metal == "Gold",
                        onClick = {
                            metal = "Gold"
                            if (purity == "999") purity = "24K"
                        },
                        label = { Text("Gold") }
                    )
                    FilterChip(
                        selected = metal == "Silver",
                        onClick = {
                            metal = "Silver"
                            if (purity == "24K") purity = "999"
                        },
                        label = { Text("Silver") }
                    )
                }
                Text("Stock type")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "Loose",
                        onClick = { type = "Loose" },
                        label = { Text("Loose metal") }
                    )
                    FilterChip(
                        selected = type == "Biscuit",
                        onClick = { type = "Biscuit" },
                        label = { Text("Biscuit") }
                    )
                }
                OutlinedTextField(
                    purity, { purity = it },
                    label = { Text("Purity (e.g. 24K, 22K, 999)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (type == "Biscuit") {
                    OutlinedTextField(
                        quantity, { quantity = it },
                        label = { Text("Opening biscuit quantity") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    weight, { weight = it },
                    label = {
                        Text(
                            if (type == "Loose") "Opening total weight (grams)"
                            else "Weight per biscuit (grams)"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    rate, { rate = it },
                    label = { Text("Estimated rate per gram (PKR)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    notes, { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    name, metal, type, purity,
                    if (type == "Biscuit") quantity.toIntOrNull() ?: 0 else 0,
                    weight, rate, notes
                )
            }) { Text("Save opening stock") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun StockMovementDialog(
    item: InventoryEntity,
    direction: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: String, note: String) -> Unit
) {
    var amount by remember(item.id, direction) { mutableStateOf("") }
    var note by remember(item.id, direction) { mutableStateOf("") }
    val isLoose = item.productType == "Loose"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (direction == "IN") "Stock in" else "Stock out")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${item.name} • ${item.metal} • ${item.purity}")
                Text(
                    if (isLoose) {
                        "Available: ${item.unitWeightGrams} grams"
                    } else {
                        "Available: ${item.quantity} biscuits"
                    }
                )
                OutlinedTextField(
                    amount, { amount = it },
                    label = {
                        Text(if (isLoose) "Weight in grams" else "Biscuit quantity")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    note, { note = it },
                    label = { Text("Reason / reference (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(amount, note) }) {
                Text(if (direction == "IN") "Record stock-in" else "Record stock-out")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun MovementRow(movement: InventoryMovementEntity) {
    val weight = movement.weightGramsDelta.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val sign = if (weight > BigDecimal.ZERO) "+" else ""
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            "${movement.movementType} • ${sign}${weight.displayDecimal()} g",
            fontWeight = FontWeight.SemiBold
        )
        if (movement.quantityDelta != 0) {
            Text("Quantity change: ${movement.quantityDelta} biscuits")
        }
        if (movement.note.isNotBlank()) Text(movement.note)
        Text(
            java.text.SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                java.util.Locale.getDefault()
            ).format(java.util.Date(movement.createdAt)),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun InventoryEntity.totalWeight(): BigDecimal {
    val weight = unitWeightGrams.toBigDecimalOrNull() ?: BigDecimal.ZERO
    return if (productType == "Biscuit") {
        weight.multiply(BigDecimal(quantity))
    } else {
        weight
    }
}

private fun BigDecimal.displayDecimal(): String =
    stripTrailingZeros().toPlainString().let {
        if (it.isBlank() || it == "-0") "0" else it
    }
