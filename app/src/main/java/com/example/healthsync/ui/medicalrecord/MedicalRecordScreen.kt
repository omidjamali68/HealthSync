package com.example.healthsync.ui.medicalrecord

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.healthsync.R
import com.example.healthsync.domain.model.MedicalRecordCategory
import com.example.healthsync.domain.model.MedicalRecordDto
import com.example.healthsync.domain.model.MedicalRecordFileDto
import com.example.healthsync.ui.Routes
import com.example.healthsync.ui.components.AppBottomNavigation
import com.example.healthsync.util.DateUtils
import com.example.healthsync.util.DateUtils.toEnglishDigits
import com.example.healthsync.util.DateUtils.toPersianDigits
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

@Composable
private fun getCategoryIconAndColor(category: MedicalRecordCategory): Pair<ImageVector, Color> {
    return when (category) {
        MedicalRecordCategory.VISIT -> Icons.Default.MedicalServices to Color(0xFF00ACC1)
        MedicalRecordCategory.LAB -> Icons.Default.Science to Color(0xFF1E88E5)
        MedicalRecordCategory.PRESCRIPTION -> Icons.Default.LocalPharmacy to Color(0xFF43A047)
        MedicalRecordCategory.SONOGRAPHY -> Icons.Default.Image to Color(0xFF8E24AA)
        MedicalRecordCategory.MRI -> Icons.Default.FolderSpecial to Color(0xFFFB8C00)
    }
}

@Composable
private fun getFileIcon(extension: String?, fileName: String): ImageVector {
    val ext = (extension ?: fileName.substringAfterLast('.', "")).lowercase()
    return when {
        ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp") -> Icons.Default.Image
        ext == "pdf" -> Icons.Default.PictureAsPdf
        else -> Icons.Default.AttachFile
    }
}

