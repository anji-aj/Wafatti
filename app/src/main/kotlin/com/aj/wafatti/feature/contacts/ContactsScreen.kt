package com.aj.wafatti.feature.contacts

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aj.wafatti.core.designsystem.components.AvatarView
import com.aj.wafatti.core.designsystem.components.LoadingIndicator
import com.aj.wafatti.core.designsystem.components.WafattiButton
import com.aj.wafatti.core.designsystem.components.WafattiTopBar
import com.aj.wafatti.core.utils.PermissionUtils
import com.aj.wafatti.domain.model.ContactFilter
import com.aj.wafatti.domain.model.DeviceContact

@Composable
fun ContactsScreen(
    onBackClick: () -> Unit,
    onUserSelected: (String) -> Unit,
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val isPermanentlyDenied = activity?.let {
            PermissionUtils.isPermissionPermanentlyDenied(it, Manifest.permission.READ_CONTACTS)
        } ?: false
        viewModel.onPermissionResult(isGranted, isPermanentlyDenied)
    }

    LaunchedEffect(Unit) {
        viewModel.checkPermissionStatus(context)
    }

    // In-App Rationale Pre-Permission Explanation Dialog
    if (uiState.permissionStatus == PermissionStatus.SHOW_RATIONALE) {
        AlertDialog(
            onDismissRequest = { viewModel.onNotNowClicked() },
            title = {
                Text(
                    text = "Contacts access",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Allow Wafatti to access your contacts so you can find friends who are already using Wafatti.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onAllowRationaleClicked()
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onNotNowClicked() }) {
                    Text("Not now", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        topBar = {
            WafattiTopBar(
                title = "Contacts",
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when (uiState.permissionStatus) {
                PermissionStatus.GRANTED -> {
                    ContactsContent(
                        uiState = uiState,
                        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        onFilterChanged = { viewModel.onFilterChanged(it) },
                        onUserSelected = onUserSelected,
                        onInviteClicked = { contact -> launchInviteShareSheet(context, contact.name) },
                        onRetryClicked = { viewModel.retryMatching() }
                    )
                }
                PermissionStatus.PERMANENTLY_DENIED -> {
                    PermissionDeniedContent(
                        title = "Contacts permission is disabled",
                        description = "Contacts permission is disabled. Enable it in Settings to find your contacts on Wafatti.",
                        buttonText = "Open Settings",
                        onButtonClick = { PermissionUtils.openAppSettings(context) }
                    )
                }
                PermissionStatus.DENIED, PermissionStatus.NOT_REQUESTED, PermissionStatus.SHOW_RATIONALE -> {
                    PermissionDeniedContent(
                        title = "Contacts permission is needed",
                        description = "Contacts permission is needed to find your friends on Wafatti.",
                        buttonText = "Allow Contacts Access",
                        onButtonClick = {
                            val isPermanentlyDenied = activity?.let {
                                PermissionUtils.isPermissionPermanentlyDenied(it, Manifest.permission.READ_CONTACTS)
                            } ?: false
                            if (isPermanentlyDenied) {
                                PermissionUtils.openAppSettings(context)
                            } else {
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ContactsContent(
    uiState: ContactsUiState,
    onSearchQueryChanged: (String) -> Unit,
    onFilterChanged: (ContactFilter) -> Unit,
    onUserSelected: (String) -> Unit,
    onInviteClicked: (DeviceContact) -> Unit,
    onRetryClicked: () -> Unit
) {
    val filteredContacts = remember(uiState.contacts, uiState.searchQuery, uiState.filter) {
        uiState.contacts.filter { contact ->
            val matchesQuery = uiState.searchQuery.isBlank() ||
                    contact.name.contains(uiState.searchQuery, ignoreCase = true) ||
                    contact.phoneNumber.contains(uiState.searchQuery, ignoreCase = true)

            val matchesFilter = when (uiState.filter) {
                ContactFilter.ALL -> true
                ContactFilter.WAFATTI -> contact.isWafattiUser
                ContactFilter.INVITE -> !contact.isWafattiUser
            }

            matchesQuery && matchesFilter
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = {
                Text(
                    text = "Search contacts...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            } else null,
            singleLine = true,
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Filter chips: All | Wafatti | Invite
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ContactFilter.entries.forEach { filterOption ->
                val isSelected = uiState.filter == filterOption
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterChanged(filterOption) },
                    label = {
                        Text(
                            text = when (filterOption) {
                                ContactFilter.ALL -> "All"
                                ContactFilter.WAFATTI -> "Wafatti"
                                ContactFilter.INVITE -> "Invite"
                            },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LoadingIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Checking your contacts...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.error,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRetryClicked,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }
        } else if (filteredContacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        uiState.contacts.isEmpty() -> "No contacts found"
                        uiState.filter == ContactFilter.WAFATTI -> "No Wafatti contacts yet\nInvite your friends to start chatting."
                        else -> "No contacts match your search"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredContacts, key = { it.id + "_" + it.phoneNumber }) { contact ->
                    ContactRowItem(
                        contact = contact,
                        onMessageClicked = {
                            val userId = contact.wafattiUserId ?: contact.id
                            onUserSelected(userId)
                        },
                        onInviteClicked = { onInviteClicked(contact) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
fun ContactRowItem(
    contact: DeviceContact,
    onMessageClicked: () -> Unit,
    onInviteClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarView(
            displayName = contact.name,
            avatarUrl = contact.photoUri,
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (contact.isWafattiUser) "Wafatti" else "Not on Wafatti",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (contact.isWafattiUser) FontWeight.SemiBold else FontWeight.Normal,
                color = if (contact.isWafattiUser) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (contact.isWafattiUser) {
            Button(
                onClick = onMessageClicked,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Message", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            OutlinedButton(
                onClick = onInviteClicked,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Invite", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun PermissionDeniedContent(
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Contacts,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                WafattiButton(
                    text = buttonText,
                    onClick = onButtonClick,
                    useGradient = true
                )
            }
        }
    }
}

private fun launchInviteShareSheet(context: Context, contactName: String) {
    val inviteIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            "Join me on Wafatti! Download Wafatti and start chatting with me: https://wafatti.com"
        )
    }
    context.startActivity(Intent.createChooser(inviteIntent, "Invite $contactName to Wafatti"))
}
