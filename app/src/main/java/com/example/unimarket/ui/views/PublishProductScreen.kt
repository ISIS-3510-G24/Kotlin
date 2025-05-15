package com.example.unimarket.ui.views

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.ui.models.ClassItem
import com.example.unimarket.ui.models.Major
import com.example.unimarket.ui.viewmodels.ImageUploadState
import com.example.unimarket.ui.viewmodels.PublishProductViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun PublishProductScreen(
    navController: NavController,
    viewModel: PublishProductViewModel = hiltViewModel()
) {
    val isOnline by viewModel.isOnline.collectAsState()
    val majors by viewModel.majors.collectAsState()
    val classes by viewModel.classes.collectAsState()
    val scope        = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val uiEvents = viewModel.uiEvent

    val uploadState by viewModel.uploadState.collectAsState()

    // Form state
    var selectedMajor by remember { mutableStateOf<Major?>(null) }
    var selectedClass by remember { mutableStateOf<ClassItem?>(null) }
    var expandedMajor by remember { mutableStateOf(false) }
    var expandedClass by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var labelsText by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf<String?>(null)}


    // Listen to UiEvents
    LaunchedEffect(Unit) {
        uiEvents.collect { event ->
            when (event) {
                is PublishProductViewModel.UiEvent.ShowMessage ->
                    snackbar.showSnackbar(event.text)

                PublishProductViewModel.UiEvent.NavigateBack ->
                    navController.popBackStack()
            }
        }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadImage(it)
        }
    }


    Scaffold(
        topBar       = { TopAppBar(title = { Text("Publish Product") }) },
        snackbarHost = { SnackbarHost(hostState = snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isOnline) {
                OfflineBanner(
                    message = "No internet - your product will be published when you reconnect",
                    height = 45.dp
                )
            }
            // ─────────────────────────────────
            // Dropdown de Major
            ExposedDropdownMenuBox(
                expanded = expandedMajor,
                onExpandedChange = { expandedMajor = it }
            ) {
                OutlinedTextField(
                    value = selectedMajor?.name.orEmpty(),
                    onValueChange = { /* no-edit */ },
                    readOnly = true,
                    label = { Text("Major") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expandedMajor)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedMajor,
                    onDismissRequest = { expandedMajor = false },
                    modifier = Modifier.exposedDropdownSize()
                ) {
                    majors.forEach { major ->
                        DropdownMenuItem(
                            text = { Text(major.name) },
                            onClick = {
                                selectedMajor = major
                                expandedMajor = false
                                viewModel.onMajorSelected(major)
                            }
                        )
                    }
                }
            }

            // ─────────────────────────────────
            // Dropdown class
            ExposedDropdownMenuBox(
                expanded = expandedClass,
                onExpandedChange = {
                    if (selectedMajor != null)
                        expandedClass = it
                }
            ) {
                OutlinedTextField(
                    value = selectedClass?.name.orEmpty(),
                    onValueChange = { /* no-edit */ },
                    readOnly = true,
                    enabled = selectedMajor != null,
                    label = { Text("Class") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expandedClass)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedClass,
                    onDismissRequest = { expandedClass = false },
                    modifier = Modifier.exposedDropdownSize()
                ) {
                    classes.forEach { cls ->
                        DropdownMenuItem(
                            text = { Text(cls.name) },
                            onClick = {
                                selectedClass = cls
                                expandedClass = false
                            }
                        )
                    }
                }
            }

            // ─────────────────────────────────
            // Text fields
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("Price") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = labelsText,
                onValueChange = { labelsText = it },
                label = { Text("Labels (comma separated)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { picker.launch("image/*") },
                enabled = isOnline,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Image")
            }
            when (uploadState) {
                is ImageUploadState.Pending -> {
                    CircularProgressIndicator()
                }
                is ImageUploadState.Failed -> {
                    Text("Upload failed", color = MaterialTheme.colorScheme.error)
                }
                is ImageUploadState.Success -> {
                    val url = (uploadState as ImageUploadState.Success).remotePath
                    imageUrl = url
                    Image(
                        painter = rememberAsyncImagePainter(imageUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
                else -> Unit
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull()
                    viewModel.publish(
                        selectedMajor,
                        selectedClass,
                        title,
                        description,
                        price,
                        labelsText.split(",").map(String::trim).filter(String::isNotEmpty),
                        imageUrl ?: ""
                    )
                },
                enabled = true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Publish")
            }
        }
    }
}
