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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.unimarket.data.FirebaseFirestoreSingleton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishFindScreen(
    navController: NavController
) {
    var majors by remember { mutableStateOf<List<String>>(emptyList()) }
    var expandedMajor by remember { mutableStateOf(false) }
    var selectedMajor by remember { mutableStateOf<String?>(null) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var labelsText by remember { mutableStateOf("") }
    val imageUri = remember { mutableStateOf<Uri?>(null) }
    var isPublishing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val storage   = FirebaseStorage.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val auth      = FirebaseAuth.getInstance()

    // Majors loading
    LaunchedEffect(Unit) {
        FirebaseFirestoreSingleton.getCollection("finds")
            .get()
            .addOnSuccessListener { snap ->
                majors = snap.documents
                    .mapNotNull { it.getString("major")?.uppercase() }
                    .distinct()
            }
    }

    // image selector
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> imageUri.value = uri }

    Scaffold(
        topBar       = { TopAppBar(title = { Text("Publish Find") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Dropdown Major
            ExposedDropdownMenuBox(
                expanded = expandedMajor,
                onExpandedChange = { expandedMajor = !expandedMajor }
            ) {
                OutlinedTextField(
                    value        = selectedMajor ?: "",
                    onValueChange = {},
                    readOnly     = true,
                    label        = { Text("Major") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedMajor) },
                    modifier     = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded        = expandedMajor,
                    onDismissRequest = { expandedMajor = false }
                ) {
                    majors.forEach { major ->
                        DropdownMenuItem(
                            text    = { Text(major) },
                            onClick = {
                                selectedMajor = major
                                expandedMajor = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // Title
            OutlinedTextField(
                value        = title,
                onValueChange = { title = it },
                label        = { Text("Title") },
                modifier     = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // Description
            OutlinedTextField(
                value        = description,
                onValueChange = { description = it },
                label        = { Text("Description") },
                modifier     = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // Labels
            OutlinedTextField(
                value        = labelsText,
                onValueChange = { labelsText = it },
                label        = { Text("Labels (comma separated)") },
                modifier     = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // Image picker
            Button(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Image")
            }
            imageUri.value?.let { uri ->
                Spacer(Modifier.height(8.dp))
                Image(
                    painter           = rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale       = ContentScale.Crop
                )
            }
            Spacer(Modifier.height(16.dp))

            // Publish button
            Button(
                onClick = {
                    // Validación
                    if (selectedMajor != null &&
                        title.isNotBlank() &&
                        description.isNotBlank() &&
                        imageUri.value != null
                    ) {
                        isPublishing = true
                        coroutineScope.launch { snackbarHostState.showSnackbar("Publishing...") }

                        // Image upload
                        val imgRef = storage.reference.child("finds_images/${UUID.randomUUID()}")
                        imgRef.putFile(imageUri.value!!)
                            .addOnSuccessListener {
                                imgRef.downloadUrl.addOnSuccessListener { url ->
                                    // User display
                                    val uid = auth.currentUser?.uid ?: ""
                                    firestore.collection("User")
                                        .document(uid)
                                        .get()
                                        .addOnSuccessListener { userDoc ->
                                            val userName = userDoc.getString("displayName") ?: ""

                                            // Map
                                            val newFind = mapOf(
                                                "title"       to title,
                                                "description" to description,
                                                "image"       to url.toString(),
                                                "labels"      to labelsText
                                                    .split(',')
                                                    .map(String::trim)
                                                    .filter(String::isNotEmpty),
                                                "major"       to selectedMajor,
                                                "userId"      to uid,
                                                "userName"    to userName,
                                                "timestamp"   to Timestamp.now(),
                                                "status"      to "active",
                                                "offerCount"  to 0,
                                                "upvoteCount" to 0
                                            )

                                            // Document
                                            firestore.collection("finds")
                                                .add(newFind)
                                                .addOnSuccessListener {
                                                    isPublishing = false
                                                    navController.popBackStack()
                                                }
                                                .addOnFailureListener { e ->
                                                    isPublishing = false
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Error: ${e.message}")
                                                    }
                                                }
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                isPublishing = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Image upload failed: ${e.message}")
                                }
                            }
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Complete all fields and select an image.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled  = !isPublishing
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp)
                    )
                }
                Text("Publish Find")
            }
        }
    }
}
