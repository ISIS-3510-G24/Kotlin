package com.example.unimarket.ui.explore

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unimarket.ui.data.FirebaseFirestoreSingleton
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishProductScreen(
    navController: NavController,
    exploreViewModel: ExploreViewModel = viewModel()
) {
    var classId by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var labelsText by remember { mutableStateOf("") } // Labels separated by commas

    // Load majors from Firestore
    var majors by remember { mutableStateOf<List<Major>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var selectedMajor by remember { mutableStateOf<Major?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val isLoading by exploreViewModel.isLoading.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Load majors from Firestore
    LaunchedEffect(Unit) {
        FirebaseFirestoreSingleton.getCollection("majors")
            .get()
            .addOnSuccessListener { result ->
                val majorList = result.documents.mapNotNull { doc ->
                    doc.toObject(Major::class.java)?.copy(majorID = doc.id)
                }
                majors = majorList
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
            OutlinedTextField(
                value = classId,
                onValueChange = { classId = it },
                label = { Text("Class ID") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = { Text("Image URL") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = labelsText,
                onValueChange = { labelsText = it },
                label = { Text("Labels (comma separated)") },
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedMajor?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Major") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    majors.forEach { major ->
                        DropdownMenuItem(
                            text = { Text(major.name) },
                            onClick = {
                                selectedMajor = major
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    // Validate input fields and publish the product
                    val price = priceText.toDoubleOrNull()
                    if (classId.isNotBlank() && title.isNotBlank() && description.isNotBlank() && price != null && imageUrl.isNotBlank() && selectedMajor != null) {
                        val labels =
                            labelsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val newProduct = Product(
                            classId = classId,
                            createdAt = Timestamp.now(),
                            description = description,
                            imageUrls = listOf(imageUrl),
                            labels = labels,
                            majorID = selectedMajor!!.majorID,
                            price = price,
                            sellerID = exploreViewModel.getCurrentUserId(),
                            status = "Available",
                            title = title,
                            updatedAt = Timestamp.now()
                        )
                        exploreViewModel.publishProduct(
                            newProduct,
                            onSuccess = {
                                // Go back to the Explore screen
                                navController.navigate("explore") {
                                    popUpTo("explore") { inclusive = true }
                                }
                            },
                            onFailure = { errorMsg ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(errorMsg)
                                }
                            }
                        )
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Please fill in all fields.")
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
