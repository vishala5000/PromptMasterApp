
package com.promptmaster.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private val Red      = Color(0xFFD32F2F)
private val RedLight = Color(0xFFFF1744)
private val Black    = Color(0xFF000000)
private val White    = Color(0xFFFFFFFF)

// ---------- Square-pattern background ----------
@Composable
fun SquareBackground(content: @Composable () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "bg")
    val offset by infinite.animateFloat(
        initialValue = 0f, targetValue = 80f,
        animationSpec = infiniteRepeatable(
            tween(8000, easing = LinearEasing), RepeatMode.Restart
        ), label = "offset"
    )
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Black)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 80f
            val stroke = Stroke(width = 1.2f)
            var y = -step + (offset % step)
            while (y < size.height + step) {
                var x = -step + (offset % step)
                while (x < size.width + step) {
                    drawRect(
                        color = Red.copy(alpha = 0.25f),
                        topLeft = Offset(x, y),
                        size = Size(step, step),
                        style = stroke
                    )
                    x += step
                }
                y += step
            }
        }
        Box(modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Red.copy(alpha = 0.25f), Color.Transparent),
                    radius = 900f
                )
            ))
        content()
    }
}

// ---------- Animated button ----------
@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = 400f), label = "scale"
    )
    val glow by animateFloatAsState(
        if (pressed) 0.9f else 0.55f, label = "glow"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .scale(scale)
            .background(
                brush = Brush.linearGradient(
                    listOf(RedLight, Red)
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // outer glow stroke
        Canvas(modifier = Modifier
            .matchParentSize()
            .background(
                shape = RoundedCornerShape(18.dp), color = Color.Transparent
            )) {
            drawRoundRect(
                color = White.copy(alpha = glow),
                style = Stroke(width = 2.5f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f)
            )
        }
        Text(
            text = text,
            color = White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

// ---------- Home screen ----------
@Composable
fun HomeScreen(nav: androidx.navigation.NavController) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    SquareBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = shown,
                enter = fadeIn(tween(700)) + slideInVertically { -80 }
            ) {
                Text(
                    "PROMPT\nMASTER",
                    color = White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 46.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            AnimatedVisibility(
                visible = shown,
                enter = fadeIn(tween(900))
            ) {
                Text(
                    "Pick a category",
                    color = White.copy(alpha = 0.75f),
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(40.dp))
            AnimatedVisibility(
                visible = shown,
                enter = fadeIn(tween(600)) + slideInVertically { 120 }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    NeonButton("👨  MAN")    { nav.navigate("man") }
                    NeonButton("👩  WOMEN")  { nav.navigate("women") }
                    NeonButton("💑  COUPLES"){ nav.navigate("couples") }
                }
            }
        }
    }
}

// ---------- Category screen ----------
@Composable
fun CategoryScreen(
    title: String,
    promptText: String,
    titleText: String
) {
    val ctx = LocalContext.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(title) { shown = false }
    LaunchedEffect(title) { shown = true }

    fun copy(label: String, text: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text.trim()))
        Toast.makeText(ctx, "$label copied ✅", Toast.LENGTH_SHORT).show()
    }

    SquareBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = shown,
                enter = fadeIn(tween(600)) + slideInVertically { -60 }
            ) {
                Text(
                    title,
                    color = White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.height(40.dp))
            AnimatedVisibility(
                visible = shown,
                enter = fadeIn(tween(700)) + slideInVertically { 100 }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    NeonButton("$title PROMPT") { copy("Prompt", promptText) }
                    NeonButton("TITLE PROMPT")  { copy("Title",  titleText)  }
                }
            }
        }
    }
}

// ---------- Prompts ----------
const val MAN_PROMPT = """CREATE MAN PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, MASTERPIECE, 9:16"""
const val WOMEN_PROMPT = """CREATE GIRL PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, BEAUTIFUL, PERFECT MAKEUP, MASTERPIECE, 9:16"""
const val COUPLES_PROMPT = """CREATE COUPLES PHOTO EDITING PROMPT UNDER 400 CHARACTERS, UNIQUE OUTFIT, UNIQUE BACKGROUND, UNIQUE STYLE, UNIQUE POSE, UNIQUE SETTING, UNIQUE EFFECT, FACE 100% LOCKED, HYPER REALISTIC, 8K, PHOTOREALISTIC, NEVER REPEAT, COMPLETELY UNQIUE, MASTERPIECE, 9:16"""
const val TITLE_PROMPT = """CREATE 1 MOST ENGAGING HIGH CTR TITLE UNDER 80 CHARACTERS FOR YOUTUBE SHORTS VIDEO INCLUDING 3 HASHTAGS AND SOME IMOJIES, NEVER REPEAT, COMPLETELY UNIQUE, 2 SEARCHABLE KEYWORD IN THIS ONE TITLE,"""

// ---------- MainActivity ----------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val nav = rememberNavController()
            NavHost(nav, startDestination = "home") {
                composable("home")   { HomeScreen(nav) }
                composable("man")    { CategoryScreen("MAN",    MAN_PROMPT,    TITLE_PROMPT) }
                composable("women")  { CategoryScreen("WOMEN",  WOMEN_PROMPT,  TITLE_PROMPT) }
                composable("couples"){ CategoryScreen("COUPLES",COUPLES_PROMPT,TITLE_PROMPT) }
            }
        }
    }
}
