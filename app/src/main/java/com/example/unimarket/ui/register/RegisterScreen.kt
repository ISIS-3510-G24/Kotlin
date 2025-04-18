package com.example.unimarket.ui.register

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.ui.data.FirebaseFirestoreSingleton
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Observe registration state
    var isUploadingImage by remember { mutableStateOf(false) }
    val registerSuccessState = viewModel.registerSuccess.value
    val errorMessageState = viewModel.errorMessage.value
    val profileUrl by viewModel.profilePictureUrl

    // Launcher for image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(GetContent()) { uri: Uri? ->
        uri?.let {
            isUploadingImage = true
            viewModel.uploadProfilePicture(it) { success ->
                isUploadingImage = false
                if (!success) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Failed to upload image. Please try again.",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(registerSuccessState) {
        if (registerSuccessState == true) {
            onRegisterSuccess()
        }
    }

    // Load majors from Firestore
    var majorsList by remember { mutableStateOf(listOf<String>()) }
    LaunchedEffect(Unit) {
        FirebaseFirestoreSingleton.getCollection("majors")
            .get()
            .addOnSuccessListener { result ->
                majorsList = result.documents.mapNotNull { it.getString("name") }
            }
            .addOnFailureListener {
                FirebaseCrashlytics.getInstance().recordException(it)
                majorsList = emptyList()
            }
    }

    var majorDropdownExpanded by remember { mutableStateOf(false) }
    val allPreferences = listOf("Academics", "Education", "Handcrafts", "Sports", "Wellness")

    // Wrap content in a Column with verticalScroll
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Sign up",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Create an account to get started",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )
        Box(
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            val painter = rememberAsyncImagePainter(model = profileUrl)
            Image(
                painter,
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Upload Profile Picture")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = viewModel.displayName.value,
            onValueChange = { viewModel.displayName.value = it },
            label = { Text("Display Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = viewModel.email.value,
            onValueChange = { viewModel.email.value = it },
            label = { Text("Email Address") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = viewModel.bio.value,
            onValueChange = { viewModel.bio.value = it },
            label = { Text("Bio") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = majorDropdownExpanded,
            onExpandedChange = { majorDropdownExpanded = !majorDropdownExpanded }
        ) {
            OutlinedTextField(
                value = viewModel.major.value,
                onValueChange = { /* No direct editing */ },
                readOnly = true,
                label = { Text("Major") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = majorDropdownExpanded) },
            )
            ExposedDropdownMenu(
                expanded = majorDropdownExpanded,
                onDismissRequest = { majorDropdownExpanded = false }
            ) {
                majorsList.forEach { majorOption ->
                    DropdownMenuItem(
                        text = { Text(majorOption) },
                        onClick = {
                            viewModel.major.value = majorOption
                            majorDropdownExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select your preferences:",
            style = MaterialTheme.typography.bodyMedium
        )
        allPreferences.forEach { pref ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                val isSelected = viewModel.preferences.value.contains(pref)
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { checked ->
                        if (checked) {
                            viewModel.preferences.value = viewModel.preferences.value + pref
                        } else {
                            viewModel.preferences.value = viewModel.preferences.value - pref
                        }
                    }
                )
                Text(text = pref, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = viewModel.password.value,
            onValueChange = { viewModel.password.value = it },
            label = { Text("Create a password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = viewModel.confirmPassword.value,
            onValueChange = { viewModel.confirmPassword.value = it },
            label = { Text("Confirm password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Checkbox(
                checked = viewModel.acceptTerms.value,
                onCheckedChange = { viewModel.acceptTerms.value = it }
            )
            Text(
                text = "I’ve read and agree with the Terms and Conditions\nand the Privacy Policy",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.registerUser() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isUploadingImage
        ) {
            if (isUploadingImage) {
                Text("Uploading image...")
            } else {
                Text("Sign up")
            }
        }

        errorMessageState?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
