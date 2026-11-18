package com.promptmaster.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private val Red      = Color(0xFFD32F2F)
private val RedLight = Color(0xFFFF1744)
private val Black    = Color(0xFF000000)
private val White    = Color(0xFFFFFFFF)

// ---------- DATABASE (OFFLINE JSON STORAGE) ----------
class PromptDatabase(context: Context) {
    private val promptsFile = File(context.filesDir, "prompts.json")
    private val statsFile = File(context.filesDir, "stats.json")
    
    data class Prompt(
        val id: Long = System.currentTimeMillis(),
        val text: String,
        val category: String,
        val timestamp: Long = System.currentTimeMillis(),
        val isFavorite: Boolean = false,
        val usageCount: Int = 0
    )
    
    data class Stats(
        val totalGenerated: Int = 0,
        val totalCopied: Int = 0,
        val totalShared: Int = 0,
        val favoriteCount: Int = 0
    )
    
    fun loadPrompts(): List<Prompt> {
        return try {
            if (!promptsFile.exists()) return emptyList()
            val json = promptsFile.readText()
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val obj = arr.getJSONObject(it)
                Prompt(
                    id = obj.getLong("id"),
                    text = obj.getString("text"),
                    category = obj.getString("category"),
                    timestamp = obj.getLong("timestamp"),
                    isFavorite = obj.optBoolean("favorite", false),
                    usageCount = obj.optInt("usage", 0)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun savePrompt(prompt: Prompt) {
        val current = loadPrompts().toMutableList()
        val existing = current.find { it.text == prompt.text && it.category == prompt.category }
        if (existing != null) {
            val index = current.indexOf(existing)
            current[index] = existing.copy(usageCount = existing.usageCount + 1)
        } else {
            current.add(0, prompt)
            if (current.size > 200) current.removeAt(current.lastIndex)
        }
        saveToFile(promptsFile, current)
    }
    
    fun toggleFavorite(promptId: Long) {
        val current = loadPrompts().toMutableList()
        val index = current.indexOfFirst { it.id == promptId }
        if (index >= 0) {
            current[index] = current[index].copy(isFavorite = !current[index].isFavorite)
            saveToFile(promptsFile, current)
        }
    }
    
    fun deletePrompt(promptId: Long) {
        val current = loadPrompts().toMutableList()
        current.removeAll { it.id == promptId }
        saveToFile(promptsFile, current)
    }
    
    fun clearAll() {
        promptsFile.delete()
        statsFile.delete()
    }
    
    fun loadStats(): Stats {
        return try {
            if (!statsFile.exists()) return Stats()
            val json = statsFile.readText()
            val obj = JSONObject(json)
            Stats(
                totalGenerated = obj.optInt("generated", 0),
                totalCopied = obj.optInt("copied", 0),
                totalShared = obj.optInt("shared", 0),
                favoriteCount = obj.optInt("favorites", 0)
            )
        } catch (e: Exception) {
            Stats()
        }
    }
    
    fun updateStats(action: String) {
        val stats = loadStats()
        val updated = when (action) {
            "generated" -> stats.copy(totalGenerated = stats.totalGenerated + 1)
            "copied" -> stats.copy(totalCopied = stats.totalCopied + 1)
            "shared" -> stats.copy(totalShared = stats.totalShared + 1)
            else -> stats
        }
        val obj = JSONObject().apply {
            put("generated", updated.totalGenerated)
            put("copied", updated.totalCopied)
            put("shared", updated.totalShared)
            put("favorites", loadPrompts().count { it.isFavorite })
        }
        statsFile.writeText(obj.toString())
    }
    
    fun exportData(): String {
        val prompts = loadPrompts()
        val stats = loadStats()
        val obj = JSONObject().apply {
            put("prompts", JSONArray().apply {
                prompts.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("text", p.text)
                        put("category", p.category)
                        put("timestamp", p.timestamp)
                        put("favorite", p.isFavorite)
                        put("usage", p.usageCount)
                    })
                }
            })
            put("stats", JSONObject().apply {
                put("generated", stats.totalGenerated)
                put("copied", stats.totalCopied)
                put("shared", stats.totalShared)
            })
            put("exportDate", System.currentTimeMillis())
            put("version", "3.0")
        }
        return obj.toString(2)
    }
    
