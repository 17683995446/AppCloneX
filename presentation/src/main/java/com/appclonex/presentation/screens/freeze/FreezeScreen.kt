package com.appclonex.presentation.screens.freeze

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.appclonex.R
import com.appclonex.domain.model.ClonedApplicationInformationModel
import com.appclonex.infrastructure.caching.ApplicationIconModel
import com.appclonex.presentation.screen.freeze.ApplicationFreezingScreenViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FreezeScreen(
    onNavigateBack: () -> Unit,
    viewModel: ApplicationFreezingScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadApps()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.freeze_title)) },
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
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = { viewModel.unfreezeAll() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.unfreeze_all),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                FloatingActionButton(
                    onClick = { viewModel.freezeAll() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.freeze_all),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading && uiState.clonedApps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Summary row
                    Text(
                        text = stringResource(R.string.freeze_summary, uiState.frozenCount, uiState.totalCount),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Processing indicator
                    if (uiState.isProcessing) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                            Text(
                                text = stringResource(R.string.loading),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // App list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.clonedApps,
                            key = { it.packageName }
                        ) { app ->
                            FreezeAppItem(
                                app = app,
                                isFrozen = uiState.frozenApps[app.packageName] == true,
                                onToggleFreeze = { viewModel.toggleFreeze(app.packageName) },
                                modifier = Modifier.animateItemPlacement(
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessLow,
                                        dampingRatio = Spring.DampingRatioMediumBouncy
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FreezeAppItem(
    app: ClonedApplicationInformationModel,
    isFrozen: Boolean,
    onToggleFreeze: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isFrozen) 0.5f else 1f),
        colors = CardDefaults.cardColors(
            containerColor = if (isFrozen) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 应用图标 - 使用 Coil AsyncImage
            AsyncImage(
                model = ApplicationIconModel(app.packageName),
                contentDescription = app.appName,
                modifier = Modifier.size(40.dp),
                placeholder = rememberAsyncImagePainter(
                    model = null,
                    placeholder = painterResource(android.R.drawable.sym_def_app_icon)
                ),
                error = rememberAsyncImagePainter(
                    model = null,
                    placeholder = painterResource(android.R.drawable.sym_def_app_icon)
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isFrozen,
                onCheckedChange = { onToggleFreeze() }
            )
        }
    }
}
