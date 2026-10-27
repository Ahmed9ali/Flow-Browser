package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AutomationExecutionState
import com.example.model.AutomationScript
import com.example.model.AutomationStatus
import com.example.model.ElementSemanticType
import com.example.model.ScannedElement

@Composable
fun AutomationHUD(
    executionState: AutomationExecutionState,
    script: AutomationScript,
    scannedElements: List<ScannedElement>,
    onToggleRecord: () -> Unit,
    onStartPlay: () -> Unit,
    onStopPlay: () -> Unit,
    onTriggerScan: () -> Unit,
    onClearScan: () -> Unit,
    onAddScannedElement: (ScannedElement) -> Unit,
    onAutoMapAllScanned: () -> Unit,
    onOpenScriptManager: () -> Unit,
    onCloseHud: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMinimized by remember { mutableStateOf(false) }

    val statusColor = when (executionState.status) {
        AutomationStatus.RECORDING -> Color(0xFFEF4444)
        AutomationStatus.PLAYING -> Color(0xFF10B981)
        AutomationStatus.SCANNING -> Color(0xFF06B6D4)
        AutomationStatus.PAUSED -> Color(0xFFF59E0B)
        AutomationStatus.IDLE -> MaterialTheme.colorScheme.primary
    }

    val statusText = when (executionState.status) {
        AutomationStatus.RECORDING -> "REC"
        AutomationStatus.PLAYING -> "RUNNING"
        AutomationStatus.SCANNING -> "SCANNING"
        AutomationStatus.PAUSED -> "PAUSED"
        AutomationStatus.IDLE -> "READY"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, statusColor.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .testTag("automation_hud")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // HUD Top Bar: Title + Status Pill + Minimize + Close
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "Automation HUD",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = statusText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isMinimized) "Expand" else "Minimize",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { isMinimized = !isMinimized }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    IconButton(
                        onClick = onCloseHud,
                        modifier = Modifier.size(28.dp).testTag("btn_close_hud")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close HUD",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !isMinimized,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    // Execution Progress Bar & Status (when playing)
                    if (executionState.status == AutomationStatus.PLAYING) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Iteration ${executionState.currentIteration}/${executionState.totalIterations} • Step ${executionState.currentStepIndex}/${executionState.totalSteps}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981)
                                )
                                if (script.rotateProxyOnRepeat) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VpnLock,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Rotating Proxy",
                                            fontSize = 10.sp,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            val progress = if (executionState.totalSteps > 0) {
                                executionState.currentStepIndex.toFloat() / executionState.totalSteps.toFloat()
                            } else 0f

                            LinearProgressIndicator(
                                progress = { progress },
                                color = Color(0xFF10B981),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )

                            if (executionState.currentStepDescription.isNotBlank()) {
                                Text(
                                    text = "Action: ${executionState.currentStepDescription}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    // Recording Info (when recording)
                    if (executionState.status == AutomationStatus.RECORDING) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Recording actions (${script.steps.size} steps recorded). Click or type on page.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Controls Row (Record, Play/Stop, Scan, Script Manager)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. Record / Stop Record Button
                        val isRecording = executionState.status == AutomationStatus.RECORDING
                        Button(
                            onClick = onToggleRecord,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) Color(0xFFEF4444) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isRecording) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).testTag("btn_hud_record")
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                contentDescription = if (isRecording) "Stop Recording" else "Record",
                                tint = if (isRecording) Color.White else Color(0xFFEF4444),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isRecording) "Stop" else "Record", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 2. Play / Stop Playback Button
                        val isPlaying = executionState.status == AutomationStatus.PLAYING
                        Button(
                            onClick = {
                                if (isPlaying) onStopPlay() else onStartPlay()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPlaying) Color(0xFFEF4444) else Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).testTag("btn_hud_play")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Stop" else "Play",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isPlaying) "Stop" else "Run (${script.repeatCount}x)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 3. Scan DOM Button
                        OutlinedButton(
                            onClick = {
                                if (scannedElements.isNotEmpty()) onClearScan() else onTriggerScan()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).testTag("btn_hud_scan")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = "Scan",
                                tint = Color(0xFF06B6D4),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = if (scannedElements.isNotEmpty()) "Clear" else "Scan", fontSize = 11.sp)
                        }

                        // 4. Script & Dataset Manager Button
                        OutlinedButton(
                            onClick = onOpenScriptManager,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).testTag("btn_hud_script")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "Script",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = "Script", fontSize = 11.sp)
                        }
                    }

                    // Scanned Elements Section & Auto-Map Header
                    if (scannedElements.isNotEmpty()) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Captured Elements (${scannedElements.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF06B6D4)
                                    )
                                }

                                // 1-Tap Auto Map Complete Form Action
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF6366F1).copy(alpha = 0.15f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onAutoMapAllScanned() }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoFixHigh,
                                            contentDescription = null,
                                            tint = Color(0xFF6366F1),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Auto-Map Form",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF6366F1)
                                        )
                                    }
                                }
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(scannedElements) { element ->
                                    ScannedElementCard(
                                        element = element,
                                        onAdd = { onAddScannedElement(element) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScannedElementCard(
    element: ScannedElement,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (badgeText, badgeColor) = when (element.semanticType) {
        ElementSemanticType.EMAIL -> Pair("EMAIL", Color(0xFF06B6D4))
        ElementSemanticType.PASSWORD -> Pair("PASSWORD", Color(0xFF8B5CF6))
        ElementSemanticType.CONFIRM_PASSWORD -> Pair("CONFIRM PASS", Color(0xFF9333EA))
        ElementSemanticType.PHONE -> Pair("PHONE", Color(0xFFF97316))
        ElementSemanticType.COUNTRY_CODE -> Pair("COUNTRY CODE", Color(0xFF10B981))
        ElementSemanticType.GENDER -> Pair("GENDER", Color(0xFFEC4899))
        ElementSemanticType.DOB, ElementSemanticType.DOB_DAY, ElementSemanticType.DOB_MONTH, ElementSemanticType.DOB_YEAR -> Pair("DOB / CALENDAR", Color(0xFF3B82F6))
        ElementSemanticType.FILE_UPLOAD -> Pair("FILE UPLOAD", Color(0xFFF59E0B))
        ElementSemanticType.FIRST_NAME, ElementSemanticType.LAST_NAME, ElementSemanticType.FULL_NAME -> Pair("NAME", Color(0xFF14B8A6))
        ElementSemanticType.ADDRESS, ElementSemanticType.ADDRESS_LINE_2, ElementSemanticType.CITY, ElementSemanticType.STATE -> Pair("ADDRESS", Color(0xFF64748B))
        ElementSemanticType.ZIP_PIN -> Pair("ZIP / PIN", Color(0xFF6366F1))
        ElementSemanticType.CHECKBOX_TERMS -> Pair("TERMS", Color(0xFF059669))
        ElementSemanticType.SUBMIT_BUTTON -> Pair("SUBMIT", Color(0xFF10B981))
        ElementSemanticType.NEXT_BUTTON -> Pair("NEXT", Color(0xFF0284C7))
        ElementSemanticType.DROPDOWN -> Pair("DROPDOWN", Color(0xFFD946EF))
        ElementSemanticType.GENERIC_BUTTON -> Pair("BUTTON", Color(0xFF64748B))
        ElementSemanticType.GENERIC_INPUT -> Pair("INPUT", Color(0xFF3B82F6))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f)),
        modifier = modifier.width(190.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                IconButton(
                    onClick = onAdd,
                    modifier = Modifier.size(24.dp).testTag("btn_add_scanned_${element.selector.take(10)}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add step",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val displayTitle = element.placeholder.ifBlank {
                element.nameAttr.ifBlank {
                    element.text.ifBlank { element.idAttr.ifBlank { element.selector } }
                }
            }

            Text(
                text = displayTitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = element.selector,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
