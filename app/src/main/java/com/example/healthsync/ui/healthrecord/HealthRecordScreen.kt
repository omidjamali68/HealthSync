package com.example.healthsync.ui.healthrecord

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.healthsync.R
import com.example.healthsync.domain.model.HealthMetricViewModel
import com.example.healthsync.domain.model.HealthRecordIndexViewModel
import com.example.healthsync.util.DateUtils
import com.example.healthsync.util.DateUtils.toPersianDigits
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

@Composable
private fun getMetricIconAndColor(code: String, title: String): Pair<ImageVector, Color> {
    val t = title.lowercase()
    val c = code.lowercase()
    return when {
        t.contains("ضربان") || c.contains("hr") || c.contains("heart") ->
            Icons.Default.Favorite to Color(0xFFE53935)
        t.contains("فشار") || c.contains("bp") || c.contains("sys") || c.contains("dia") ->
            Icons.Default.Speed to Color(0xFF1E88E5)
        t.contains("اکسیژن") || c.contains("spo2") || c.contains("oxygen") ->
            Icons.Default.WaterDrop to Color(0xFF00ACC1)
        t.contains("قند") || c.contains("glucose") || c.contains("blood") ->
            Icons.Default.Bloodtype to Color(0xFFD81B60)
        t.contains("گام") || t.contains("قدم") || c.contains("step") ->
            Icons.Default.DirectionsWalk to Color(0xFF43A047)
        t.contains("خواب") || c.contains("sleep") ->
            Icons.Default.NightsStay to Color(0xFF5E35B1)
        t.contains("وزن") || c.contains("weight") ->
            Icons.Default.Scale to Color(0xFFFB8C00)
        t.contains("دما") || c.contains("temp") ->
            Icons.Default.Thermostat to Color(0xFFF4511E)
        else ->
            Icons.Default.HealthAndSafety to MaterialTheme.colorScheme.primary
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthRecordScreen(
    onBack: () -> Unit,
    vm: HealthRecordViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    val isPersian = DateUtils.isPersianMode()
    val snackbarHostState = remember { SnackbarHostState() }
    var isInsightExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(s.message) {
        s.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.health_record_title), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!s.latestInsight.isNullOrBlank() || s.insightLoading) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { isInsightExpanded = !isInsightExpanded },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        stringResource(R.string.latest_insight),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        stringResource(R.string.insight_limit_hint),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = vm::requestNewInsight) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.request_new_insight),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    imageVector = if (isInsightExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (s.insightLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        } else {
                            AnimatedVisibility(visible = isInsightExpanded) {
                                s.latestInsight?.let { text ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 220.dp)
                                            .verticalScroll(rememberScrollState())
                                            .padding(top = 4.dp, bottom = 4.dp)
                                    ) {
                                        Text(
                                            text = text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            TabRow(selectedTabIndex = s.selectedTab) {
                Tab(
                    selected = s.selectedTab == 0,
                    onClick = { vm.selectTab(0) },
                    text = { Text(stringResource(R.string.new_record)) }
                )
                Tab(
                    selected = s.selectedTab == 1,
                    onClick = { vm.selectTab(1) },
                    text = { Text(stringResource(R.string.record_history)) }
                )
            }

            if (s.selectedTab == 0) {
                CreateRecordForm(
                    state = s,
                    onSelectMetricType = vm::selectMetricType,
                    onUpdateComponentValue = vm::updateComponentValue,
                    onSubmit = vm::submitRecord
                )
            } else {
                RecordHistorySection(
                    state = s,
                    isPersian = isPersian,
                    onSetSearch = vm::setSearchQuery,
                    onSetFilterDate = vm::setFilterDateQuery,
                    onApplyFilters = vm::applyFilters,
                    onClearFilters = vm::clearFilters,
                    onGoToPage = vm::goToPage,
                    onSelectDetail = vm::selectDetailRecord,
                    onUpdateRecord = vm::updateRecord,
                    onDelete = vm::deleteRecord
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateRecordForm(
    state: HealthRecordUiState,
    onSelectMetricType: (HealthMetricViewModel) -> Unit,
    onUpdateComponentValue: (String, String) -> Unit,
    onSubmit: () -> Unit
) {
    var expandedDropdown by remember { mutableStateOf(false) }
    val activeComponents = remember(state.selectedMetricType, state.metricComponents) {
        state.selectedMetricType?.let { type ->
            state.metricComponents.filter { it.healthMetricTypeId == type.id }
        } ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.select_metric_type),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        ExposedDropdownMenuBox(
            expanded = expandedDropdown,
            onExpandedChange = { expandedDropdown = !expandedDropdown },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = state.selectedMetricType?.title ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.select_metric_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            ExposedDropdownMenu(
                expanded = expandedDropdown,
                onDismissRequest = { expandedDropdown = false }
            ) {
                state.metricTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.title) },
                        onClick = {
                            onSelectMetricType(type)
                            expandedDropdown = false
                        }
                    )
                }
            }
        }

        if (activeComponents.isNotEmpty()) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    activeComponents.forEach { comp ->
                        val value = state.componentValues[comp.id] ?: ""
                        val (compIcon, compColor) = getMetricIconAndColor(comp.code, comp.title)
                        OutlinedTextField(
                            value = value,
                            onValueChange = { onUpdateComponentValue(comp.id, it) },
                            label = {
                                val labelText = if (comp.unit.isNotBlank()) "${comp.title} (${comp.unit})" else comp.title
                                Text(labelText)
                            },
                            leadingIcon = {
                                Icon(imageVector = compIcon, contentDescription = null, tint = compColor, modifier = Modifier.size(20.dp))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !state.isLoading && state.selectedMetricType != null,
            shape = MaterialTheme.shapes.medium
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.submit_record))
                }
            }
        }
    }
}

