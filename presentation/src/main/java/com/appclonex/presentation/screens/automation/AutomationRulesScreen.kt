package com.appclonex.presentation.screens.automation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appclonex.R
import com.appclonex.domain.model.AutomationRule
import com.appclonex.domain.model.AutoFreezeConfig
import com.appclonex.presentation.screen.automation.AutomationRulesScreenViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AutomationRulesScreen(
    onNavigateBack: () -> Unit,
    viewModel: AutomationRulesScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    if (uiState.showCreateDialog) {
        CreateRuleDialog(
            availableApps = uiState.availableApps,
            onDismiss = { viewModel.dismissCreateDialog() },
            onConfirm = { rule ->
                viewModel.createRule(rule)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.automation_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog() }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.automation_create_rule))
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading && uiState.rules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Auto-freeze config section
                item {
                    AutoFreezeConfigSection(
                        config = uiState.autoFreezeConfig,
                        onConfigChanged = { viewModel.updateAutoFreezeConfig(it) }
                    )
                }

                // Rules list header
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.automation_rules_list),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Rules list
                if (uiState.rules.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.automation_no_rules),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.automation_no_rules_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    items(
                        items = uiState.rules,
                        key = { it.id }
                    ) { rule ->
                        RuleItem(
                            rule = rule,
                            onToggle = { enabled -> viewModel.toggleRule(rule.id, enabled) },
                            onDelete = { viewModel.deleteRule(rule.id) }
                        )
                    }
                }

                // Bottom spacing for FAB
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}

