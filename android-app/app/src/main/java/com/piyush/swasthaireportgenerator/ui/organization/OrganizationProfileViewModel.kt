package com.piyush.swasthaireportgenerator.ui.organization

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piyush.swasthaireportgenerator.data.api.ApiClient
import com.piyush.swasthaireportgenerator.data.local.TokenManager
import com.piyush.swasthaireportgenerator.data.model.OrganizationProfileResponse
import com.piyush.swasthaireportgenerator.data.model.UpdateOrganizationProfileRequest
import com.piyush.swasthaireportgenerator.data.repository.DashboardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class OrganizationProfileUiState(
    val userRole: String = "LAB_STAFF",
    val profile: OrganizationProfileResponse? = null,
    val logoBytes: ByteArray? = null,
    val signatureBytes: ByteArray? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingLogo: Boolean = false,
    val isUploadingSignature: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class OrganizationProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DashboardRepository(ApiClient.getAnalyticsApi(application))
    private val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow(OrganizationProfileUiState())
    val uiState: StateFlow<OrganizationProfileUiState> = _uiState.asStateFlow()

    fun resetState() {
        _uiState.value = OrganizationProfileUiState()
    }

    fun loadProfile() {
        viewModelScope.launch {
            val session = tokenManager.userSessionFlow.first()
            if (session == null || session.accessToken.isBlank()) return@launch
            val role = session.role.ifBlank { "LAB_STAFF" }
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                userRole = role
            )

            val result = repository.getOrganizationProfile()
            result.fold(
                onSuccess = { profile ->
                    val logo = if (profile.logoConfigured) repository.getLogoBytes() else null
                    val sig = if (profile.signatureConfigured) repository.getSignatureBytes() else null
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        profile = profile,
                        logoBytes = logo,
                        signatureBytes = sig
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load organization profile"
                    )
                }
            )
        }
    }

    fun saveProfile(
        addressLine1: String,
        addressLine2: String,
        city: String,
        state: String,
        postalCode: String,
        country: String,
        phone: String,
        alternatePhone: String,
        email: String,
        website: String,
        reportFooterText: String,
        reportDisclaimer: String
    ) {
        val cleanWebsite = website.trim()
        if (cleanWebsite.isNotEmpty() && !cleanWebsite.startsWith("http://") && !cleanWebsite.startsWith("https://")) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Website URL must start with http:// or https://",
                successMessage = null
            )
            return
        }

        val cleanEmail = email.trim()
        if (cleanEmail.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please enter a valid organization email address",
                successMessage = null
            )
            return
        }

        val phoneRegex = Regex("^[0-9+()\\- .]*$")
        val cleanPhone = phone.trim()
        val cleanAltPhone = alternatePhone.trim()
        if ((cleanPhone.isNotEmpty() && !phoneRegex.matches(cleanPhone)) ||
            (cleanAltPhone.isNotEmpty() && !phoneRegex.matches(cleanAltPhone))
        ) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Phone numbers may contain only digits, +, -, (), spaces, or dots",
                successMessage = null
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                errorMessage = null,
                successMessage = null
            )
            val request = UpdateOrganizationProfileRequest(
                addressLine1 = addressLine1.trim().ifBlank { null },
                addressLine2 = addressLine2.trim().ifBlank { null },
                city = city.trim().ifBlank { null },
                state = state.trim().ifBlank { null },
                postalCode = postalCode.trim().ifBlank { null },
                country = country.trim().ifBlank { null },
                phone = cleanPhone.ifBlank { null },
                alternatePhone = cleanAltPhone.ifBlank { null },
                email = cleanEmail.ifBlank { null },
                website = cleanWebsite.ifBlank { null },
                reportFooterText = reportFooterText.trim().ifBlank { null },
                reportDisclaimer = reportDisclaimer.trim().ifBlank { null }
            )
            val result = repository.updateOrganizationProfile(request)
            result.fold(
                onSuccess = { updated ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        profile = updated,
                        successMessage = "Organization settings and report letterhead saved."
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Failed to save organization profile"
                    )
                }
            )
        }
    }

    fun uploadLogoFromUri(context: Context, uri: Uri) {
        uploadImage(
            context = context,
            uri = uri,
            defaultName = "logo.png",
            setBusy = { busy -> _uiState.value = _uiState.value.copy(isUploadingLogo = busy) },
            uploader = { fileName, mime, bytes -> repository.uploadLogo(fileName, mime, bytes) },
            fetchBytes = { repository.getLogoBytes() },
            onUpdated = { profile, freshBytes ->
                _uiState.value = _uiState.value.copy(
                    profile = profile,
                    logoBytes = freshBytes,
                    successMessage = "Laboratory logo uploaded successfully."
                )
            }
        )
    }

    fun deleteLogo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUploadingLogo = true,
                errorMessage = null,
                successMessage = null
            )
            repository.deleteLogo().fold(
                onSuccess = { updated ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingLogo = false,
                        profile = updated,
                        logoBytes = null,
                        successMessage = "Laboratory logo removed."
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingLogo = false,
                        errorMessage = error.message ?: "Failed to remove logo"
                    )
                }
            )
        }
    }

    fun uploadSignatureFromUri(context: Context, uri: Uri) {
        uploadImage(
            context = context,
            uri = uri,
            defaultName = "signature.png",
            setBusy = { busy -> _uiState.value = _uiState.value.copy(isUploadingSignature = busy) },
            uploader = { fileName, mime, bytes -> repository.uploadSignature(fileName, mime, bytes) },
            fetchBytes = { repository.getSignatureBytes() },
            onUpdated = { profile, freshBytes ->
                _uiState.value = _uiState.value.copy(
                    profile = profile,
                    signatureBytes = freshBytes,
                    successMessage = "Authorized signature uploaded successfully."
                )
            }
        )
    }

    fun uploadSignatureBytes(bytes: ByteArray, fileName: String = "digital-signature.png", mimeType: String = "image/png") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUploadingSignature = true,
                errorMessage = null,
                successMessage = null
            )
            val uploadResult = repository.uploadSignature(fileName, mimeType, bytes)
            uploadResult.fold(
                onSuccess = { updated ->
                    val freshBytes = repository.getSignatureBytes() ?: bytes
                    _uiState.value = _uiState.value.copy(
                        isUploadingSignature = false,
                        profile = updated,
                        signatureBytes = freshBytes,
                        successMessage = "Digital signature saved successfully."
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingSignature = false,
                        errorMessage = error.message ?: "Failed to save digital signature"
                    )
                }
            )
        }
    }

    fun deleteSignature() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUploadingSignature = true,
                errorMessage = null,
                successMessage = null
            )
            repository.deleteSignature().fold(
                onSuccess = { updated ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingSignature = false,
                        profile = updated,
                        signatureBytes = null,
                        successMessage = "Authorized signature removed."
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingSignature = false,
                        errorMessage = error.message ?: "Failed to remove signature"
                    )
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    private fun uploadImage(
        context: Context,
        uri: Uri,
        defaultName: String,
        setBusy: (Boolean) -> Unit,
        uploader: suspend (String, String, ByteArray) -> Result<OrganizationProfileResponse>,
        fetchBytes: suspend () -> ByteArray?,
        onUpdated: (OrganizationProfileResponse, ByteArray?) -> Unit
    ) {
        viewModelScope.launch {
            setBusy(true)
            _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)

            val readPayload = withContext(Dispatchers.IO) {
                try {
                    val mimeType = context.contentResolver.getType(uri) ?: "image/png"
                    val fileName = resolveFileName(context, uri) ?: defaultName
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    Triple(fileName, mimeType, bytes)
                } catch (_: Exception) {
                    null
                }
            }

            if (readPayload == null || readPayload.third == null) {
                setBusy(false)
                _uiState.value = _uiState.value.copy(errorMessage = "Could not read selected image file.")
                return@launch
            }

            val (fileName, mimeType, bytes) = readPayload
            if (!mimeType.startsWith("image/")) {
                setBusy(false)
                _uiState.value = _uiState.value.copy(errorMessage = "Please select a valid image (PNG, JPEG, WebP).")
                return@launch
            }
            if (bytes == null || bytes.size > 2 * 1024 * 1024) {
                setBusy(false)
                _uiState.value = _uiState.value.copy(
                    errorMessage = if (bytes == null) "Could not read selected image." else "Image size must be less than 2 MB."
                )
                return@launch
            }

            val uploadResult = uploader(fileName, mimeType, bytes)
            uploadResult.fold(
                onSuccess = { updated ->
                    val freshBytes = fetchBytes() ?: bytes
                    setBusy(false)
                    onUpdated(updated, freshBytes)
                },
                onFailure = { error ->
                    setBusy(false)
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Failed to upload image"
                    )
                }
            )
        }
    }

    private fun resolveFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        } catch (_: Exception) {
            null
        }
    }
}
