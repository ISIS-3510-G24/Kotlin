package com.example.unimarket.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.unimarket.data.PublishWithImagePayload
import com.example.unimarket.data.SyncWorker
import com.example.unimarket.data.UniMarketRepository
import com.example.unimarket.di.IoDispatcher
import com.example.unimarket.ui.models.ClassItem
import com.example.unimarket.ui.models.Major
import com.example.unimarket.utils.ConnectivityObserver
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val appContext: Context,
    private val repo: UniMarketRepository,
    private val firestore: FirebaseFirestore,
    private val crashlytics: FirebaseCrashlytics,
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

    sealed class ImageUploadState {
        object Idle : ImageUploadState()
        data class Pending(val localUri: String, val remotePath: String) : ImageUploadState()
        data class Success(val remotePath: String) : ImageUploadState()
        data class Failed(val localUri: String) : ImageUploadState()
    }

    private val _uploadState = MutableStateFlow<ImageUploadState>(ImageUploadState.Idle)
    val uploadState: StateFlow<ImageUploadState> = _uploadState.asStateFlow()

    private val handler = CoroutineExceptionHandler { _, e ->
        crashlytics.recordException(e)
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowMessage(e.message ?: "Error: ${e.message}"))
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
        viewModelScope.launch(ioDispatcher + handler) {
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
        viewModelScope.launch(ioDispatcher + handler) {
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
        localImageUri: Uri?,
    ) = viewModelScope.launch(ioDispatcher + handler) {
        if (selectedMajor == null || selectedClass == null
            || title.isBlank() || description.isBlank() || price == null
        ) {
            _uiEvent.emit(UiEvent.ShowMessage("Fill all fields"))
            return@launch
        }
        if (localImageUri == null) {
            _uiEvent.emit(UiEvent.ShowMessage("Choose an image first"))
            return@launch
        }

        val remotePath = "product_images/${System.currentTimeMillis()}.jpg"
        val payload = PublishWithImagePayload(
            majorId       = selectedMajor!!.id,
            classId       = selectedClass!!.id,
            title         = title,
            description   = description,
            price         = price!!,
            labels        = labels,
            localImageUri = localImageUri.toString(),
            remotePath    = remotePath,
            status        = "Available"
        )

        repo.enqueuePublishWithImage(payload)

        val work = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager
            .getInstance(appContext)
            .enqueueUniqueWork(
                "sync_pending_ops",
                ExistingWorkPolicy.KEEP,
                work
            )

        _uiEvent.emit(
            UiEvent.ShowMessage(
                if (isOnline.value)
                "Product queued for upload"
                else "Product queued for upload, will be uploaded when online"
            )
        )
        _uiEvent.emit(UiEvent.NavigateBack)
    }

    fun uploadImage(uri: Uri) {
        val path = "product_images/${System.currentTimeMillis()}.jpg"
        _uploadState.value = ImageUploadState.Pending(uri.toString(), path)

        viewModelScope.launch(ioDispatcher + handler) {
            repo.uploadImage( // Repo queues the upload and updates Room
                localUri = uri.toString(),
                remotePath = path
            )

            repo.observeImageCacheEntries()
                .filter { entries ->
                    entries.any { it.remotePath == path && it.state != "PENDING" }
                }
                .first()
                .find { it.remotePath == path }
                ?.let { entry ->
                    if (entry.state == "SUCCESS" && entry.downloadUrl != null) {
                        _uploadState.value = ImageUploadState.Success(entry.downloadUrl)
                    } else {
                        _uploadState.value = ImageUploadState.Failed(uri.toString())
                    }
                }
        }
    }


}