fun openFileInExternalViewer(context: Context, baseUrl: String, filePathOrUrl: String?) {
    if (filePathOrUrl.isNullOrBlank()) {
        Toast.makeText(context, "آدرس فایل معتبر نیست.", Toast.LENGTH_SHORT).show()
        return
    }
    val fullUrl = if (filePathOrUrl.startsWith("http://") || filePathOrUrl.startsWith("https://")) {
        filePathOrUrl
    } else {
        val cleanBase = baseUrl.trimEnd('/').removeSuffix("/api")
        val cleanPath = filePathOrUrl.replace("\\", "/").removePrefix("wwwroot/").removePrefix("/")
        "$cleanBase/$cleanPath"
    }

    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "امکان باز کردن فایل وجود ندارد.", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersianDatePickerDialog(
    initialDateStr: String? = null,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val (initYear, initMonth, initDay) = remember(initialDateStr) {
        if (!initialDateStr.isNullOrBlank()) {
            val clean = initialDateStr.trim().toEnglishDigits()
            val parts = clean.split("/", "-")
            if (parts.size == 3) {
                val y = parts[0].toIntOrNull()
                val m = parts[1].toIntOrNull()
                val d = parts[2].toIntOrNull()
                if (y != null && m != null && d != null && y in 1300..1500 && m in 1..12 && d in 1..31) {
                    Triple(y, m, d)
                } else null
            } else null
        } else null
    } ?: run {
        val zdt = ZonedDateTime.now(ZoneId.of("Asia/Tehran"))
        val (y, m, d) = DateUtils.gregorianToJalali(zdt.year, zdt.monthValue, zdt.dayOfMonth)
        Triple(y, m, d)
    }

    var selectedYear by remember { mutableIntStateOf(initYear) }
    var selectedMonth by remember { mutableIntStateOf(initMonth) }
    var selectedDay by remember { mutableIntStateOf(initDay) }

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
    initialDateStr: String? = null,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = remember(initialDateStr) {
        if (!initialDateStr.isNullOrBlank()) {
            val gregStr = DateUtils.convertJalaliStringToGregorian(initialDateStr)
            val zdt = DateUtils.parseIsoToZonedDateTime(gregStr)
            zdt?.toInstant()?.toEpochMilli()
        } else null
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

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
fun AppDatePickerDialog(
    isPersian: Boolean,
    initialDateStr: String? = null,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (isPersian) {
        PersianDatePickerDialog(
            initialDateStr = initialDateStr,
            onDateSelected = onDateSelected,
            onDismiss = onDismiss
        )
    } else {
        GregorianDatePickerDialog(
            initialDateStr = initialDateStr,
            onDateSelected = onDateSelected,
            onDismiss = onDismiss
        )
    }
}

// Modal Dialog for File Interpretation matching _FileInterpretationModal in Razor view
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileInterpretationModal(
    fileName: String?,
    interpretationText: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onReInterpret: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "نتیجه تفسیر فایل با هوش مصنوعی",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!fileName.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = fileName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                HorizontalDivider()

                if (isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "در حال دریافت و تفسیر فایل با هوش مصنوعی…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (!interpretationText.isNullOrBlank()) {
                    Text(
                        text = interpretationText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = "نتیجه تفسیری برای این فایل دریافت نشد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReInterpret,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.re_interpret_file), style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.close), fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordScreen(
    onBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit = {},
    vm: MedicalRecordViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    val isPersian = DateUtils.isPersianMode()
    val snackbarHostState = remember { SnackbarHostState() }
    var deleteConfirmRecordId by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            vm.addSelectedFiles(uris)
        }
    }

    LaunchedEffect(s.message) {
        s.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.clearMessage()
        }
    }

    if (deleteConfirmRecordId != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmRecordId = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_medical_record_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val idToDelete = deleteConfirmRecordId!!
                        deleteConfirmRecordId = null
                        vm.deleteRecord(idToDelete)
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmRecordId = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (s.selectedRecordDetail != null) {
        MedicalRecordDetailSheet(
            record = s.selectedRecordDetail!!,
            activeInterpretation = s.activeFileInterpretation,
            interpretationLoading = s.interpretationLoading,
            activeFileId = s.activeFileId,
            baseUrl = vm.baseUrl,
            onDismiss = { vm.selectRecordDetail(null) },
            onGetInterpretation = vm::openFileInterpretationModal,
            onReInterpret = vm::reInterpretFile
        )
    }

    if (s.showInterpretationModal) {
        FileInterpretationModal(
            fileName = s.modalFileName,
            interpretationText = s.activeFileInterpretation,
            isLoading = s.interpretationLoading,
            onDismiss = vm::closeFileInterpretationModal,
            onReInterpret = vm::reInterpretModalFile
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.medical_records_title), color = Color.White) },
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
        bottomBar = {
            AppBottomNavigation(
                currentRoute = Routes.MEDICAL_RECORD,
                onNavigate = onNavigateToRoute
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
            TabRow(
                selectedTabIndex = s.selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = s.selectedTab == 0,
                    onClick = { vm.selectTab(0) },
                    text = { Text(stringResource(R.string.medical_records_history), fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = s.selectedTab == 1,
                    onClick = { vm.selectTab(1) },
                    text = {
                        Text(
                            stringResource(
                                if (s.editingRecordId != null) R.string.edit_medical_record else R.string.new_medical_record
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }

            if (s.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when (s.selectedTab) {
                0 -> MedicalRecordListSection(
                    state = s,
                    isPersian = isPersian,
                    onSearchQueryChange = vm::setSearchQuery,
                    onCategorySelect = vm::setSelectedCategory,
                    onFilterDateChange = vm::setFilterDateQuery,
                    onApplyFilters = vm::applyFilters,
                    onClearFilters = vm::clearFilters,
                    onGoToPage = vm::goToPage,
                    onSelectDetail = vm::selectRecordDetail,
                    onEditRecord = vm::prepareEditForm,
                    onDeleteRecord = { deleteConfirmRecordId = it }
                )

                1 -> MedicalRecordFormSection(
                    state = s,
                    isPersian = isPersian,
                    baseUrl = vm.baseUrl,
                    onTitleChange = vm::updateFormTitle,
                    onCategoryChange = vm::updateFormCategory,
                    onRecordDateChange = vm::updateFormRecordDate,
                    onDescriptionChange = vm::updateFormDescription,
                    onDoctorNameChange = vm::updateFormDoctorName,
                    onCenterNameChange = vm::updateFormCenterName,
                    onPickFiles = { filePickerLauncher.launch("*/*") },
                    onRemoveExistingFile = vm::deleteExistingFile,
                    onRemoveSelectedFile = vm::removeSelectedFile,
                    onGetInterpretation = vm::openFileInterpretationModal,
                    onReInterpret = vm::reInterpretFile,
                    onClearForm = vm::clearForm,
                    onSubmitForm = vm::submitForm
                )
            }
        }
    }
}

@Composable
private fun MedicalRecordListSection(
    state: MedicalRecordUiState,
    isPersian: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (MedicalRecordCategory?) -> Unit,
    onFilterDateChange: (String) -> Unit,
    onApplyFilters: () -> Unit,
    onClearFilters: () -> Unit,
    onGoToPage: (Int) -> Unit,
    onSelectDetail: (MedicalRecordDto) -> Unit,
    onEditRecord: (MedicalRecordDto) -> Unit,
    onDeleteRecord: (String) -> Unit
) {
    var isFilterExpanded by remember { mutableStateOf(false) }
    var showFilterDatePicker by remember { mutableStateOf(false) }

    if (showFilterDatePicker) {
        AppDatePickerDialog(
            isPersian = isPersian,
            initialDateStr = state.filterDateQuery,
            onDateSelected = { selectedDate ->
                onFilterDateChange(selectedDate)
            },
            onDismiss = { showFilterDatePicker = false }
        )
    }

    val activeFilterCount = listOfNotNull(
        state.searchQuery.ifBlank { null },
        state.selectedCategory,
        state.filterDateQuery.ifBlank { null }
    ).size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Collapsible Filter Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Clickable Header to Expand/Collapse
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isFilterExpanded = !isFilterExpanded }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "فیلتر و جستجوی پرونده‌ها",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (activeFilterCount > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "$activeFilterCount فیلتر فعال".let { if (isPersian) it.toPersianDigits() else it },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Icon(
                        imageVector = if (isFilterExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Collapsible Content
                AnimatedVisibility(visible = isFilterExpanded) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                        // Category Chips ON TOP
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "فیلتر بر اساس دسته‌بندی:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    FilterChip(
                                        selected = state.selectedCategory == null,
                                        onClick = { onCategorySelect(null) },
                                        label = { Text(stringResource(R.string.all_categories), style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                                items(MedicalRecordCategory.entries.toTypedArray()) { cat ->
                                    val (icon, color) = getCategoryIconAndColor(cat)
                                    FilterChip(
                                        selected = state.selectedCategory == cat,
                                        onClick = { onCategorySelect(cat) },
                                        leadingIcon = { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp)) },
                                        label = { Text(cat.title, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }

                        // Title / Description Search Input
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.filter_title_category_label),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = onSearchQueryChange,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(stringResource(R.string.filter_title_category_placeholder)) },
                                trailingIcon = {
                                    if (state.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchQueryChange("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = null)
                                        }
                                    } else {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        // Filter by Date
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.filter_date_header_label),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = state.filterDateQuery,
                                onValueChange = onFilterDateChange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showFilterDatePicker = true },
                                placeholder = { Text(stringResource(R.string.filter_date_placeholder)) },
                                trailingIcon = {
                                    IconButton(onClick = { showFilterDatePicker = true }) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        // Action Buttons (Apply & Clear)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    isFilterExpanded = false
                                    onApplyFilters()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = stringResource(R.string.apply_filter),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            Button(
                                onClick = onClearFilters,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B))
                            ) {
                                Text(
                                    text = stringResource(R.string.clear_filter),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Records List
        if (state.records.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.no_medical_records_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.records, key = { it.id }) { record ->
                    MedicalRecordItemCard(
                        record = record,
                        isPersian = isPersian,
                        onDetailClick = { onSelectDetail(record) },
                        onEditClick = { onEditRecord(record) },
                        onDeleteClick = { onDeleteRecord(record.id) }
                    )
                }
            }
        }

        // Pagination Controls
        if (state.totalPages > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onGoToPage(state.currentPage - 1) },
                    enabled = state.currentPage > 1
                ) {
                    Text(stringResource(R.string.previous))
                }

                val pageText = stringResource(
                    R.string.page_indicator,
                    state.currentPage,
                    state.totalPages,
                    state.totalCount
                ).let { if (isPersian) it.toPersianDigits() else it }

                Text(pageText, style = MaterialTheme.typography.bodyMedium)

                OutlinedButton(
                    onClick = { onGoToPage(state.currentPage + 1) },
                    enabled = state.currentPage < state.totalPages
                ) {
                    Text(stringResource(R.string.next))
                }
            }
        }
    }
}

@Composable
private fun MedicalRecordItemCard(
    record: MedicalRecordDto,
    isPersian: Boolean,
    onDetailClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val category = MedicalRecordCategory.fromValue(record.category)
    val (icon, color) = getCategoryIconAndColor(category)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetailClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = color.copy(alpha = 0.15f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column {
                        Text(
                            text = record.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = color
                        )
                    }
                }

                if (record.files.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            val countText = "${record.files.size} فایل".let { if (isPersian) it.toPersianDigits() else it }
                            Text(
                                countText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

            if (!record.recordDate.isNullOrBlank()) {
                val formattedDate = DateUtils.formatIsoDateTime(record.recordDate, isPersian)
                Text(
                    text = "تاریخ: $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (!record.doctorName.isNullOrBlank() || !record.centerName.isNullOrBlank()) {
                val doctorCenterInfo = listOfNotNull(record.doctorName, record.centerName).joinToString(" | ")
                Text(
                    text = doctorCenterInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFC62828))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicalRecordFormSection(
    state: MedicalRecordUiState,
    isPersian: Boolean,
    baseUrl: String,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (MedicalRecordCategory) -> Unit,
    onRecordDateChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDoctorNameChange: (String) -> Unit,
    onCenterNameChange: (String) -> Unit,
    onPickFiles: () -> Unit,
    onRemoveExistingFile: (String) -> Unit,
    onRemoveSelectedFile: (Uri) -> Unit,
    onGetInterpretation: (String, String) -> Unit,
    onReInterpret: (String) -> Unit,
    onClearForm: () -> Unit,
    onSubmitForm: () -> Unit
) {
    val scrollState = rememberScrollState()
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var showFormDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showFormDatePicker) {
        AppDatePickerDialog(
            isPersian = isPersian,
            initialDateStr = state.formRecordDate,
            onDateSelected = { selectedDate ->
                onRecordDateChange(selectedDate)
            },
            onDismiss = { showFormDatePicker = false }
        )
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                val currentDesc = state.formDescription
                val updatedDesc = if (currentDesc.isBlank()) spokenText else "$currentDesc $spokenText"
                onDescriptionChange(updatedDesc)
            }
        }
    }

    fun launchSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "توضیحات خود را بگویید…")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "سرویس تشخیص گفتار در این دستگاه پشتیبانی نمی‌شود.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Blue Banner
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(
                                if (state.editingRecordId != null) R.string.edit_medical_record else R.string.add_medical_history_title
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Field 1: Title
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.record_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = state.formTitle,
                            onValueChange = onTitleChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.title_placeholder)) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Field 2: Category Dropdown
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.record_category),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        ExposedDropdownMenuBox(
                            expanded = categoryDropdownExpanded,
                            onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = state.formCategory.title,
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text(stringResource(R.string.category_select_placeholder)) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false }
                            ) {
                                MedicalRecordCategory.entries.forEach { cat ->
                                    val (icon, color) = getCategoryIconAndColor(cat)
                                    DropdownMenuItem(
                                        text = { Text(cat.title, fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp)) },
                                        onClick = {
                                            onCategoryChange(cat)
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Field 3: Description with Mic & Clear Toolbar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.description),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { launchSpeechToText() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "ضبط صدا",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (state.formDescription.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onDescriptionChange("") },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "پاک کردن",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = state.formDescription,
                            onValueChange = onDescriptionChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp),
                            placeholder = { Text(stringResource(R.string.description_placeholder)) },
                            shape = RoundedCornerShape(8.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.voice_mic_permission_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }

                    // Field 4: Date
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.record_date),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = state.formRecordDate,
                            onValueChange = onRecordDateChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFormDatePicker = true },
                            placeholder = { Text(stringResource(R.string.date_select_placeholder)) },
                            trailingIcon = {
                                IconButton(onClick = { showFormDatePicker = true }) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Display Existing Uploaded Files when Editing (Matching Razor View filesContainer)
                    if (state.editingRecordId != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "فایل‌های ضمیمه:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (state.existingFiles.isEmpty()) {
                                Text(
                                    text = "فایلی ثبت نشده است.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.existingFiles.forEach { file ->
                                        MedicalRecordFileItem(
                                            file = file,
                                            activeFileId = state.activeFileId,
                                            interpretationLoading = state.interpretationLoading,
                                            activeInterpretation = if (state.activeFileId == file.id) state.activeFileInterpretation else file.interpretation,
                                            onFileClick = {
                                                openFileInExternalViewer(context, baseUrl, file.filePath ?: file.fileUrl)
                                            },
                                            onGetInterpretation = { onGetInterpretation(file.id, file.fileName.ifBlank { "فایل ضمیمه" }) },
                                            onReInterpret = { onReInterpret(file.id) },
                                            onDeleteFile = { onRemoveExistingFile(file.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Field 5: File Attachment Box (New Uploads)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (state.editingRecordId != null) "افزودن فایل جدید:" else stringResource(R.string.attached_files),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawWithContent {
                                    drawContent()
                                    drawRoundRect(
                                        color = borderColor,
                                        style = Stroke(
                                            width = 2.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        )
                                    )
                                }
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    val filesText = if (state.selectedFileUris.isEmpty()) {
                                        stringResource(R.string.no_file_selected)
                                    } else {
                                        "${state.selectedFileUris.size} فایل جدید انتخاب شده"
                                    }
                                    Text(
                                        text = filesText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (state.selectedFileUris.isEmpty()) Color.Gray else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Button(
                                    onClick = onPickFiles,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(stringResource(R.string.select_file), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Selected Files Preview List
                        if (state.selectedFileUris.isNotEmpty()) {
                            Column(
                                modifier = Modifier.padding(top = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                state.selectedFileUris.forEach { uri ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.Default.AttachFile,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = uri.lastPathSegment ?: uri.toString(),
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = { onRemoveSelectedFile(uri) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Action Buttons (Save & Back)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onSubmitForm,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !state.isLoading
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.save_button),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        OutlinedButton(
                            onClick = onClearForm,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.back_button),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicalRecordDetailSheet(
    record: MedicalRecordDto,
    activeInterpretation: String?,
    interpretationLoading: Boolean,
    activeFileId: String?,
    baseUrl: String,
    onDismiss: () -> Unit,
    onGetInterpretation: (String, String) -> Unit,
    onReInterpret: (String) -> Unit
) {
    val category = MedicalRecordCategory.fromValue(record.category)
    val (icon, color) = getCategoryIconAndColor(category)
    val isPersian = DateUtils.isPersianMode()
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = color.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                    }
                }
                Column {
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = color
                    )
                }
            }

            HorizontalDivider()

            if (!record.recordDate.isNullOrBlank()) {
                val formattedDate = DateUtils.formatIsoDateTime(record.recordDate, isPersian)
                Text("تاریخ ثبت: $formattedDate", style = MaterialTheme.typography.bodyMedium)
            }

            if (!record.doctorName.isNullOrBlank()) {
                Text("پزشک معالج: ${record.doctorName}", style = MaterialTheme.typography.bodyMedium)
            }

            if (!record.centerName.isNullOrBlank()) {
                Text("مرکز درمانی: ${record.centerName}", style = MaterialTheme.typography.bodyMedium)
            }

            if (!record.description.isNullOrBlank()) {
                Text(
                    text = "توضیحات: ${record.description}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (record.files.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.attached_files),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    record.files.forEach { file ->
                        MedicalRecordFileItem(
                            file = file,
                            activeFileId = activeFileId,
                            interpretationLoading = interpretationLoading,
                            activeInterpretation = if (activeFileId == file.id) activeInterpretation else file.interpretation,
                            onFileClick = {
                                openFileInExternalViewer(context, baseUrl, file.filePath ?: file.fileUrl)
                            },
                            onGetInterpretation = { onGetInterpretation(file.id, file.fileName.ifBlank { "فایل ضمیمه" }) },
                            onReInterpret = { onReInterpret(file.id) }
                        )
                    }
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.close))
            }
        }
    }
}

@Composable
private fun MedicalRecordFileItem(
    file: MedicalRecordFileDto,
    activeFileId: String?,
    interpretationLoading: Boolean,
    activeInterpretation: String?,
    onFileClick: (() -> Unit)? = null,
    onGetInterpretation: () -> Unit,
    onReInterpret: () -> Unit,
    onDeleteFile: (() -> Unit)? = null
) {
    val isCurrentFile = activeFileId == file.id
    val isInterpreting = isCurrentFile && interpretationLoading
    val fileIcon = getFileIcon(file.extension, file.fileName)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onFileClick != null) Modifier.clickable { onFileClick() } else Modifier),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(fileIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Text(
                        text = file.fileName.ifBlank { "مدرک پزشکی" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = onGetInterpretation,
                        enabled = !isInterpreting,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.ai_interpretation), style = MaterialTheme.typography.labelSmall)
                    }

                    if (onDeleteFile != null) {
                        OutlinedButton(
                            onClick = onDeleteFile,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (isInterpreting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("در حال تحلیل فایل با هوش مصنوعی…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (!activeInterpretation.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text(
                                    stringResource(R.string.file_interpretation_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = onReInterpret, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            text = activeInterpretation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
