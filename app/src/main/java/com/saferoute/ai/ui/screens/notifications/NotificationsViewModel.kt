package com.saferoute.ai.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saferoute.ai.data.repository.NotificationRepository
import com.saferoute.ai.domain.model.AppNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<AppNotification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            notificationRepository.getNotifications()
                .onSuccess {
                    _uiState.value = NotificationsUiState(notifications = it, isLoading = false)
                    delay(1000)
                    notificationRepository.markAllRead()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(error = it.message, isLoading = false)
                }
        }
    }
}
