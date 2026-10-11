package com.aj.wafatti.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aj.wafatti.core.designsystem.components.AvatarView
import com.aj.wafatti.core.designsystem.components.LoadingIndicator
import com.aj.wafatti.core.designsystem.components.WafattiButton
import com.aj.wafatti.core.designsystem.components.WafattiTextField
import com.aj.wafatti.core.designsystem.components.WafattiTopBar
import com.aj.wafatti.ui.theme.WafattiSuccess

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            WafattiTopBar(
                title = "Profile",
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        ProfileContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            viewModel = viewModel
        )
    }
}

@Composable
fun ProfileContent(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    if (uiState.isLoading) {
        LoadingIndicator(modifier = modifier)
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AvatarView(
                displayName = uiState.displayNameInput.ifEmpty { uiState.user?.displayName ?: "User" },
                avatarUrl = uiState.user?.avatarUrl,
                size = 96.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            WafattiTextField(
                value = uiState.displayNameInput,
                onValueChange = { viewModel.onDisplayNameChanged(it) },
                label = "Display Name"
            )

            Spacer(modifier = Modifier.height(16.dp))

            WafattiTextField(
                value = uiState.user?.email ?: "",
                onValueChange = {},
                readOnly = true,
                label = "Email"
            )

            Spacer(modifier = Modifier.height(16.dp))

            WafattiTextField(
                value = uiState.phoneNumberInput,
                onValueChange = { viewModel.onPhoneNumberChanged(it) },
                label = "Phone Number",
                placeholder = "e.g. +91 98765 43210"
            )

            Spacer(modifier = Modifier.height(16.dp))

            WafattiTextField(
                value = uiState.bioInput,
                onValueChange = { viewModel.onBioChanged(it) },
                label = "Bio",
                placeholder = "write something about yourself"
            )

            if (uiState.isSuccess) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Profile updated successfully!",
                    color = WafattiSuccess,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            WafattiButton(
                text = "Save Profile",
                onClick = { viewModel.saveProfile() },
                isLoading = uiState.isSaving,
                useGradient = true
            )
        }
    }
}
