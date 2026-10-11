package com.goldlab.management.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape

import kotlinx.coroutines.withContext

import kotlinx.coroutines.launch

import kotlinx.coroutines.Dispatchers

import com.goldlab.management.data.DatabaseBackup

import com.goldlab.management.data.LabDatabase

import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goldlab.management.data.PurchaseEntity
import com.goldlab.management.data.PaymentEntity
import com.goldlab.management.domain.MetalCalculator
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Screen(val label: String) { Dashboard("Dashboard"), Purchases("Transactions"), AddGold("Add Gold Purchase"), AddGoldBiscuit("Gold Biscuit"), AddSilver("Silver Biscuit"), Customers("Customers/Suppliers"), Reports("Reports"), Settings("Settings"), Inventory("Inventory"), Backup("Backup & Restore") }

@Composable fun LabApp(vm: LabViewModel) {
    var screen by remember { mutableStateOf(Screen.Dashboard) }
    var showDelete by remember { mutableStateOf<PurchaseEntity?>(null) }
    val purchases by vm.purchases.collectAsState()
    val customers by vm.customers.collectAsState()
    val message by vm.message.collectAsState()
    val payments by vm.selectedPurchasePayments.collectAsState()
    var showPaymentsFor by remember { mutableStateOf<PurchaseEntity?>(null) }
    var exportReceiptFor by remember { mutableStateOf<PurchaseEntity?>(null) }
    val context = LocalContext.current

    val createPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        val purchase = exportReceiptFor
        try {
            if (uri != null && purchase != null) {
                val output = context.contentResolver.openOutputStream(uri)
                    ?: error("Could not open the selected file")
                output.use { it.write(ReceiptPdf.create(purchase)) }
                Toast.makeText(context, "PDF receipt saved", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "PDF export failed: ${e.message ?: "Unknown error"}",
                Toast.LENGTH_LONG
            ).show()
        } finally {
            exportReceiptFor = null
        }
    }
    Scaffold(bottomBar = { NavigationBar { listOf(Screen.Dashboard, Screen.Purchases, Screen.Customers, Screen.Reports, Screen.Settings).forEach { s -> NavigationBarItem(selected=screen==s, onClick={screen=s}, icon={ Icon(when(s){Screen.Dashboard->Icons.Default.Dashboard;Screen.Purchases->Icons.Default.ReceiptLong;Screen.Customers->Icons.Default.People;Screen.Reports->Icons.Default.Assessment;else->Icons.Default.Settings}, contentDescription=s.label) }, label={Text(s.label)}) } } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
                Column(Modifier.padding(vertical=16.dp)) { Text("GOLD & SILVER", style=MaterialTheme.typography.labelLarge, color=MaterialTheme.colorScheme.primary); Text(screen.label, style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold) }
                if (screen != Screen.Dashboard) TextButton(onClick={screen=Screen.Dashboard}) { Text("Home") }
            }
            when(screen) {
                Screen.Dashboard -> Dashboard(purchases, customers.size, vm, onNavigate={screen=it})
                Screen.Purchases -> PurchasesScreen(
                    purchases,
                    vm,
                    onDelete = { showDelete = it },
                    onPayments = {
                        showPaymentsFor = it
                        vm.showPayments(it.id)
                    },
                    onExportPdf = {
                        exportReceiptFor = it
                        createPdfLauncher.launch("SNJ-${it.transactionId}.pdf")
                    },
                    onPrint = {
                        try {
                            ReceiptPdf.print(context, it)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Print failed: ${e.message ?: "Unknown error"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
                Screen.AddGold -> PurchaseForm("Gold", "Loose Gold", vm, onDone={screen=Screen.Purchases})
                Screen.AddGoldBiscuit -> PurchaseForm("Gold", "Gold Biscuit", vm, onDone={screen=Screen.Purchases})
                Screen.AddSilver -> PurchaseForm("Silver", "Silver Biscuit", vm, onDone={screen=Screen.Purchases})
                Screen.Customers -> CustomersScreen(customers, vm)
                Screen.Reports -> ReportsScreen(purchases)
                Screen.Settings -> SettingsScreen(onBackup = { screen = Screen.Backup })
                Screen.Inventory -> InventoryScreen()
                Screen.Backup -> BackupScreen()
            }
        }
    }
    if (message != null) AlertDialog(onDismissRequest={vm.clearMessage()}, title={Text("Gold Lab")}, text={Text(message ?: "")}, confirmButton={TextButton(onClick={vm.clearMessage()}){Text("OK")}})
    if (showDelete != null) AlertDialog(onDismissRequest={showDelete=null}, title={Text("Delete transaction?")}, text={Text("This will permanently remove ${showDelete?.transactionId} and its payment entries.")}, confirmButton={TextButton(onClick={ val p=showDelete!!; vm.deletePurchase(p); showDelete=null }){Text("Delete", color=MaterialTheme.colorScheme.error)}}, dismissButton={TextButton(onClick={showDelete=null}){Text("Cancel")}})
    if (showPaymentsFor != null) {
        PaymentDialog(
            purchase = showPaymentsFor!!,
            payments = payments,
            onDismiss = {
                showPaymentsFor = null
                vm.closePayments()
            },
            onAddPayment = { amount, method, note ->
                vm.recordPayment(showPaymentsFor!!.id, amount, method, note)
            }
        )
    }
}

@Composable private fun Dashboard(items: List<PurchaseEntity>, customerCount: Int, vm: LabViewModel, onNavigate:(Screen)->Unit) {
    val total = vm.dashboardTotal(items)
    val goldWeight = items.filter{it.metal=="Gold"}.sumOf { runCatching { BigDecimal(it.weight).toDouble() }.getOrDefault(0.0) }
    val silverWeight = items.filter{it.metal=="Silver"}.sumOf { runCatching { BigDecimal(it.weight).toDouble() }.getOrDefault(0.0) }
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { SummaryCard("Purchases", items.size.toString(), Modifier.weight(1f)); SummaryCard("Customers", customerCount.toString(), Modifier.weight(1f)) } }
        item { Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { SummaryCard("Gold weight", "%.3f g".format(goldWeight), Modifier.weight(1f)); SummaryCard("Silver weight", "%.3f g".format(silverWeight), Modifier.weight(1f)) } }
        item { SummaryCard("Total purchase value", "PKR ${total.toPlainString()}", Modifier.fillMaxWidth()) }
        item { Text("Quick actions", style=MaterialTheme.typography.titleLarge) }
        item { ActionButton("Add Gold Purchase", Icons.Default.Add, Modifier.fillMaxWidth()){onNavigate(Screen.AddGold)} }
        item { ActionButton("Add Gold Biscuit", Icons.Default.Inventory2, Modifier.fillMaxWidth()){onNavigate(Screen.AddGoldBiscuit)} }
        item { ActionButton("Add Silver Biscuit", Icons.Default.Diamond, Modifier.fillMaxWidth()){onNavigate(Screen.AddSilver)} }
        item { ActionButton("Inventory Management", Icons.Default.Inventory2, Modifier.fillMaxWidth()) { onNavigate(Screen.Inventory) } }
        item { ActionButton("Add Customer / Supplier", Icons.Default.PersonAdd, Modifier.fillMaxWidth()){onNavigate(Screen.Customers)} }
        item { ActionButton("View Transactions", Icons.Default.ReceiptLong, Modifier.fillMaxWidth()){onNavigate(Screen.Purchases)} }
        item { ActionButton("Reports", Icons.Default.Assessment, Modifier.fillMaxWidth()){onNavigate(Screen.Reports)} }
        item { Text("Recent transactions", style=MaterialTheme.typography.titleLarge) }
        if(items.isEmpty()) item { EmptyState("No purchases yet", "Use a quick action above to record your first transaction.") }
        items(items.take(5), key={it.id}) { PurchaseCard(it) }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(3.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(50)
                    )
            )
        }
    }
}
@Composable
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Text(label, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null
        )
    }
}
@Composable
private fun EmptyState(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = body,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable private fun PurchaseForm(metal:String, productType:String, vm:LabViewModel, onDone:()->Unit) {
    var party by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var purity by remember { mutableStateOf(if(metal=="Gold") "24K" else "999") }
    var weight by remember { mutableStateOf("") }; var weightUnit by remember { mutableStateOf("grams") }; var rate by remember { mutableStateOf("") }; var rateUnit by remember { mutableStateOf("per gram") }
    var paid by remember { mutableStateOf("0") }; var company by remember { mutableStateOf("") }; var serial by remember { mutableStateOf("") }; var method by remember { mutableStateOf("Cash") }; var notes by remember { mutableStateOf("") }; var overrideTotal by remember { mutableStateOf("") }; var useOverride by remember { mutableStateOf(false) }
    val computed = remember(weight,weightUnit,rate,rateUnit) { runCatching { MetalCalculator.calculate(BigDecimal(weight),weightUnit,BigDecimal(rate),rateUnit).toPlainString() }.getOrDefault("0.00") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("$productType details", style=MaterialTheme.typography.titleMedium)
        OutlinedTextField(party,{party=it},label={Text("Customer / supplier name *")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        OutlinedTextField(phone,{phone=it},label={Text("Phone (optional)")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        if(productType.contains("Biscuit")) { OutlinedTextField(company,{company=it},label={Text("Company / brand")},modifier=Modifier.fillMaxWidth()); OutlinedTextField(serial,{serial=it},label={Text("Serial number")},modifier=Modifier.fillMaxWidth()) }
        OutlinedTextField(purity,{purity=it},label={Text(if(metal=="Gold") "Purity (24K, 22K, custom)" else "Fineness (999, 925, custom)")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        OutlinedTextField(weight,{weight=it},label={Text("Weight *")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("grams","tola","kilograms").forEach { FilterChip(selected=weightUnit==it,onClick={weightUnit=it},label={Text(it)}) } }
        OutlinedTextField(rate,{rate=it},label={Text("Purchase rate *")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("per gram","per tola").forEach { FilterChip(selected=rateUnit==it,onClick={rateUnit=it},label={Text(it)}) } }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text("Calculated total", color=MaterialTheme.colorScheme.primary); Text("PKR $computed", style=MaterialTheme.typography.headlineSmall); Text("Formula uses the selected weight and rate units.", style=MaterialTheme.typography.bodySmall) } }
        Row { Checkbox(useOverride,{useOverride=it}); Text("Manually override total (confirm business value)", modifier=Modifier.padding(top=12.dp)) }
        if(useOverride) OutlinedTextField(overrideTotal,{overrideTotal=it},label={Text("Confirmed total amount")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(paid,{paid=it},label={Text("Amount paid")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("Cash","Bank transfer","Other").forEach { FilterChip(selected=method==it,onClick={method=it},label={Text(it)}) } }
        OutlinedTextField(notes,{notes=it},label={Text("Notes (optional)")},modifier=Modifier.fillMaxWidth(),minLines=2)
        Button(onClick={
            val finalTotal = if(useOverride) overrideTotal else computed
            val valid = party.isNotBlank() && runCatching { BigDecimal(weight)>BigDecimal.ZERO && BigDecimal(rate)>=BigDecimal.ZERO && BigDecimal(paid)>=BigDecimal.ZERO && BigDecimal(finalTotal)>=BigDecimal.ZERO && BigDecimal(paid)<=BigDecimal(finalTotal) }.getOrDefault(false)
            if(valid) { val now=System.currentTimeMillis(); vm.addPurchase(PurchaseEntity(transactionId="", partyName=party.trim(),phone=phone.trim(),metal=metal,productType=productType,company=company,serialNumber=serial,purity=purity,weight=weight,weightUnit=weightUnit,rate=rate,rateUnit=rateUnit,totalAmount=finalTotal,paidAmount=paid,paymentMethod=method,notes=notes,createdAt=now,updatedAt=now,manualTotalOverride=useOverride)); onDone() }
            else vm.addPurchase(PurchaseEntity(transactionId="INVALID", partyName="", metal=metal, weight="0", rate="0", totalAmount="0"))
        }, modifier=Modifier.fillMaxWidth()) { Text("Save purchase") }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun PurchasesScreen(
    items: List<PurchaseEntity>,
    vm: LabViewModel,
    onDelete: (PurchaseEntity) -> Unit,
    onPayments: (PurchaseEntity) -> Unit,
    onExportPdf: (PurchaseEntity) -> Unit,
    onPrint: (PurchaseEntity) -> Unit
) {
    val query by vm.searchQuery.collectAsState()

    Column {
        OutlinedTextField(
            value = query,
            onValueChange = vm::setQuery,
            label = { Text("Search ID, name, company, serial") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        if (items.isEmpty()) {
            EmptyState(
                "No matching transactions",
                "Saved purchases will appear here."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.id }) { p ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            PurchaseCard(p)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = { onPayments(p) }) {
                                    Icon(Icons.Default.Payments, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Payments")
                                }
                                TextButton(onClick = { onExportPdf(p) }) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("PDF")
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = { onPrint(p) }) {
                                    Icon(Icons.Default.Print, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Print")
                                }
                                TextButton(onClick = { onDelete(p) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentDialog(
    purchase: PurchaseEntity,
    payments: List<PaymentEntity>,
    onDismiss: () -> Unit,
    onAddPayment: (String, String, String) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Cash") }
    var note by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf("") }

    val total = runCatching { BigDecimal(purchase.totalAmount) }
        .getOrDefault(BigDecimal.ZERO)
    val paid = runCatching { BigDecimal(purchase.paidAmount) }
        .getOrDefault(BigDecimal.ZERO)
    val due = total.subtract(paid).max(BigDecimal.ZERO)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Payment history") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(purchase.transactionId, fontWeight = FontWeight.Bold)
                Text("Party: ${purchase.partyName}")
                Text("Total: PKR $total")
                Text("Paid: PKR $paid")
                Text("Due: PKR $due", color = MaterialTheme.colorScheme.primary)
                HorizontalDivider()

                if (payments.isEmpty()) {
                    Text("No payment entries found.")
                } else {
                    payments.sortedByDescending { it.paidAt }.forEach { payment ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp)) {
                                Text(
                                    "PKR ${payment.amount}",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(payment.method)
                                Text(
                                    SimpleDateFormat(
                                        "dd MMM yyyy, hh:mm a",
                                        Locale.getDefault()
                                    ).format(Date(payment.paidAt)),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (payment.note.isNotBlank()) {
                                    Text(payment.note)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (due > BigDecimal.ZERO) showAdd = true
                },
                enabled = due > BigDecimal.ZERO
            ) {
                Text("Add payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Record payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Outstanding: PKR $due")
                    OutlinedTextField(
                        value = amount,
                        onValueChange = {
                            amount = it
                            validationError = ""
                        },
                        label = { Text("Payment amount (PKR)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Payment method")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Cash", "Bank transfer", "Other").forEach {
                            FilterChip(
                                selected = method == it,
                                onClick = { method = it },
                                label = { Text(it) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (validationError.isNotBlank()) {
                        Text(
                            validationError,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val entered = runCatching { BigDecimal(amount.trim()) }
                            .getOrNull()

                        when {
                            entered == null || entered <= BigDecimal.ZERO ->
                                validationError = "Enter a valid amount greater than zero."

                            entered > due ->
                                validationError = "Payment cannot exceed the outstanding due."

                            else -> {
                                onAddPayment(amount.trim(), method, note.trim())
                                showAdd = false
                            }
                        }
                    }
                ) {
                    Text("Save payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable private fun PurchaseCard(p:PurchaseEntity) { Column(Modifier.fillMaxWidth().padding(vertical=4.dp)) { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(p.transactionId, fontWeight=FontWeight.Bold); Text(p.metal, color=MaterialTheme.colorScheme.primary) }; Text(p.partyName); Text("${p.productType} • ${p.weight} ${p.weightUnit} • ${p.purity}",style=MaterialTheme.typography.bodySmall); Text("PKR ${p.totalAmount}  |  Paid ${p.paidAmount}  |  Due ${runCatching { BigDecimal(p.totalAmount).subtract(BigDecimal(p.paidAmount)).toPlainString() }.getOrDefault("—")}") } }

@Composable private fun CustomersScreen(customers:List<com.goldlab.management.data.CustomerEntity>, vm:LabViewModel) {
    var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var kind by remember { mutableStateOf("Supplier") }; var address by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }
    Column { Text("Add customer or supplier", style=MaterialTheme.typography.titleMedium); OutlinedTextField(name,{name=it},label={Text("Full name *")},modifier=Modifier.fillMaxWidth()); OutlinedTextField(phone,{phone=it},label={Text("Phone")},modifier=Modifier.fillMaxWidth()); Row { listOf("Supplier","Customer","Both").forEach { FilterChip(selected=kind==it,onClick={kind=it},label={Text(it)},modifier=Modifier.padding(end=6.dp)) } }; OutlinedTextField(address,{address=it},label={Text("Address")},modifier=Modifier.fillMaxWidth()); OutlinedTextField(notes,{notes=it},label={Text("Notes")},modifier=Modifier.fillMaxWidth()); Button(onClick={if(name.isNotBlank()){vm.addCustomer(name,phone,kind,address,notes);name="";phone="";address="";notes=""}},modifier=Modifier.fillMaxWidth()){Text("Save contact")}; Spacer(Modifier.height(12.dp)); Text("Directory (${customers.size})",style=MaterialTheme.typography.titleLarge); LazyColumn { items(customers,key={it.id}) { c -> ListItem(headlineContent={Text(c.name)}, supportingContent={Text("${c.kind} • ${c.phone}")}) } } }
}

@Composable private fun ReportsScreen(items:List<PurchaseEntity>) {
    val total=items.fold(BigDecimal.ZERO){a,p->a.add(runCatching{BigDecimal(p.totalAmount)}.getOrDefault(BigDecimal.ZERO))}; val paid=items.fold(BigDecimal.ZERO){a,p->a.add(runCatching{BigDecimal(p.paidAmount)}.getOrDefault(BigDecimal.ZERO))}
    LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) { item { SummaryCard("All-time purchase value", "PKR $total", Modifier.fillMaxWidth()) }; item { SummaryCard("Recorded paid", "PKR $paid", Modifier.fillMaxWidth()) }; item { SummaryCard("Outstanding", "PKR ${total.subtract(paid)}", Modifier.fillMaxWidth()) }; item { Text("Metal breakdown",style=MaterialTheme.typography.titleLarge) }; item { Text("Gold: ${items.count{it.metal=="Gold"}} transactions • PKR ${items.filter{it.metal=="Gold"}.sumOf{runCatching{BigDecimal(it.totalAmount)}.getOrDefault(BigDecimal.ZERO)}}") }; item { Text("Silver: ${items.count{it.metal=="Silver"}} transactions • PKR ${items.filter{it.metal=="Silver"}.sumOf{runCatching{BigDecimal(it.totalAmount)}.getOrDefault(BigDecimal.ZERO)}}") }; item { Text("For complete date-range, PDF, CSV and XLSX exports, see implementation status in README.",style=MaterialTheme.typography.bodySmall) } }
}
@Composable
private fun SettingsScreen(onBackup: () -> Unit) {
    var lab by remember { mutableStateOf("Gold & Silver Laboratory") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var footer by remember { mutableStateOf("Thank you") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Laboratory settings", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            lab, { lab = it },
            label = { Text("Laboratory name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            phone, { phone = it },
            label = { Text("Contact phone") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            address, { address = it },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            footer, { footer = it },
            label = { Text("Receipt footer") },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            "Currency: PKR • Default conversion: 1 tola = 11.6638 grams",
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        Text("Data management", style = MaterialTheme.typography.titleMedium)

        Button(
            onClick = onBackup,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Backup, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Backup & Restore")
        }

        Text(
            "Export a backup or restore records from a previously saved backup.",
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            "Settings fields are currently a UI draft and are not saved permanently.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun BackupScreen() {
    val context = LocalContext.current
    val database = remember(context) {
        LabDatabase.getInstance(context)
    }
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }
    var status by remember {
        mutableStateOf("Export a backup before trying restore.")
    }
    var pendingRestore by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                busy = true
                try {
                    val json = withContext(Dispatchers.IO) {
                        DatabaseBackup.exportJson(database)
                    }

                    withContext(Dispatchers.IO) {
                        val output = context.contentResolver.openOutputStream(uri)
                            ?: error("Could not open the selected file.")
                        output.bufferedWriter(Charsets.UTF_8).use {
                            it.write(json)
                        }
                    }

                    status = "Backup exported successfully."
                } catch (e: Exception) {
                    status = "Export failed: ${e.message ?: "Unknown error"}"
                } finally {
                    busy = false
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                busy = true
                try {
                    val json = withContext(Dispatchers.IO) {
                        val input = context.contentResolver.openInputStream(uri)
                            ?: error("Could not open the selected file.")
                        input.bufferedReader(Charsets.UTF_8).use {
                            it.readText()
                        }
                    }

                    withContext(Dispatchers.IO) {
                        DatabaseBackup.validateBackup(json, database)
                    }

                    pendingRestore = json
                    status = "Backup validated. Confirm restore to continue."
                } catch (e: Exception) {
                    status = "Backup rejected: ${e.message ?: "Invalid file"}"
                } finally {
                    busy = false
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Backup & Restore",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Export customers, purchases, payment history, audit logs and " +
                "laboratory settings into a versioned JSON backup."
        )

        Button(
            onClick = {
                exportLauncher.launch("SNJ_GoldLab_Backup.json")
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Export backup")
        }

        OutlinedButton(
            onClick = {
                importLauncher.launch(
                    arrayOf("application/json", "text/plain", "*/*")
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy
        ) {
            Icon(Icons.Default.FileUpload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Choose backup to restore")
        }

        if (busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Working… please keep the app open.")
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Important", fontWeight = FontWeight.Bold)
                Text(
                    "Restoring replaces the current records with the selected " +
                        "backup. A safety copy of the current records is saved " +
                        "inside the app before restore. Keep exported backups " +
                        "in a private, secure location."
                )
            }
        }

        Text(
            status,
            color = if (status.contains("failed", true) ||
                status.contains("rejected", true)
            ) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }

    val restoreJson = pendingRestore
    if (restoreJson != null) {
        AlertDialog(
            onDismissRequest = {
                if (!busy) pendingRestore = null
            },
            title = { Text("Confirm database restore") },
            text = {
                Text(
                    "This will replace current customers, purchases, payments, " +
                        "audit logs and settings with the selected backup. " +
                        "A safety copy will be saved first. Continue?"
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                val count = withContext(Dispatchers.IO) {
                                    DatabaseBackup.restore(
                                        context,
                                        database,
                                        restoreJson
                                    )
                                }
                                status = "Restore successful: $count records. " +
                                    "A safety copy was saved in app storage."
                                pendingRestore = null
                            } catch (e: Exception) {
                                status = "Restore failed: " +
                                    (e.message ?: "Unknown error")
                            } finally {
                                busy = false
                            }
                        }
                    }
                ) {
                    Text("Restore records")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !busy,
                    onClick = { pendingRestore = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

