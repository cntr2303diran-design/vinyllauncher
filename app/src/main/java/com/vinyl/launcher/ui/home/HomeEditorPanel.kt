package com.vinyl.launcher.ui.home

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinyl.launcher.data.AppInfo
import com.vinyl.launcher.data.ThemePreset
import com.vinyl.launcher.ui.settings.GestureControls
import com.vinyl.launcher.ui.settings.HomeStyleControls
import com.vinyl.launcher.ui.settings.SectionTitle
import com.vinyl.launcher.ui.settings.SettingsActivity

/**
 * Panel editor home screen. Semua perubahan gaya langsung tampil di layar
 * (pratinjau) karena HomeScreen menggambar dari draft.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeEditorPanel(vm: HomeViewModel, landscape: Boolean, apps: List<AppInfo>) {
    val draft by vm.draft.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var naming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ThemePreset?>(null) }
    val home = draft ?: return

    val shape = if (landscape) RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
    else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    Column(
        (if (landscape) Modifier.width(EDITOR_WIDTH).fillMaxHeight() else Modifier.fillMaxWidth().height(EDITOR_HEIGHT_PORTRAIT))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.97f), shape)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    if (landscape) WindowInsetsSides.Vertical + WindowInsetsSides.End
                    else WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
    ) {
        // Header tetap
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Edit home screen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            TextButton(onClick = vm::cancelEdit) { Text("Batal") }
            Button(onClick = vm::saveEdit) { Text("Simpan") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            SectionTitle("Preset tema")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                settings.presets.forEach { p ->
                    Text(
                        p.name,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                            .combinedClickable(onClick = { vm.applyPreset(p) }, onLongClick = { deleting = p })
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
                OutlinedButton(onClick = { naming = true }) { Text("+ Simpan gaya ini") }
            }
            if (settings.presets.isNotEmpty()) Text(
                "Ketuk preset untuk menerapkan, tekan lama untuk menghapus.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )

            HomeStyleControls(home) { new -> vm.updateDraft { new } }

            SectionTitle("Gestur")
            GestureControls(settings.gestures, apps) { g -> vm.updateSettings { it.copy(gestures = g) } }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Pengaturan lainnya…") }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (naming) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { naming = false },
            title = { Text("Simpan preset") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("Nama preset") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { vm.savePreset(name); naming = false }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { naming = false }) { Text("Batal") } },
        )
    }
    deleting?.let { p ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Hapus preset \"${p.name}\"?") },
            confirmButton = { TextButton(onClick = { vm.deletePreset(p.id); deleting = null }) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Batal") } },
        )
    }
}
