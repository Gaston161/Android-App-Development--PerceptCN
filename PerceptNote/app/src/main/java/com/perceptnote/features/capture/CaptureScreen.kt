// 📄 app/src/main/java/com/perceptnote/features/capture/CaptureScreen.kt
package com.perceptnote.features.capture

import android.Manifest
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.perceptnote.sensors.camera.CameraModule
import com.perceptnote.ui.components.AudioWaveform
import com.perceptnote.ui.components.SensorStatusBar
import com.perceptnote.ui.theme.RecordingRed
import com.perceptnote.ui.theme.RecordingRedAlpha
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CaptureScreen(
    onNavigateBack: () -> Unit,
    onSessionSaved: (Long) -> Unit,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // === Gestion des permissions runtime ===
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    if (!permissionsState.allPermissionsGranted) {
        PermissionsRationale(
            onRequest = { permissionsState.launchMultiplePermissionRequest() },
            onBack = onNavigateBack
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (uiState.isRecording) "● EN COURS" else "Capture",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (uiState.isRecording) RecordingRed else MaterialTheme.colorScheme.onSurface
                        )
                        if (uiState.sessionTitle.isNotEmpty()) {
                            Text(
                                text = uiState.sessionTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        val sessionId = viewModel.stopSession()
                        if (sessionId != null) onSessionSaved(sessionId) else onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    // Compteur de notes sauvegardées
                    if (uiState.savedNoteCount > 0) {
                        Badge { Text(uiState.savedNoteCount.toString()) }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // === Barre capteurs ===
            SensorStatusBar(
                isMicActive = uiState.isRecording,
                isCameraActive = uiState.isCameraActive,
                isGpsActive = true,
                isAccelActive = uiState.isPhoneFlat
            )

            // === Prévisualisation caméra (optionnelle) ===
            AnimatedVisibility(
                visible = uiState.isCameraActive,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                ) {
                    CameraPreview(
                        cameraModule = remember { CameraModule(context) },
                        ocrAnalyzer = viewModel.ocrAnalyzer,
                        lifecycleOwner = lifecycleOwner
                    )
                    // Overlay OCR détecté
                    if (uiState.ocrText.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(8.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = uiState.ocrText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.padding(8.dp),
                                maxLines = 3
                            )
                        }
                    }
                }
            }

            // === Zone principale ===
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Indice de pose
                if (!uiState.isRecording) {
                    GestureHintCard()
                }

                // Visualisation audio
                AnimatedVisibility(visible = uiState.isRecording) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = RecordingRedAlpha
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(RecordingRed)
                                )
                                Text(
                                    text = "Enregistrement en cours",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = RecordingRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            AudioWaveform(isActive = uiState.isRecording)
                            // Transcription partielle
                            if (uiState.partialTranscription.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "\"${uiState.partialTranscription}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }

                // Boutons d'action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bouton caméra OCR
                    OutlinedButton(
                        onClick = viewModel::toggleCamera,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (uiState.isCameraActive) Icons.Default.CameraAlt else Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (uiState.isCameraActive) "Masquer caméra" else "Scan OCR")
                    }

                    // Bouton démarrage manuel
                    if (!uiState.isRecording) {
                        Button(
                            onClick = viewModel::startSessionManually,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = RecordingRed)
                        ) {
                            Icon(Icons.Default.Mic, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Démarrer")
                        }
                    } else {
                        Button(
                            onClick = {
                                val sessionId = viewModel.stopSession()
                                if (sessionId != null) onSessionSaved(sessionId)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Terminer")
                        }
                    }
                }

                // Notes sauvegardées
                if (uiState.savedNoteCount > 0) {
                    Text(
                        text = "✓ ${uiState.savedNoteCount} note(s) enregistrée(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    cameraModule: CameraModule,
    ocrAnalyzer: com.perceptnote.sensors.camera.OcrAnalyzer,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).also { previewView ->
                cameraModule.startCamera(lifecycleOwner, previewView, ocrAnalyzer)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun GestureHintCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.PhoneAndroid,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = "Pose ton téléphone",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "L'enregistrement démarre automatiquement quand tu poses ton téléphone à plat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PermissionsRationale(onRequest: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Permissions requises",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "PerceptNote nécessite l'accès à la caméra, au microphone et à la localisation pour fonctionner. Ces données restent sur ton appareil.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
            Text("Autoriser les permissions")
        }
        TextButton(onClick = onBack) { Text("Plus tard") }
    }
}
