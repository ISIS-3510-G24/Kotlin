package com.example.unimarket.ui.views

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.ui.data.FirebaseFirestoreSingleton
import com.example.unimarket.ui.viewModels.ExploreViewModel
import com.example.unimarket.ui.models.Major
import com.example.unimarket.ui.models.Product
import com.google.firebase.Timestamp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.launch

// Data class to represent a class item from Firestore.
// Each document's ID is used as the id and it contains an attribute "name".
data class ClassItem(
    val id: String = "",
    val name: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishProductScreen(
    navController: NavController,
    exploreViewModel: ExploreViewModel = viewModel()
) {
    // ------------------------------------------------------------------------------------------
    // States for dropdowns and user inputs
    // ------------------------------------------------------------------------------------------
    // Major dropdown states
    var majors by remember { mutableStateOf<List<Major>>(emptyList()) }
    var expandedMajor by remember { mutableStateOf(false) }
    var selectedMajor by remember { mutableStateOf<Major?>(null) }

    // Class dropdown states (now using ClassItem)
    var classes by remember { mutableStateOf<List<ClassItem>>(emptyList()) }
    var expandedClass by remember { mutableStateOf(false) }
    var selectedClass by remember { mutableStateOf<ClassItem?>(null) }

    // Other product fields
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var labelsText by remember { mutableStateOf("") } // Labels separated by commas

    // Image picking state (replaces manual Image URL)
    val imageUri = remember { mutableStateOf<Uri?>(null) }

    // Snackbar and loading states
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoading by exploreViewModel.isLoading.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // ------------------------------------------------------------------------------------------
    // Image picker launcher: launches gallery to pick an image
    // ------------------------------------------------------------------------------------------
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri.value = uri
    }

    // ------------------------------------------------------------------------------------------
    // Load majors from Firestore only once.
    // ------------------------------------------------------------------------------------------
    LaunchedEffect(Unit) {
        FirebaseFirestoreSingleton.getCollection("majors")
            .get()
            .addOnSuccessListener { result ->
                val majorList = result.documents.mapNotNull { doc ->
                    // Now, the major's id is handled with the field "id"
                    doc.toObject(Major::class.java)?.copy(id = doc.id)
                }
                majors = majorList
            }
    }

    // ------------------------------------------------------------------------------------------
    // Load classes based on the selected major from the "clases" subcollection.
    // Each class document's ID is used and its "name" attribute is read.
    // ------------------------------------------------------------------------------------------
    LaunchedEffect(selectedMajor) {
        selectedMajor?.let { major ->
            FirebaseFirestoreSingleton.getCollection("majors")
                .document(major.id) // Use "id" instead of "majorID"
                .collection("clases")  // Ensure this matches your Firestore structure
                .get()
                .addOnSuccessListener { result ->
                    val classList = result.documents.mapNotNull { doc ->
                        doc.toObject(ClassItem::class.java)?.copy(id = doc.id)
                    }
                    classes = classList
                }
                .addOnFailureListener {
                    FirebaseCrashlytics.getInstance().recordException(it)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Failed to load classes.")
                    }
                }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Publish Product") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // --------------------------------------------------------------------------------------
            // Major selection dropdown
            // --------------------------------------------------------------------------------------
            ExposedDropdownMenuBox(
                expanded = expandedMajor,
                onExpandedChange = { expandedMajor = !expandedMajor }
            ) {
                OutlinedTextField(
                    value = selectedMajor?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Major") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMajor)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedMajor,
                    onDismissRequest = { expandedMajor = false }
                ) {
                    majors.forEach { major ->
                        DropdownMenuItem(
                            text = { Text(major.name) },
                            onClick = {
                                selectedMajor = major
                                expandedMajor = false
                                // Reset the selected class when a new major is picked
                                selectedClass = null
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // --------------------------------------------------------------------------------------
            // Class selection dropdown
            // Enabled only if a major is selected and classes have been loaded
            // --------------------------------------------------------------------------------------
            ExposedDropdownMenuBox(
                expanded = expandedClass,
                onExpandedChange = {
                    if (selectedMajor != null && classes.isNotEmpty()) {
                        expandedClass = !expandedClass
                    }
                }
            ) {
                OutlinedTextField(
                    value = selectedClass?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Class ID") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedClass)
                    },
                    enabled = selectedMajor != null && classes.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedClass,
                    onDismissRequest = { expandedClass = false }
                ) {
                    classes.forEach { classItem ->
                        DropdownMenuItem(
                            text = { Text(classItem.name) },
                            onClick = {
                                selectedClass = classItem
                                expandedClass = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // --------------------------------------------------------------------------------------
            // Title, description, price, and labels input fields
            // --------------------------------------------------------------------------------------
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("Price") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = labelsText,
                onValueChange = { labelsText = it },
                label = { Text("Labels (comma separated)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // --------------------------------------------------------------------------------------
            // Image picker: instead of an Image URL, allow the user to select an image.
            // --------------------------------------------------------------------------------------
            Button(
                onClick = {
                    // Launch the gallery to pick an image
                    imagePickerLauncher.launch("image/*")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Image")
            }

            // Show image preview if an image has been selected
            imageUri.value?.let { uri ->
                Spacer(modifier = Modifier.height(8.dp))
                val painter = rememberAsyncImagePainter(
                    model = uri,
                    contentScale = ContentScale.Crop
                )
                Image(
                    painter = painter,
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --------------------------------------------------------------------------------------
            // Publish button: validate inputs and publish the product.
            // --------------------------------------------------------------------------------------
            Button(
                onClick = {
                    // Validate input fields and publish the product
                    val price = priceText.toDoubleOrNull()

                    // TODO: Upload image to Firebase Storage and obtain a download URL.
                    // For now, we assume the imageUri is not null and use its URI string.
                    if (
                        selectedMajor != null &&
                        selectedClass != null &&
                        title.isNotBlank() &&
                        description.isNotBlank() &&
                        price != null &&
                        imageUri.value != null
                    ) {
                        val labels = labelsText
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }

                        val finalImageUrl = imageUri.value.toString()

                        val newProduct = Product(
                            classId = selectedClass!!.id,
                            createdAt = Timestamp.now(),
                            description = description,
                            imageUrls = listOf(finalImageUrl),
                            labels = labels,
                            majorID = selectedMajor!!.id, // Use the major's id here
                            price = price,
                            sellerID = exploreViewModel.getCurrentUserId(),
                            status = "Available",
                            title = title,
                            updatedAt = Timestamp.now()
                        )

                        exploreViewModel.publishProduct(
                            newProduct,
                            onSuccess = {
                                navController.navigate("explore") {
                                    popUpTo("explore") { inclusive = true }
                                }
                            },
                            onFailure = { errorMsg ->
                                FirebaseCrashlytics.getInstance().log(errorMsg)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(errorMsg)
                                }
                            }
                        )
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Please fill in all required fields.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text("Publish")
            }
        }
    }
}
