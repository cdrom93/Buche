package com.cdrom93.buche

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cdrom93.buche.ui.theme.*
import java.text.DecimalFormat

data class WeightRange(val min: Int, val max: Int)
data class WoodFavorite(
    val name: String, 
    val value: String, 
    val isStere: Boolean,
    val surfaceArea: Double, 
    val maxHeight: Double, 
    val maxPayload: Double? = null,
    val length: Double,
    val width: Double,
    val iconType: String = "trailer"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BucheTheme {
                WoodConverterScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WoodConverterScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val sharedPrefs = remember { context.getSharedPreferences("buche_prefs", Context.MODE_PRIVATE) }
    
    var inputValue by remember { mutableStateOf("1") }
    var priceInput by remember { mutableStateOf("") }
    var priceUnitByStere by remember { mutableStateOf(true) }
    var isStereToM3 by remember { mutableStateOf(true) }
    
    var favorites by remember { mutableStateOf(loadFavorites(sharedPrefs)) }
    var selectedContainer by remember { mutableStateOf<WoodFavorite?>(null) }
    
    var showSaveDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var favoriteToEdit by remember { mutableStateOf<WoodFavorite?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<WoodFavorite?>(null) }

    val logLengths = listOf("1 m", "50 cm", "45 cm", "40 cm", "33 cm", "30 cm", "25 cm", "20 cm")
    var selectedLength by remember { mutableStateOf("50 cm") }
    val woodConditions = listOf("Rangé", "En vrac")
    var selectedCondition by remember { mutableStateOf("Rangé") }

    val woodWeights = mapOf(
        "Chêne" to WeightRange(700, 800), "Hêtre" to WeightRange(650, 750),
        "Charme" to WeightRange(750, 850), "Frêne" to WeightRange(650, 750),
        "Châtaignier" to WeightRange(550, 650), "Bouleau" to WeightRange(450, 550),
        "Peuplier" to WeightRange(350, 450), "Sapin/Épicéa" to WeightRange(350, 500),
        "Pin" to WeightRange(400, 550), "Non spécifié" to null
    )
    val woodTypes = woodWeights.keys.toList()
    var selectedWood by remember { mutableStateOf("Chêne") }

    val coefficient = getCoefficient(selectedLength, selectedCondition)
    val outputUnit = if (isStereToM3) "m³" else "STÈRES"

    val resultValue = remember(inputValue, isStereToM3, coefficient) {
        val input = inputValue.toDoubleOrNull() ?: 0.0
        if (isStereToM3) input * coefficient else input / coefficient
    }

    val steresAmount = if (isStereToM3) (inputValue.toDoubleOrNull() ?: 0.0) else resultValue
    val m3Amount = if (isStereToM3) resultValue else (inputValue.toDoubleOrNull() ?: 0.0)
    
    val weightRange = woodWeights[selectedWood]?.let {
        WeightRange((steresAmount * it.min).toInt(), (steresAmount * it.max).toInt())
    }

    val decimalFormat = DecimalFormat("#,###.##")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bûche & Stère", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Aide")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            val totalPrice = remember(inputValue, priceInput, priceUnitByStere, steresAmount, m3Amount) {
                val price = priceInput.toDoubleOrNull() ?: 0.0
                if (price > 0) decimalFormat.format((if (priceUnitByStere) steresAmount else m3Amount) * price) else null
            }
            
            var heightWarning = false
            var weightWarning = false
            selectedContainer?.let { container ->
                val surface = container.surfaceArea
                val fillH = m3Amount / surface
                heightWarning = fillH > container.maxHeight
                weightWarning = container.maxPayload?.let { (weightRange?.max ?: 0) > it } ?: false
            }

            Surface(
                tonalElevation = 8.dp, shadowElevation = 16.dp,
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(Modifier.padding(16.dp).navigationBarsPadding()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Résultat équivalent", style = MaterialTheme.typography.labelSmall)
                            Text("${decimalFormat.format(resultValue)} $outputUnit", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            weightRange?.let {
                                Text("Poids estimé", style = MaterialTheme.typography.labelSmall)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (weightWarning) Text("⚠️ ", fontSize = 18.sp)
                                    Text("${it.min}-${it.max} kg", fontSize = 18.sp, fontWeight = FontWeight.Bold, 
                                        color = if(weightWarning) MaterialTheme.colorScheme.error else Color.Unspecified)
                                }
                            }
                            if (totalPrice != null) {
                                Text("Prix total : $totalPrice €", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (selectedContainer != null) {
                        val surface = selectedContainer!!.surfaceArea
                        val fillH = m3Amount / surface
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if(selectedContainer!!.iconType == "shed") Icons.Default.Warehouse else Icons.Default.LocalShipping, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Remplissage ${selectedContainer!!.name} : ", style = MaterialTheme.typography.bodySmall)
                            if (heightWarning) Text("⚠️ ", fontSize = 14.sp)
                            Text("${decimalFormat.format(fillH * 100)} cm", fontWeight = FontWeight.Bold, 
                                color = if (heightWarning) MaterialTheme.colorScheme.error else Color.Unspecified)
                            
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = { selectedContainer = null },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("QUITTER", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = modifier.padding(paddingValues).fillMaxSize().verticalScroll(rememberScrollState())) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("QUANTITÉ ET UNITÉ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { 
                            val curr = inputValue.toDoubleOrNull() ?: 1.0
                            if (curr > 0.1) inputValue = (curr - 0.5).toString()
                        }, modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)) { Icon(Icons.Default.Remove, null) }
                        
                        OutlinedTextField(
                            value = inputValue, onValueChange = { inputValue = it }, modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            suffix = { 
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isStereToM3 = !isStereToM3 }) {
                                    Text(if(isStereToM3) "stères" else "m³", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Icon(Icons.Default.SwapVert, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                        
                        IconButton(onClick = { 
                            val curr = inputValue.toDoubleOrNull() ?: 0.0
                            inputValue = (curr + 0.5).toString()
                        }, modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)) { Icon(Icons.Default.Add, null) }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Text("FORMAT BÛCHES ET RANGEMENT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        woodConditions.forEach { SelectableButton(it, selectedCondition == it, { selectedCondition = it }, Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            logLengths.take(4).forEach { SelectableButton(it, selectedLength == it, { selectedLength = it }, Modifier.weight(1f)) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            logLengths.drop(4).forEach { SelectableButton(it, selectedLength == it, { selectedLength = it }, Modifier.weight(1f)) }
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ESSENCE ET PRIX", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    var expanded by remember { mutableStateOf(false) }
                    Box(Modifier.padding(vertical = 8.dp)) {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Nature, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(selectedWood); Spacer(Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            woodTypes.forEach { wood -> DropdownMenuItem(text = { Text(wood) }, onClick = { selectedWood = wood; expanded = false }) }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = priceInput, onValueChange = { priceInput = it }, modifier = Modifier.weight(1f), label = { Text("Prix unitaire") }, prefix = { Text("€ ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        Spacer(Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if(priceUnitByStere) "au stère" else "au m³", style = MaterialTheme.typography.bodySmall)
                            Switch(checked = !priceUnitByStere, onCheckedChange = { priceUnitByStere = !it }, modifier = Modifier.scale(0.7f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("MES REMORQUES ET ABRIS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                IconButton(onClick = { favoriteToEdit = null; showSaveDialog = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            
            if (favorites.isNotEmpty()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    favorites.forEach { fav ->
                        FavoriteItem(fav, {
                            selectedContainer = fav
                        }, {
                            favoriteToEdit = fav
                            showSaveDialog = true
                        })
                    }
                }
            }
            InfoCard()
            Spacer(Modifier.height(140.dp))
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("À propos de Bûche & Stère") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Cette application vous aide à calculer précisément vos besoins en bois de chauffage.\n\n" +
                        "• Convertissez stères ↔ m³ selon la longueur de coupe.\n" +
                        "• Estimez le poids pour sécuriser vos transports.\n" +
                        "• Gérez vos remorques pour connaître la hauteur de remplissage idéale.",
                        fontSize = 14.sp
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Vous appréciez l'application ? Elle est gratuite et sans publicité.", fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { uriHandler.openUri("https://github.com/cdrom93/Buche") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.Favorite, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Me soutenir")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showInfoDialog = false }) { Text("Fermer") } }
        )
    }

    if (showSaveDialog) {
        SaveDialog(
            initialFavorite = favoriteToEdit,
            onDismiss = { showSaveDialog = false },
            onSave = { name, w, l, h, p, icon ->
                val widthVal = w.toDoubleOrNull() ?: 0.0
                val lengthVal = l.toDoubleOrNull() ?: 0.0
                val heightVal = h.toDoubleOrNull() ?: 0.0
                val area = widthVal * lengthVal
                val newFav = WoodFavorite(name, "0", isStereToM3, area, heightVal, if(icon == "trailer") p.toDoubleOrNull() else null, lengthVal, widthVal, icon)
                favorites = favorites.filter { it.name != (favoriteToEdit?.name ?: name) } + newFav
                saveFavorites(sharedPrefs, favorites)
                if (selectedContainer?.name == favoriteToEdit?.name) selectedContainer = newFav
                showSaveDialog = false
            },
            onDelete = { fav ->
                showDeleteConfirm = fav
                showSaveDialog = false
            }
        )
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Supprimer ?") },
            text = { Text("Voulez-vous vraiment supprimer '${showDeleteConfirm?.name}' ?") },
            confirmButton = {
                TextButton(onClick = {
                    val fav = showDeleteConfirm!!
                    if (selectedContainer?.name == fav.name) selectedContainer = null
                    favorites = favorites.filter { it.name != fav.name }
                    saveFavorites(sharedPrefs, favorites)
                    showDeleteConfirm = null
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Annuler") }
            }
        )
    }
}

@Composable
fun SaveDialog(initialFavorite: WoodFavorite?, onDismiss: () -> Unit, onSave: (String, String, String, String, String, String) -> Unit, onDelete: (WoodFavorite) -> Unit) {
    var name by remember { mutableStateOf(initialFavorite?.name ?: "") }
    var w by remember { mutableStateOf(initialFavorite?.width?.toString() ?: "") }
    var l by remember { mutableStateOf(initialFavorite?.length?.toString() ?: "") }
    var h by remember { mutableStateOf(initialFavorite?.maxHeight?.toString() ?: "") }
    var p by remember { mutableStateOf(initialFavorite?.maxPayload?.toInt()?.toString() ?: "") }
    var iconType by remember { mutableStateOf(initialFavorite?.iconType ?: "trailer") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if(initialFavorite == null) "Nouveau contenant" else "Modifier")
                if (initialFavorite != null) {
                    IconButton(onClick = { onDelete(initialFavorite) }) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(
                        modifier = Modifier.weight(1f).clickable { iconType = "trailer" },
                        shape = RoundedCornerShape(8.dp),
                        color = if(iconType == "trailer") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if(iconType == "trailer") MaterialTheme.colorScheme.primary else Color.LightGray)
                    ) {
                        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LocalShipping, null)
                            Text("Remorque", fontSize = 10.sp)
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f).clickable { iconType = "shed" },
                        shape = RoundedCornerShape(8.dp),
                        color = if(iconType == "shed") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if(iconType == "shed") MaterialTheme.colorScheme.primary else Color.LightGray)
                    ) {
                        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Warehouse, null)
                            Text("Abri / Box", fontSize = 10.sp)
                        }
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = l, onValueChange = { l = it }, label = { Text("Long. (m)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = w, onValueChange = { w = it }, label = { Text("Larg. (m)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = h, onValueChange = { h = it }, label = { Text("Haut. (m)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    if (iconType == "trailer") {
                        OutlinedTextField(value = p, onValueChange = { p = it }, label = { Text("Charge (kg)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(name, w, l, h, p, iconType) }) { Text("Sauvegarder") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

fun getCoefficient(len: String, cond: String): Double {
    return if (cond == "Rangé") {
        when(len) { "1 m"->1.0; "50 cm"->0.8; "45 cm"->0.77; "40 cm"->0.74; "33 cm"->0.7; "30 cm"->0.66; "25 cm"->0.6; else->0.57 }
    } else {
        when(len) { "1 m"->1.25; "50 cm"->1.0; "45 cm"->0.96; "40 cm"->0.93; "33 cm"->0.88; "30 cm"->0.83; "25 cm"->0.75; else->0.71 }
    }
}

@Composable
fun FavoriteItem(favorite: WoodFavorite, onSelect: () -> Unit, onEdit: () -> Unit) {
    val df = DecimalFormat("#.##")
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).clickable { onSelect() },
        shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface
    ) {
        Row(modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if(favorite.iconType == "shed") Icons.Default.Warehouse else Icons.Default.LocalShipping, null, Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(favorite.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                val vol = favorite.maxHeight * favorite.surfaceArea
                val details = if (favorite.iconType == "trailer" && favorite.maxPayload != null) {
                    "Capacité : ${df.format(vol)}m³ | H: ${df.format(favorite.maxHeight*100)}cm | P: ${favorite.maxPayload.toInt()}kg"
                } else {
                    "Capacité : ${df.format(vol)}m³ | H: ${df.format(favorite.maxHeight*100)}cm"
                }
                Text(text = details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Edit, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) }
        }
    }
}

@Composable
fun SelectableButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (cont, content) = if (selected) {
        if(isSystemInDarkTheme()) DarkButtonSelected to DarkOnButtonSelected else LightButtonSelected to LightOnButtonSelected
    } else Color.Transparent to MaterialTheme.colorScheme.onSurface
    Button(onClick = onClick, modifier = modifier.height(40.dp), contentPadding = PaddingValues(horizontal = 4.dp), shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = cont, contentColor = content), border = if (selected) null else BorderStroke(1.dp, Color.LightGray)
    ) { Text(text, textAlign = TextAlign.Center, fontSize = 13.sp, maxLines = 1) }
}

@Composable
fun InfoCard() {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text("Le volume apparent change selon la coupe (ex: 33cm vrac = 0,7 m³ pour 1 stère), mais la quantité de bois reste identique.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun saveFavorites(prefs: android.content.SharedPreferences, list: List<WoodFavorite>) {
    val set = list.map { "${it.name}|${it.value}|${it.isStere}|${it.surfaceArea}|${it.maxHeight}|${it.maxPayload ?: ""}|${it.length}|${it.width}|${it.iconType}" }.toSet()
    prefs.edit().putStringSet("favorites", set).apply()
}

private fun loadFavorites(prefs: android.content.SharedPreferences): List<WoodFavorite> {
    val set = prefs.getStringSet("favorites", emptySet()) ?: emptySet()
    val list = mutableListOf<WoodFavorite>()
    for (s in set) {
        try {
            val p = s.split("|")
            if (p.size >= 5) {
                list.add(WoodFavorite(
                    p[0], p[1], p[2].toBoolean(), p[3].toDouble(), p[4].toDouble(),
                    p.getOrNull(5)?.toDoubleOrNull(),
                    p.getOrNull(6)?.toDoubleOrNull() ?: 0.0,
                    p.getOrNull(7)?.toDoubleOrNull() ?: 0.0,
                    p.getOrNull(8) ?: "trailer"
                ))
            }
        } catch (e: Exception) { /* Ignorer les données corrompues */ }
    }
    return list.sortedBy { it.name }
}