@Composable
private fun AutoFreezeConfigSection(
    config: AutoFreezeConfig,
    onConfigChanged: (AutoFreezeConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.automation_auto_freeze_config),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Enable/disable switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_auto_freeze),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = config.enabled,
                    onCheckedChange = { enabled ->
                        onConfigChanged(config.copy(enabled = enabled))
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Freeze delay slider
            Text(
                text = stringResource(R.string.automation_freeze_delay),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.automation_freeze_delay_minutes, config.delayMinutes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            androidx.compose.material3.Slider(
                value = config.delayMinutes.toFloat(),
                onValueChange = { newValue ->
                    onConfigChanged(config.copy(delayMinutes = newValue.toInt().coerceIn(5, 60)))
                },
                valueRange = 5f..60f,
                steps = 10
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Freeze on screen off toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.automation_trigger_screen_off),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = config.freezeOnScreenOff,
                    onCheckedChange = { enabled ->
                        onConfigChanged(config.copy(freezeOnScreenOff = enabled))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RuleItem(
    rule: AutomationRule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val ruleTypeLabel = when (rule.ruleType) {
        AutomationRule.RuleType.AUTO_FREEZE -> stringResource(R.string.automation_rule_type_auto_freeze)
        AutomationRule.RuleType.AUTO_UNFREEZE -> stringResource(R.string.automation_rule_type_auto_unfreeze)
        AutomationRule.RuleType.AUTO_SWITCH -> stringResource(R.string.automation_rule_type_auto_switch)
    }

    val triggerTypeLabel = when (rule.triggerType) {
        AutomationRule.TriggerType.TIME_BASED -> stringResource(R.string.automation_trigger_time_based)
        AutomationRule.TriggerType.SCREEN_OFF -> stringResource(R.string.automation_trigger_screen_off)
        AutomationRule.TriggerType.APP_BACKGROUND -> stringResource(R.string.automation_trigger_app_background)
        AutomationRule.TriggerType.SCHEDULE -> stringResource(R.string.automation_trigger_time_based)
    }

    val badgeColor = when (rule.ruleType) {
        AutomationRule.RuleType.AUTO_FREEZE -> MaterialTheme.colorScheme.errorContainer
        AutomationRule.RuleType.AUTO_UNFREEZE -> MaterialTheme.colorScheme.primaryContainer
        AutomationRule.RuleType.AUTO_SWITCH -> MaterialTheme.colorScheme.tertiaryContainer
    }

    val badgeTextColor = when (rule.ruleType) {
        AutomationRule.RuleType.AUTO_FREEZE -> MaterialTheme.colorScheme.onErrorContainer
        AutomationRule.RuleType.AUTO_UNFREEZE -> MaterialTheme.colorScheme.onPrimaryContainer
        AutomationRule.RuleType.AUTO_SWITCH -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateItemPlacement(),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.isEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rule.description.ifEmpty { ruleTypeLabel },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ruleTypeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeTextColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = triggerTypeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(
                            R.string.automation_target_apps_count,
                            rule.targetPackageNames.size
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = rule.isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateRuleDialog(
    availableApps: List<com.appclonex.domain.model.ClonedApplicationInformationModel>,
    profileId: Int = 0,
    onDismiss: () -> Unit,
    onConfirm: (AutomationRule) -> Unit
) {
    var ruleName by remember { mutableStateOf("") }
    var selectedRuleType by remember { mutableStateOf(AutomationRule.RuleType.AUTO_FREEZE) }
    var selectedTriggerType by remember { mutableStateOf(AutomationRule.TriggerType.SCREEN_OFF) }
    var selectedApps by remember { mutableStateOf(setOf<String>()) }
    var showAppSelection by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableStateOf(23) }
    var selectedMinute by remember { mutableStateOf(0) }

    // Dropdown states
    var ruleTypeExpanded by remember { mutableStateOf(false) }
    var triggerTypeExpanded by remember { mutableStateOf(false) }

    val ruleTypeOptions = AutomationRule.RuleType.entries
    val triggerTypeOptions = AutomationRule.TriggerType.entries

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.automation_create_rule))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rule name
                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    label = { Text(stringResource(R.string.automation_rule_name)) },
                    placeholder = { Text(stringResource(R.string.automation_rule_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Rule type dropdown
                ExposedDropdownMenuBox(
                    expanded = ruleTypeExpanded,
                    onExpandedChange = { ruleTypeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = when (selectedRuleType) {
                            AutomationRule.RuleType.AUTO_FREEZE -> stringResource(R.string.automation_rule_type_auto_freeze)
                            AutomationRule.RuleType.AUTO_UNFREEZE -> stringResource(R.string.automation_rule_type_auto_unfreeze)
                            AutomationRule.RuleType.AUTO_SWITCH -> stringResource(R.string.automation_rule_type_auto_switch)
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.automation_rule_type)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ruleTypeExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = ruleTypeExpanded,
                        onDismissRequest = { ruleTypeExpanded = false }
                    ) {
                        ruleTypeOptions.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (type) {
                                            AutomationRule.RuleType.AUTO_FREEZE -> stringResource(R.string.automation_rule_type_auto_freeze)
                                            AutomationRule.RuleType.AUTO_UNFREEZE -> stringResource(R.string.automation_rule_type_auto_unfreeze)
                                            AutomationRule.RuleType.AUTO_SWITCH -> stringResource(R.string.automation_rule_type_auto_switch)
                                        }
                                    )
                                },
                                onClick = {
                                    selectedRuleType = type
                                    ruleTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Trigger type dropdown
                ExposedDropdownMenuBox(
                    expanded = triggerTypeExpanded,
                    onExpandedChange = { triggerTypeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = when (selectedTriggerType) {
                            AutomationRule.TriggerType.TIME_BASED -> stringResource(R.string.automation_trigger_time_based)
                            AutomationRule.TriggerType.SCREEN_OFF -> stringResource(R.string.automation_trigger_screen_off)
                            AutomationRule.TriggerType.APP_BACKGROUND -> stringResource(R.string.automation_trigger_app_background)
                            AutomationRule.TriggerType.SCHEDULE -> stringResource(R.string.automation_trigger_time_based)
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.automation_trigger_type)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = triggerTypeExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = triggerTypeExpanded,
                        onDismissRequest = { triggerTypeExpanded = false }
                    ) {
                        triggerTypeOptions.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (type) {
                                            AutomationRule.TriggerType.TIME_BASED -> stringResource(R.string.automation_trigger_time_based)
                                            AutomationRule.TriggerType.SCREEN_OFF -> stringResource(R.string.automation_trigger_screen_off)
                                            AutomationRule.TriggerType.APP_BACKGROUND -> stringResource(R.string.automation_trigger_app_background)
                                            AutomationRule.TriggerType.SCHEDULE -> stringResource(R.string.automation_trigger_time_based)
                                        }
                                    )
                                },
                                onClick = {
                                    selectedTriggerType = type
                                    triggerTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Schedule time picker (for TIME_BASED trigger)
                if (selectedTriggerType == AutomationRule.TriggerType.TIME_BASED ||
                    selectedTriggerType == AutomationRule.TriggerType.SCHEDULE
                ) {
                    Text(
                        text = stringResource(R.string.automation_schedule_time),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.OutlinedButton(onClick = {
                            if (selectedHour < 23) selectedHour++ else selectedHour = 0
                        }) {
                            Text("%02d".format(selectedHour))
                        }
                        Text(":")
                        androidx.compose.material3.OutlinedButton(onClick = {
                            if (selectedMinute < 55) selectedMinute += 5 else selectedMinute = 0
                        }) {
                            Text("%02d".format(selectedMinute))
                        }
                    }
                }

                // Target apps selection
                Text(
                    text = stringResource(R.string.automation_target_apps),
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = { showAppSelection = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (selectedApps.isEmpty()) {
                            stringResource(R.string.automation_select_apps)
                        } else {
                            stringResource(R.string.automation_target_apps_count, selectedApps.size)
                        }
                    )
                }

                // App selection chips
                if (showAppSelection && availableApps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        availableApps.forEach { app ->
                            val isSelected = selectedApps.contains(app.packageName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        selectedApps = if (isSelected) {
                                            selectedApps - app.packageName
                                        } else {
                                            selectedApps + app.packageName
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.getDisplayName(),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else if (showAppSelection && availableApps.isEmpty()) {
                    Text(
                        text = stringResource(R.string.automation_no_apps_available),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val action = when (selectedRuleType) {
                        AutomationRule.RuleType.AUTO_FREEZE -> AutomationRule.ActionType.FREEZE
                        AutomationRule.RuleType.AUTO_UNFREEZE -> AutomationRule.ActionType.UNFREEZE
                        AutomationRule.RuleType.AUTO_SWITCH -> AutomationRule.ActionType.SWITCH_PROFILE
                    }
                    val rule = AutomationRule(
                        profileId = profileId,
                        ruleType = selectedRuleType,
                        triggerType = selectedTriggerType,
                        triggerValue = "%02d:%02d".format(selectedHour, selectedMinute),
                        targetPackageNames = selectedApps.toList(),
                        action = action,
                        description = ruleName
                    )
                    onConfirm(rule)
                }
            ) {
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
