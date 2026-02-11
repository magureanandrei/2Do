package com.example.learning_test.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SyncStatusIcon(isSyncing: Boolean, modifier: Modifier = Modifier) {
    if (isSyncing) {
        CircularProgressIndicator(
            modifier = modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
    } else {
        Icon(
            imageVector = Icons.Default.CloudDone,
            contentDescription = "Synced",
            tint = Color.Gray.copy(alpha = 0.5f),
            modifier = modifier.size(24.dp)
        )
    }
}
