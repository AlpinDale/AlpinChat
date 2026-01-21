package com.alpin.chat.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.alpin.chat.MainActivity
import com.alpin.chat.R
import com.alpin.chat.data.local.datastore.AppSettings
import com.alpin.chat.data.repository.ChatRepository
import com.alpin.chat.domain.model.Message
import com.alpin.chat.domain.model.ModelConfig
import com.alpin.chat.tokenizer.ContextManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GenerationService : Service() {

    @Inject
    lateinit var chatRepository: ChatRepository

    @Inject
    lateinit var contextManager: ContextManager

    private val binder = GenerationBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var generationJob: Job? = null

    private val _streamingContent = MutableSharedFlow<String>(extraBufferCapacity = 100)
    val streamingContent: SharedFlow<String> = _streamingContent.asSharedFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val generationError: SharedFlow<String> = _generationError.asSharedFlow()

    private val _generationComplete = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val generationComplete: SharedFlow<Unit> = _generationComplete.asSharedFlow()

    inner class GenerationBinder : Binder() {
        fun getService(): GenerationService = this@GenerationService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Generation Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when AI is generating a response"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.generating_response))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun startGeneration(
        modelConfig: ModelConfig,
        messages: List<Message>,
        settings: AppSettings,
        thinkingEnabled: Boolean = false
    ) {
        stopGeneration()

        _isGenerating.value = true
        startForeground(NOTIFICATION_ID, createNotification())

        generationJob = serviceScope.launch {
            // Trim messages to fit within context length
            val trimmedMessages = contextManager.trimToContext(
                messages = messages,
                settings = settings,
                contextLength = modelConfig.contextLength,
                reserveForResponse = 1024
            )

            chatRepository.streamChatCompletion(
                modelConfig = modelConfig,
                messages = trimmedMessages,
                settings = settings,
                thinkingEnabled = thinkingEnabled
            )
                .catch { e ->
                    _generationError.emit(e.message ?: "An error occurred")
                    _isGenerating.value = false
                    stopForegroundService()
                }
                .onCompletion {
                    _generationComplete.emit(Unit)
                    _isGenerating.value = false
                    stopForegroundService()
                }
                .collect { chunk ->
                    _streamingContent.emit(chunk)
                }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        generationJob = null
        _isGenerating.value = false
        stopForegroundService()
    }

    private fun stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "generation_channel"
        const val NOTIFICATION_ID = 1

        fun startService(context: Context) {
            val intent = Intent(context, GenerationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