@Composable
private fun RecordHistorySection(
    state: HealthRecordUiState,
    isPersian: Boolean,
    onSetSearch: (String) -> Unit,
    onSetFilterDate: (String) -> Unit,
    onApplyFilters: () -> Unit,
    onClearFilters: () -> Unit,
    onGoToPage: (Int) -> Unit,
    onSelectDetail: (HealthRecordIndexViewModel?) -> Unit,
    onUpdateRecord: (id: String, recordedAt: String, updatedValues: Map<String, String>) -> Unit,
    onDelete: (String) -> Unit
) {
    var showDatePickerDialog by remember { mutableStateOf(false) }

    if (showDatePickerDialog) {
        if (isPersian) {
            PersianDatePickerDialog(
                onDateSelected = { dateStr ->
                    onSetFilterDate(dateStr)
                },
                onDismiss = { showDatePickerDialog = false }
            )
        } else {
            GregorianDatePickerDialog(
                onDateSelected = { dateStr ->
                    onSetFilterDate(dateStr)
                },
                onDismiss = { showDatePickerDialog = false }
            )
        }
    }

    if (state.selectedDetailRecord != null) {
        RecordDetailsEditDialog(
            record = state.selectedDetailRecord,
            isPersian = isPersian,
            onSave = onUpdateRecord,
            onDismiss = { onSelectDetail(null) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onSetSearch,
                        label = { Text(stringResource(R.string.filter_metric_label), maxLines = 1) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = if (isPersian && state.filterDateQuery.isNotBlank()) state.filterDateQuery.toPersianDigits() else state.filterDateQuery,
                        onValueChange = onSetFilterDate,
                        label = { Text(stringResource(R.string.filter_date_label), maxLines = 1) },
                        leadingIcon = {
                            IconButton(onClick = { showDatePickerDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePickerDialog = true },
                        shape = MaterialTheme.shapes.medium
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onApplyFilters,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(stringResource(R.string.apply_filter))
                        }
                    }
                    OutlinedButton(
                        onClick = onClearFilters,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(stringResource(R.string.clear_filter))
                        }
                    }
                }
            }
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_records_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.records, key = { it.id }) { record ->
                    RecordItemCard(
                        record = record,
                        isPersian = isPersian,
                        onShowDetail = { onSelectDetail(record) },
                        onDelete = onDelete
                    )
                }
            }
        }

        if (state.records.isNotEmpty() || state.totalPages > 1) {
            val totalPages = state.totalPages.coerceAtLeast(1)
            val currentPage = state.currentPage.coerceIn(1, totalPages)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onGoToPage(currentPage - 1) },
                        enabled = currentPage > 1
                    ) {
                        Text(stringResource(R.string.previous))
                    }

                    val pageText = stringResource(
                        R.string.page_indicator,
                        currentPage,
                        totalPages,
                        state.totalCount
                    )
                    Text(
                        text = if (isPersian) pageText.toPersianDigits() else pageText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = { onGoToPage(currentPage + 1) },
                        enabled = currentPage < totalPages
                    ) {
                        Text(stringResource(R.string.next))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersianDatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val (nowYear, nowMonth, nowDay) = remember {
        val zdt = ZonedDateTime.now(ZoneId.of("Asia/Tehran"))
        DateUtils.gregorianToJalali(zdt.year, zdt.monthValue, zdt.dayOfMonth)
    }

    var selectedYear by remember { mutableIntStateOf(nowYear) }
    var selectedMonth by remember { mutableIntStateOf(nowMonth) }
    var selectedDay by remember { mutableIntStateOf(nowDay) }

    var yearExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }
    var dayExpanded by remember { mutableStateOf(false) }

    val months = remember {
        listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
    }
    val years = remember { (1390..1410).toList() }
    val maxDays = remember(selectedMonth) {
        when {
            selectedMonth <= 6 -> 31
            selectedMonth in 7..11 -> 30
            else -> 29
        }
    }

    LaunchedEffect(maxDays) {
        if (selectedDay > maxDays) {
            selectedDay = maxDays
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "انتخاب تاریخ شمسی",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val formattedPreview = String.format(Locale.ROOT, "%04d/%02d/%02d", selectedYear, selectedMonth, selectedDay).toPersianDigits()
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "$formattedPreview (${months[selectedMonth - 1]})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = yearExpanded,
                        onExpandedChange = { yearExpanded = !yearExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedYear.toString().toPersianDigits(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("سال") },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                        ExposedDropdownMenu(
                            expanded = yearExpanded,
                            onDismissRequest = { yearExpanded = false }
                        ) {
                            years.forEach { y ->
                                DropdownMenuItem(
                                    text = { Text(y.toString().toPersianDigits()) },
                                    onClick = {
                                        selectedYear = y
                                        yearExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = monthExpanded,
                        onExpandedChange = { monthExpanded = !monthExpanded },
                        modifier = Modifier.weight(1.2f)
                    ) {
                        OutlinedTextField(
                            value = months[selectedMonth - 1],
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("ماه") },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                        ExposedDropdownMenu(
                            expanded = monthExpanded,
                            onDismissRequest = { monthExpanded = false }
                        ) {
                            months.forEachIndexed { idx, mName ->
                                DropdownMenuItem(
                                    text = { Text(mName) },
                                    onClick = {
                                        selectedMonth = idx + 1
                                        monthExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = dayExpanded,
                        onExpandedChange = { dayExpanded = !dayExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedDay.toString().toPersianDigits(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("روز") },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                        ExposedDropdownMenu(
                            expanded = dayExpanded,
                            onDismissRequest = { dayExpanded = false }
                        ) {
                            (1..maxDays).forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d.toString().toPersianDigits()) },
                                    onClick = {
                                        selectedDay = d
                                        dayExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formatted = String.format(Locale.ROOT, "%04d/%02d/%02d", selectedYear, selectedMonth, selectedDay)
                    onDateSelected(formatted)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GregorianDatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val localDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        val formatted = String.format(Locale.ROOT, "%04d/%02d/%02d", localDate.year, localDate.monthValue, localDate.dayOfMonth)
                        onDateSelected(formatted)
                    }
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun RecordItemCard(
    record: HealthRecordIndexViewModel,
    isPersian: Boolean,
    onShowDetail: () -> Unit,
    onDelete: (String) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_record_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(record.id)
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    val validMetricValues = remember(record.metricValues) {
        record.metricValues.filterNotNull()
    }

    val metricTypeTitle = remember(validMetricValues) {
        validMetricValues.firstOrNull()?.component?.healthMetricType?.title
            ?: validMetricValues.firstOrNull()?.component?.title
            ?: ""
    }

    val metricTypeCode = remember(validMetricValues) {
        validMetricValues.firstOrNull()?.component?.healthMetricType?.code
            ?: validMetricValues.firstOrNull()?.component?.code
            ?: ""
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateUtils.formatIsoDateTime(record.recordedAt, isPersian),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (metricTypeTitle.isNotBlank()) {
                    val (typeIcon, typeTint) = getMetricIconAndColor(metricTypeCode, metricTypeTitle)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = typeIcon,
                                contentDescription = null,
                                tint = typeTint,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = metricTypeTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))

            validMetricValues.forEach { valDto ->
                val compTitle = valDto.component?.title ?: ""
                val compCode = valDto.component?.code ?: ""
                val compUnit = valDto.component?.unit ?: ""
                val (compIcon, compColor) = getMetricIconAndColor(compCode, compTitle)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = compIcon,
                            contentDescription = null,
                            tint = compColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(text = compTitle, style = MaterialTheme.typography.bodyMedium)
                    }

                    val formattedVal = if (valDto.value % 1.0 == 0.0) {
                        valDto.value.toLong().toString()
                    } else {
                        valDto.value.toString()
                    }
                    val displayVal = if (compUnit.isNotBlank()) "$formattedVal $compUnit" else formattedVal
                    Text(
                        text = if (isPersian) displayVal.toPersianDigits() else displayVal,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onShowDetail,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(stringResource(R.string.details), style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(stringResource(R.string.delete), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordDetailsEditDialog(
    record: HealthRecordIndexViewModel,
    isPersian: Boolean,
    onSave: (id: String, recordedAt: String, updatedValues: Map<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    val validMetricValues = remember(record.metricValues) {
        record.metricValues.filterNotNull()
    }

    val metricTypeTitle = remember(validMetricValues) {
        validMetricValues.firstOrNull()?.component?.healthMetricType?.title ?: ""
    }

    val editedValues = remember(record) {
        mutableStateMapOf<String, String>().apply {
            validMetricValues.forEach { valDto ->
                val formattedVal = if (valDto.value % 1.0 == 0.0) {
                    valDto.value.toLong().toString()
                } else {
                    valDto.value.toString()
                }
                put(valDto.healthMetricComponentId, formattedVal)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.record_details_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (metricTypeTitle.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.select_metric_type), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Text(metricTypeTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("زمان ثبت:", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Text(
                        DateUtils.formatIsoDateTime(record.recordedAt, isPersian),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider()

                Text("ویرایش مقادیر:", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                validMetricValues.forEach { valDto ->
                    val compId = valDto.healthMetricComponentId
                    val compTitle = valDto.component?.title ?: ""
                    val compCode = valDto.component?.code ?: ""
                    val compUnit = valDto.component?.unit ?: ""
                    val currentVal = editedValues[compId] ?: ""

                    val (compIcon, compColor) = getMetricIconAndColor(compCode, compTitle)

                    OutlinedTextField(
                        value = currentVal,
                        onValueChange = { editedValues[compId] = it },
                        label = {
                            val labelText = if (compUnit.isNotBlank()) "$compTitle ($compUnit)" else compTitle
                            Text(labelText)
                        },
                        leadingIcon = {
                            Icon(imageVector = compIcon, contentDescription = null, tint = compColor, modifier = Modifier.size(20.dp))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(record.id, record.recordedAt, editedValues.toMap())
                }
            ) {
                Text(stringResource(R.string.save_changes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
