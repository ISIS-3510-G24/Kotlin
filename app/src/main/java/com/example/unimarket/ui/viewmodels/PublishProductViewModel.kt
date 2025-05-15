package com.example.unimarket.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarket.data.PublishProductPayload
import com.example.unimarket.data.UniMarketRepository
import com.example.unimarket.di.IoDispatcher
import com.example.unimarket.ui.models.ClassItem
import com.example.unimarket.ui.models.Major
import com.example.unimarket.utils.ConnectivityObserver
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PublishProductViewModel @Inject constructor(
    private val repo: UniMarketRepository,
    private val firestore: FirebaseFirestore,
    private val crashlytics: FirebaseCrashlytics,
    private val analytics: FirebaseAnalytics,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    // Connectivity
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // Dropdown data
    private val _majors = MutableStateFlow<List<Major>>(emptyList())
    val majors: StateFlow<List<Major>> = _majors.asStateFlow()

    private val _classes = MutableStateFlow<List<ClassItem>>(emptyList())
    val classes: StateFlow<List<ClassItem>> = _classes.asStateFlow()

    // UI events (snackbar, navigation)
    sealed class UiEvent {
        data class ShowMessage(val text: String) : UiEvent()
        object NavigateBack : UiEvent()
    }

    private val _uiEvent = MutableSharedFlow<UiEvent>(replay = 0)
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    private val _uploadState = MutableStateFlow<ImageUploadState>(ImageUploadState.Idle)
    val uploadState: StateFlow<ImageUploadState> = _uploadState.asStateFlow()

    private val handler = CoroutineExceptionHandler { _, e ->
        crashlytics.recordException(e)
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowMessage(e.message ?: "Error"))
        }
    }

    init {
        // Observe connectivity
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { _isOnline.value = it }
        }

        // Load majors
        loadMajors()
    }

    fun loadMajors() {
        viewModelScope.launch(ioDispatcher) {
            try {
                val docs = firestore.collection("majors").get().await()
                val list = docs.documents.mapNotNull {
                    it.toObject(Major::class.java)?.copy(id = it.id)
                }
                // Emit the majors list
                withContext(Dispatchers.Default) {
                    list.sortedBy { it.name }
                }
                _majors.value = list
            } catch (e: Exception) {
                crashlytics.recordException(e)
                _uiEvent.emit(UiEvent.ShowMessage("Error loading majors"))
            }
        }
    }

    fun onMajorSelected(major: Major) {
        // Clean previous classes
        _classes.value = emptyList()
        viewModelScope.launch(ioDispatcher) {
            try {
                val docs = firestore
                    .collection("majors")
                    .document(major.id)
                    .collection("clases")
                    .get()
                    .await()
                val list = docs.documents.mapNotNull {
                    it.toObject(ClassItem::class.java)?.copy(id = it.id)
                }
                _classes.value = list
            } catch (e: Exception) {
                crashlytics.recordException(e)
                _uiEvent.emit(UiEvent.ShowMessage("Error loading classes"))
            }
        }
    }

    fun publish(
        selectedMajor: Major?,
        selectedClass: ClassItem?,
        title: String,
        description: String,
        price: Double?,
        labels: List<String>,
        imageUrl: String,
    ) {
        if (selectedMajor == null || selectedClass == null || title.isBlank() || description.isBlank() || price == null) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowMessage("Fill out all fields")) }
            return
        }
        val payload = PublishProductPayload(
            majorId = selectedMajor.id,
            classId = selectedClass.id,
            title = title,
            description = description,
            price = price,
            labels = labels,
            imageUrls = listOf(imageUrl),
            status = "Available"
        )

        viewModelScope.launch(ioDispatcher + handler) {
            // Enqueue the publish product operation
            repo.enqueuePublishProduct(payload)


            withContext(Dispatchers.Main) {
                if (_isOnline.value) {
                    _uiEvent.emit(UiEvent.ShowMessage("Product published successfully"))
                } else {
                    _uiEvent.emit(UiEvent.ShowMessage("No connection: Product will be published when online"))
                }
                _uiEvent.emit(UiEvent.NavigateBack)
            }
        }
    }

    fun uploadImage(uri: Uri) {
        val path = "product_images/${System.currentTimeMillis()}.jpg"
        _uploadState.value = ImageUploadState.Pending(uri.toString(), path)
        viewModelScope.launch(ioDispatcher + CoroutineExceptionHandler { _, e ->
            crashlytics.recordException(e)
            _uploadState.value = ImageUploadState.Failed(uri.toString())
        }) {
            repo.uploadImage( // Repo queues the upload and updates Room
                localUri = uri.toString(),
                remotePath = path
            )

            repo.observeImageCacheEntries()
                .filter { entries ->
                    entries.any { it.remotePath == path && it.state != "PENDING" }
                }
                .first()
                .find() { it.remotePath == path }
                ?.let { entry ->
                    if (entry.state == "SUCCESS" && entry.downloadUrl != null) {
                        _uploadState.value = ImageUploadState.Success(entry.downloadUrl)
                    } else {
                        _uploadState.value = ImageUploadState.Failed(uri.toString())
                    }
                }
        }
    }

    sealed class ImageUploadState {
        object Idle : ImageUploadState()
        data class Pending(val localUri: String, val remotePath: String) : ImageUploadState()
        data class Success(val remotePath: String) : ImageUploadState()
        data class Failed(val localUri: String) : ImageUploadState()
    }
}
