package com.example.unimarket.ui.views

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.example.unimarket.ui.models.Product
import com.example.unimarket.ui.viewmodels.ExploreViewModel
import com.example.unimarket.ui.viewmodels.ImageUploadState
import com.google.firebase.Timestamp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun PublishProductScreen(
    navController: NavController,
    exploreViewModel: ExploreViewModel = hiltViewModel()
) {
    val isLoading   by exploreViewModel.isLoading.collectAsState()
    val uploadState by exploreViewModel.uploadState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val scope        = rememberCoroutineScope()

    // Estados para dropdowns
    var majors        by remember { mutableStateOf<List<Major>>(emptyList()) }
    var expandedMajor by remember { mutableStateOf(false) }
    var selectedMajor by remember { mutableStateOf<Major?>(null) }

    var classes       by remember { mutableStateOf<List<ClassItem>>(emptyList()) }
    var expandedClass by remember { mutableStateOf(false) }
    var selectedClass by remember { mutableStateOf<ClassItem?>(null) }

    var title       by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText   by remember { mutableStateOf("") }
    var labelsText  by remember { mutableStateOf("") }

    val imageUri    = remember { mutableStateOf<Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> imageUri.value = uri }

    // Mostrar mensajes de snackbar
    LaunchedEffect(Unit) {
        exploreViewModel.uiEvent.collect { event ->
            when(event) {
                is ExploreViewModel.UIEvent.ShowMessage -> {
                    snackbarHost.showSnackbar(event.message)
                }
                is ExploreViewModel.UIEvent.ProductPublished -> {
                    navController.navigate("explore") {
                        popUpTo("explore") { inclusive = true }
                    }
                }
            }
        }
    }

    // Cuando la imagen se sube con éxito, se construye el Product y se publica
    LaunchedEffect(uploadState) {
        if (uploadState is ImageUploadState.Success) {
            val downloadUrl = (uploadState as ImageUploadState.Success).remotePath
            val price = priceText.toDoubleOrNull() ?: return@LaunchedEffect
            val product = Product(
                classId     = selectedClass!!.id,
                createdAt   = Timestamp.now(),
                description = description,
                imageUrls   = listOf(downloadUrl),
                labels      = labelsText
                    .split(",")
                    .map(String::trim)
                    .filter(String::isNotEmpty),
                majorID     = selectedMajor!!.id,
                price       = price,
                sellerID    = exploreViewModel.getCurrentUserId(),
                status      = "Available",
                title       = title,
                updatedAt   = Timestamp.now()
            )
            exploreViewModel.publishProduct(
                product
            )
        }
    }

    // Cargo lista de majors desde Firestore
    LaunchedEffect(Unit) {
        try {
            val snapshot = Firebase.firestore.collection("majors").get().await()
            majors = snapshot.documents
                .mapNotNull { it.toObject(Major::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            snackbarHost.showSnackbar("Failed to load majors")
        }
    }

    // Cuando cambia selectedMajor, cargo las clases asociadas
    LaunchedEffect(selectedMajor) {
        selectedClass = null
        selectedMajor?.let { major ->
            try {
                val snapshot = Firebase.firestore
                    .collection("majors")
                    .document(major.id)
                    .collection("clases")
                    .get()
                    .await()
                classes = snapshot.documents
                    .mapNotNull { it.toObject(ClassItem::class.java)?.copy(id = it.id) }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                snackbarHost.showSnackbar("Failed to load classes")
            }
        }
    }

    Scaffold(
        topBar       = { TopAppBar(title = { Text("Publish Product") }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ─────────────────────────────────
            // Dropdown de Major
            ExposedDropdownMenuBox(
                expanded = expandedMajor,
                onExpandedChange = { expandedMajor = !expandedMajor }
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
                        .menuAnchor()              // <<—— ¡Muy importante!
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
                            }
                        )
                    }
                }
            }

            // ─────────────────────────────────
            // Dropdown de Class (semánticamente igual al anterior)
            ExposedDropdownMenuBox(
                expanded = expandedClass,
                onExpandedChange = {
                    if (selectedMajor != null && classes.isNotEmpty())
                        expandedClass = !expandedClass
                }
            ) {
                OutlinedTextField(
                    value = selectedClass?.name.orEmpty(),
                    onValueChange = { /* no-edit */ },
                    readOnly = true,
                    enabled = selectedMajor != null && classes.isNotEmpty(),
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
            // Resto de campos
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
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Image")
            }
            imageUri.value?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull()
                    if (selectedMajor != null && selectedClass != null
                        && title.isNotBlank()
                        && description.isNotBlank()
                        && price != null
                        && imageUri.value != null
                    ) {
                        Log.d(
                            "PublishScreen",
                            "CLICK! major=$selectedMajor, class=$selectedClass, title='$title', uri=${imageUri.value}"
                        )
                        exploreViewModel.uploadProductImage(imageUri.value!!)
                    } else {
                        scope.launch {
                            snackbarHost.showSnackbar("Please fill all fields correctly.")
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Publishing..." else "Publish")
            }
        }
    }
}