    fun importData(json: String): Boolean {
        return try {
            val obj = JSONObject(json)
            val promptsArr = obj.getJSONArray("prompts")
            val prompts = (0 until promptsArr.length()).map {
                val p = promptsArr.getJSONObject(it)
                Prompt(
                    id = p.getLong("id"),
                    text = p.getString("text"),
                    category = p.getString("category"),
                    timestamp = p.getLong("timestamp"),
                    isFavorite = p.optBoolean("favorite", false),
                    usageCount = p.optInt("usage", 0)
                )
            }
            saveToFile(promptsFile, prompts)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun saveToFile(file: File, prompts: List<Prompt>) {
        val arr = JSONArray()
        prompts.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("text", it.text)
                put("category", it.category)
                put("timestamp", it.timestamp)
                put("favorite", it.isFavorite)
                put("usage", it.usageCount)
            })
        }
        file.writeText(arr.toString())
    }
}

// ---------- UI COMPONENTS ----------
@Composable
fun SquareBackground(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Black)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 80f
            val stroke = Stroke(width = 1.2f)
            var y = 0f
            while (y < size.height + step) {
                var x = 0f
                while (x < size.width + step) {
                    drawRect(color = Red.copy(alpha = 0.25f), topLeft = Offset(x, y), size = Size(step, step), style = stroke)
                    x += step
                }
                y += step
            }
        }
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(colors = listOf(Red.copy(alpha = 0.25f), Color.Transparent), radius = 900f)
        ))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(brush = Brush.linearGradient(listOf(RedLight, Red)), shape = RoundedCornerShape(18.dp))
            .clickable { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick() 
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallButton(text: String, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .height(48.dp)
            .background(brush = Brush.linearGradient(listOf(RedLight, Red)), shape = RoundedCornerShape(12.dp))
            .clickable { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick() 
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------- SCREENS ----------
@Composable
fun HomeScreen(nav: androidx.navigation.NavController, db: PromptDatabase) {
    val stats by produceState(initialValue = PromptDatabase.Stats(), db) {
        value = withContext(Dispatchers.IO) { db.loadStats() }
    }
    
    SquareBackground {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Spacer(Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("PROMPT", color = White, fontSize = 42.sp, fontWeight = FontWeight.Black)
                    Text("MASTER", color = RedLight, fontSize = 42.sp, fontWeight = FontWeight.Black)
                }
                IconButton(onClick = { nav.navigate("stats") }) {
                    Icon(Icons.Default.BarChart, "Stats", tint = White, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Advanced • Free • Offline", color = White.copy(alpha = 0.75f), fontSize = 14.sp)
            Spacer(Modifier.height(24.dp))
            
            // Stats Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Generated", stats.totalGenerated.toString(), Modifier.weight(1f))
                StatCard("Copied", stats.totalCopied.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(24.dp))
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Text("CATEGORIES", color = White.copy(0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                item { NeonButton("👨  MAN") { nav.navigate("man") } }
                item { NeonButton("👩  WOMEN") { nav.navigate("women") } }
                item { NeonButton("💑  COUPLES") { nav.navigate("couples") } }
                item { NeonButton("👶  KIDS") { nav.navigate("kids") } }
                item { NeonButton("🐕  PETS") { nav.navigate("pets") } }
                item { NeonButton("🦸  FANTASY") { nav.navigate("fantasy") } }
                item { NeonButton("🚀  SCI-FI") { nav.navigate("scifi") } }
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Text("TOOLS", color = White.copy(0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                item { NeonButton("📜  HISTORY") { nav.navigate("history") } }
                item { NeonButton("⭐  FAVORITES") { nav.navigate("favorites") } }
                item { NeonButton("⚙️  SETTINGS") { nav.navigate("settings") } }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Red.copy(0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(label, color = White.copy(0.7f), fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    title: String, 
    promptText: String, 
    titleText: String,
    db: PromptDatabase,
    nav: androidx.navigation.NavController
) {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showCustomDialog by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewText by remember { mutableStateOf("") }
    var customWord by remember { mutableStateOf("") }
    var showVariationsDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    fun copy(label: String, text: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text.trim()))
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        Toast.makeText(ctx, "$label copied ✅", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            db.savePrompt(PromptDatabase.Prompt(text = text.trim(), category = title))
            db.updateStats("copied")
        }
    }
    
    fun share(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        ctx.startActivity(Intent.createChooser(intent, "Share Prompt"))
        CoroutineScope(Dispatchers.IO).launch {
            db.updateStats("shared")
        }
    }
    
    fun preview(text: String) {
        previewText = text
        showPreviewDialog = true
        CoroutineScope(Dispatchers.IO).launch {
            db.updateStats("generated")
        }
    }

    if (showPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = { Text("Preview", color = White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        previewText,
                        color = White,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${previewText.length} / 400 characters",
                        color = if (previewText.length <= 400) RedLight else Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { 
                        copy("Prompt", previewText)
                        showPreviewDialog = false
                    }) {
                        Text("COPY", color = RedLight, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { 
                        share(previewText)
                        showPreviewDialog = false
                    }) {
                        Text("SHARE", color = RedLight, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreviewDialog = false }) {
                    Text("Close", color = White.copy(0.7f))
                }
            },
            containerColor = Black,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showCustomDialog) {
        val suggestions = listOf("CYBORG", "WARRIOR", "ANGEL", "DEMON", "ROBOT", "NINJA", "PIRATE", "ASTRONAUT")
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Custom Prompt", color = White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = customWord,
                        onValueChange = { customWord = it.uppercase() },
                        label = { Text("Enter word", color = White.copy(0.7f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            cursorColor = RedLight,
                            focusedBorderColor = Red,
                            unfocusedBorderColor = Red.copy(0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Suggestions:", color = White.copy(0.7f), fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.take(4).forEach { suggestion ->
                            FilterChip(
                                selected = customWord == suggestion,
                                onClick = { customWord = suggestion },
                                label = { Text(suggestion, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Red,
                                    containerColor = Red.copy(0.2f),
                                    selectedLabelColor = White,
                                    labelColor = White.copy(0.7f)
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (customWord.isNotBlank()) {
                        val finalPrompt = CUSTOM_PROMPT_TEMPLATE.replace("[word]", customWord.trim())
                        preview(finalPrompt)
                        showCustomDialog = false
                        customWord = ""
                    } else {
                        Toast.makeText(ctx, "Please enter a word", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Generate", color = RedLight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showCustomDialog = false
                    customWord = ""
                }) {
                    Text("Cancel", color = White.copy(0.7f))
                }
            },
            containerColor = Black,
            shape = RoundedCornerShape(16.dp)
        )
    }
    
    if (showVariationsDialog) {
        val variations = generateVariations(promptText)
        AlertDialog(
            onDismissRequest = { showVariationsDialog = false },
            title = { Text("5 Variations", color = White, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(variations) { variation ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { 
                                preview(variation)
                                showVariationsDialog = false
                            },
                            colors = CardDefaults.cardColors(containerColor = Red.copy(0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = variation,
                                    color = White,
                                    fontSize = 12.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${variation.length} chars",
                                    color = RedLight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVariationsDialog = false }) {
                    Text("Close", color = RedLight)
                }
            },
            containerColor = Black,
            shape = RoundedCornerShape(16.dp)
        )
    }

    SquareBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = White)
                }
                Text(title, color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(48.dp))
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { NeonButton("$title PROMPT") { preview(promptText) } }
                item { NeonButton("TITLE PROMPT") { preview(titleText) } }
                item { NeonButton("⚡ CUSTOM PROMPT") { showCustomDialog = true } }
                item { NeonButton("🎲 5 VARIATIONS") { showVariationsDialog = true } }
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        SmallButton("📤 SHARE", onClick = { share(promptText) })
                        SmallButton("📋 COPY", onClick = { copy("Prompt", promptText) })
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(db: PromptDatabase, nav: androidx.navigation.NavController) {
    var prompts by remember { mutableStateOf(db.loadPrompts()) }
    var searchQuery by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val filteredPrompts = prompts.filter { 
        searchQuery.isEmpty() || it.text.contains(searchQuery, ignoreCase = true) 
    }
    
    SquareBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = White)
                }
                Text("📜 History", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(48.dp))
            }
            
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search prompts...", color = White.copy(0.5f)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    cursorColor = RedLight,
                    focusedBorderColor = Red,
                    unfocusedBorderColor = Red.copy(0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            
            Spacer(Modifier.height(16.dp))
            
            if (filteredPrompts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No prompts found", color = White.copy(0.5f), fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredPrompts) { prompt ->
                        PromptCard(prompt, db, ctx) { prompts = db.loadPrompts() }
                    }
                }
            }
        }
    }
}

@Composable
fun PromptCard(prompt: PromptDatabase.Prompt, db: PromptDatabase, ctx: Context, onRefresh: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Red.copy(0.15f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(prompt.category, color = RedLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        db.toggleFavorite(prompt.id)
                        onRefresh()
                    }) {
                        Icon(
                            if (prompt.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            "Favorite",
                            tint = if (prompt.isFavorite) RedLight else White.copy(0.5f)
                        )
                    }
                    IconButton(onClick = {
                        db.deletePrompt(prompt.id)
                        onRefresh()
                    }) {
                        Icon(Icons.Default.Delete, "Delete", tint = White.copy(0.5f))
                    }
                }
            }
            Text(prompt.text, color = White, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(
                "${prompt.text.length} chars • Used ${prompt.usageCount}x",
                color = White.copy(0.5f),
                fontSize = 11.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallButton("COPY") {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Prompt", prompt.text))
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    Toast.makeText(ctx, "Copied ✅", Toast.LENGTH_SHORT).show()
                }
                SmallButton("SHARE") {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, prompt.text)
                    }
                    ctx.startActivity(Intent.createChooser(intent, "Share"))
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(db: PromptDatabase, nav: androidx.navigation.NavController) {
    var prompts by remember { mutableStateOf(db.loadPrompts().filter { it.isFavorite }) }
    val ctx = LocalContext.current
    
    SquareBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = White)
                }
                Text("⭐ Favorites", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(48.dp))
            }
            
            if (prompts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No favorites yet", color = White.copy(0.5f), fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(prompts) { prompt ->
                        PromptCard(prompt, db, ctx) { prompts = db.loadPrompts().filter { it.isFavorite } }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsScreen(db: PromptDatabase, nav: androidx.navigation.NavController) {
    val stats by produceState(initialValue = PromptDatabase.Stats(), db) {
        value = withContext(Dispatchers.IO) { db.loadStats() }
    }
    val prompts by produceState(initialValue = emptyList<PromptDatabase.Prompt>(), db) {
        value = withContext(Dispatchers.IO) { db.loadPrompts() }
    }
    
    SquareBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = White)
                }
                Text("📊 Statistics", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(48.dp))
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Red.copy(0.2f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("OVERALL STATS", color = RedLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            StatRow("Total Generated", stats.totalGenerated.toString())
                            StatRow("Total Copied", stats.totalCopied.toString())
                            StatRow("Total Shared", stats.totalShared.toString())
                            StatRow("Favorites", prompts.count { it.isFavorite }.toString())
                            StatRow("History Size", prompts.size.toString())
                        }
                    }
                }
                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Red.copy(0.2f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("TOP CATEGORIES", color = RedLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            val categoryCounts = prompts.groupBy { it.category }.mapValues { it.value.size }
                                .toList().sortedByDescending { it.second }.take(5)
                            categoryCounts.forEach { (category, count) ->
                                StatRow(category, count.toString())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = White.copy(0.7f), fontSize = 14.sp)
        Text(value, color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SettingsScreen(db: PromptDatabase, nav: androidx.navigation.NavController) {
    val ctx = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportData by remember { mutableStateOf("") }
    
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Data?", color = White, fontWeight = FontWeight.Bold) },
            text = { Text("This will delete all history, favorites, and stats. This cannot be undone.", color = White) },
            confirmButton = {
                TextButton(onClick = {
                    db.clearAll()
                    showClearDialog = false
                    Toast.makeText(ctx, "All data cleared", Toast.LENGTH_SHORT).show()
                }) {
                    Text("CLEAR ALL", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = White.copy(0.7f))
                }
            },
            containerColor = Black,
            shape = RoundedCornerShape(16.dp)
        )
    }
    
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Data", color = White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Your data has been exported. Copy the JSON below:", color = White, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        exportData,
                        color = White.copy(0.7f),
                        fontSize = 11.sp,
                        maxLines = 10,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.background(Red.copy(0.1f)).padding(8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Export", exportData))
                    Toast.makeText(ctx, "Export copied ✅", Toast.LENGTH_SHORT).show()
                    showExportDialog = false
                }) {
                    Text("COPY", color = RedLight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close", color = White.copy(0.7f))
                }
            },
            containerColor = Black,
            shape = RoundedCornerShape(16.dp)
        )
    }
    
    SquareBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = White)
                }
                Text("⚙️ Settings", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(48.dp))
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    NeonButton("📤 EXPORT DATA") {
                        exportData = db.exportData()
                        showExportDialog = true
                    }
                }
                item {
                    NeonButton("🗑️ CLEAR ALL DATA") {
                        showClearDialog = true
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Red.copy(0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("PromptMaster v3.0", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("✅ 100% Free", color = White.copy(0.7f), fontSize = 13.sp)
                            Text("✅ No Ads", color = White.copy(0.7f), fontSize = 13.sp)
                            Text("✅ No Internet Required", color = White.copy(0.7f), fontSize = 13.sp)
                            Text("✅ Offline Storage", color = White.copy(0.7f), fontSize = 13.sp)
                            Text("✅ Open Source Friendly", color = White.copy(0.7f), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ---------- PROMPT TEMPLATES ----------
const val MAN_PROMPT = "CREATE MAN PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, MASTERPIECE, 9:16"
const val WOMEN_PROMPT = "CREATE GIRL PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, BEAUTIFUL, PERFECT MAKEUP, MASTERPIECE, 9:16"
const val COUPLES_PROMPT = "CREATE COUPLES PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, MASTERPIECE, 9:16"
const val KIDS_PROMPT = "CREATE KIDS PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, CUTE, INNOCENT, MASTERPIECE, 9:16"
const val PETS_PROMPT = "CREATE PETS PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, ADORABLE, MASTERPIECE, 9:16"
const val FANTASY_PROMPT = "CREATE FANTASY PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, MAGICAL, EPIC, MASTERPIECE, 9:16"
const val SCIFI_PROMPT = "CREATE SCI-FI PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, FUTURISTIC, MASTERPIECE, 9:16"
const val TITLE_PROMPT = "CREATE 1 MOST ENGAGING HIGH CTR TITLE UNDER 80 CHARACTERS FOR YOUTUBE SHORTS VIDEO INCLUDING 3 HASHTAGS AND SOME IMOJIES, NEVER REPEAT, COMPLETELY UNIQUE, 2 SEARCHABLE KEYWORD IN THIS ONE TITLE,"
const val CUSTOM_PROMPT_TEMPLATE = "CREATE [word] PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, MASTERPIECE, 9:16"

fun generateVariations(basePrompt: String): List<String> {
    val styles = listOf("CINEMATIC LIGHTING", "ARTISTIC COMPOSITION", "DRAMATIC SHADOWS", "MINIMALIST CLEAN", "VIBRANT COLORS")
    val effects = listOf("GOLDEN HOUR GLOW", "NEON CYBERPUNK", "MOODY ATMOSPHERE", "BRIGHT & AIRY", "ETHEREAL DREAM")
    val moods = listOf("MYSTERIOUS", "ROMANTIC", "POWERFUL", "SERENE", "DYNAMIC")
    return (0 until 5).map { i ->
        "$basePrompt, STYLE: ${styles[i]}, EFFECT: ${effects[i]}, MOOD: ${moods[i]}"
    }
}

// ---------- MAIN ACTIVITY ----------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = PromptDatabase(this)
        setContent {
            val nav = rememberNavController()
            NavHost(nav, startDestination = "home") {
                composable("home") { HomeScreen(nav, db) }
                composable("man") { CategoryScreen("MAN", MAN_PROMPT, TITLE_PROMPT, db, nav) }
                composable("women") { CategoryScreen("WOMEN", WOMEN_PROMPT, TITLE_PROMPT, db, nav) }
                composable("couples") { CategoryScreen("COUPLES", COUPLES_PROMPT, TITLE_PROMPT, db, nav) }
                composable("kids") { CategoryScreen("KIDS", KIDS_PROMPT, TITLE_PROMPT, db, nav) }
                composable("pets") { CategoryScreen("PETS", PETS_PROMPT, TITLE_PROMPT, db, nav) }
                composable("fantasy") { CategoryScreen("FANTASY", FANTASY_PROMPT, TITLE_PROMPT, db, nav) }
                composable("scifi") { CategoryScreen("SCI-FI", SCIFI_PROMPT, TITLE_PROMPT, db, nav) }
                composable("history") { HistoryScreen(db, nav) }
                composable("favorites") { FavoritesScreen(db, nav) }
                composable("stats") { StatsScreen(db, nav) }
                composable("settings") { SettingsScreen(db, nav) }
            }
        }
    }
}
