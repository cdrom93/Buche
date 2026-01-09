package com.example.buche

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.buche.ui.theme.*
import java.text.DecimalFormat

data class WeightRange(val min: Int, val max: Int)

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
    var inputValue by remember { mutableStateOf("1") }
    var priceInput by remember { mutableStateOf("") }
    var priceUnitByStere by remember { mutableStateOf(true) }
    var isStereToM3 by remember { mutableStateOf(true) }

    val logLengths = listOf("1 m", "50 cm", "45 cm", "40 cm", "33 cm", "30 cm", "25 cm", "20 cm")
    var selectedLength by remember { mutableStateOf("50 cm") }

    val woodConditions = listOf("Rangé", "En vrac")
    var selectedCondition by remember { mutableStateOf("Rangé") }

    val woodWeights = mapOf(
        "Chêne" to WeightRange(700, 800),
        "Hêtre" to WeightRange(650, 750),
        "Charme" to WeightRange(750, 850),
        "Frêne" to WeightRange(650, 750),
        "Châtaignier" to WeightRange(550, 650),
        "Bouleau" to WeightRange(450, 550),
        "Peuplier" to WeightRange(350, 450),
        "Sapin/Épicéa" to WeightRange(350, 500),
        "Pin" to WeightRange(400, 550),
        "Non spécifié" to null
    )
    val woodTypes = woodWeights.keys.toList()
    var selectedWood by remember { mutableStateOf("Chêne") }

    val stackedCoefficient = when (selectedLength) {
        "1 m" -> 1.0; "50 cm" -> 0.80; "45 cm" -> 0.77; "40 cm" -> 0.74
        "33 cm" -> 0.70; "30 cm" -> 0.66; "25 cm" -> 0.60; "20 cm" -> 0.57
        else -> 1.0
    }
    val bulkCoefficient = when (selectedLength) {
        "1 m" -> 1.25; "50 cm" -> 1.0; "45 cm" -> 0.96; "40 cm" -> 0.93
        "33 cm" -> 0.88; "30 cm" -> 0.83; "25 cm" -> 0.75; "20 cm" -> 0.71
        else -> 1.25
    }

    val coefficient = if (selectedCondition == "Rangé") stackedCoefficient else bulkCoefficient

    val (inputUnit, outputUnit) = if (isStereToM3) "STÈRES" to "m³" else "m³" to "STÈRES"

    val result = remember(inputValue, isStereToM3, coefficient) {
        val input = inputValue.toDoubleOrNull() ?: 0.0
        if (isStereToM3) input * coefficient else if (coefficient != 0.0) input / coefficient else 0.0
    }

    val steresAmount = if (isStereToM3) (inputValue.toDoubleOrNull() ?: 0.0) else result
    val m3Amount = if (isStereToM3) result else (inputValue.toDoubleOrNull() ?: 0.0)
    
    val weightRange = woodWeights[selectedWood]?.let {
        WeightRange((steresAmount * it.min).toInt(), (steresAmount * it.max).toInt())
    }

    val decimalFormat = DecimalFormat("#,###.##")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Bûche & Stère", fontWeight = FontWeight.Bold)
                        Text("Calculateur de volume", style = MaterialTheme.typography.bodySmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Stères", fontWeight = if (isStereToM3) FontWeight.Bold else FontWeight.Normal)
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalIconButton(onClick = { isStereToM3 = !isStereToM3 }) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Inverser")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mètres Cube (m³)", fontWeight = if (!isStereToM3) FontWeight.Bold else FontWeight.Normal)
                    }
                    Spacer(Modifier.height(16.dp))

                    Text("QUANTITÉ (${inputUnit})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val current = inputValue.toIntOrNull() ?: 1
                                if (current > 1) inputValue = (current - 1).toString()
                            },
                            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Moins")
                        }

                        OutlinedTextField(
                            value = inputValue,
                            onValueChange = { inputValue = it },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )

                        IconButton(
                            onClick = {
                                val current = inputValue.toIntOrNull() ?: 0
                                inputValue = (current + 1).toString()
                            },
                            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Plus")
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (priceUnitByStere) "PRIX AU STÈRE" else "PRIX AU m³",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("€/Stère", style = MaterialTheme.typography.bodySmall)
                            Switch(
                                checked = !priceUnitByStere,
                                onCheckedChange = { priceUnitByStere = !it },
                                modifier = Modifier.scale(0.7f).padding(horizontal = 4.dp)
                            )
                            Text("€/m³", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ex: 75") },
                        prefix = { Text("€ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(Modifier.height(24.dp))

                    Text("LONGUEUR DES BÛCHES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            logLengths.take(4).forEach { length ->
                                SelectableButton(length, selectedLength == length, { selectedLength = length }, Modifier.weight(1f))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            logLengths.drop(4).forEach { length ->
                                SelectableButton(length, selectedLength == length, { selectedLength = length }, Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    Text("RANGEMENT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        woodConditions.forEach { condition ->
                            SelectableButton(condition, selectedCondition == condition, { selectedCondition = condition }, Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    Text("ESSENCE DE BOIS (POUR LE POIDS)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedWood)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            woodTypes.forEach { wood ->
                                DropdownMenuItem(text = { Text(wood) }, onClick = { selectedWood = wood; expanded = false })
                            }
                        }
                    }
                }
            }

            val totalPrice = remember(inputValue, priceInput, priceUnitByStere, steresAmount, m3Amount) {
                val price = priceInput.toDoubleOrNull() ?: 0.0
                if (price > 0) {
                    val amountToMultiply = if (priceUnitByStere) steresAmount else m3Amount
                    decimalFormat.format(amountToMultiply * price)
                } else null
            }

            ResultCard(
                result = decimalFormat.format(result),
                outputUnit = outputUnit,
                totalPrice = totalPrice,
                coefficient = coefficient,
                format = "$selectedLength, $selectedCondition"
            )

            if (weightRange != null) {
                WeightCard(weightRange, selectedWood)
            }
            InfoCard()
        }
    }
}

@Composable
fun SelectableButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (containerColor, contentColor) = if (selected) {
        if(isSystemInDarkTheme()) DarkButtonSelected to DarkOnButtonSelected
        else LightButtonSelected to LightOnButtonSelected
    } else {
        Color.Transparent to MaterialTheme.colorScheme.onSurface
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(IntrinsicSize.Max),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = if (selected) null else BorderStroke(1.dp, Color.LightGray)
    ) {
        Text(text, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 4.dp))
    }
}

@Composable
fun ResultCard(result: String, outputUnit: String, totalPrice: String?, coefficient: Double, format: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Résultat équivalent", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
            Text("$result $outputUnit", color = MaterialTheme.colorScheme.onPrimary, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            
            if (totalPrice != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                )
                Text("Prix total estimé", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                Text("$totalPrice €", color = MaterialTheme.colorScheme.onPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
            Text("Coefficient appliqué : $coefficient", color = MaterialTheme.colorScheme.onPrimary)
            Text("Format : $format", color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
fun WeightCard(range: WeightRange, essence: String) {
    val containerColor = if (isSystemInDarkTheme()) DarkCard else LightButtonSelected
    val contentColor = if(isSystemInDarkTheme()) DarkOnBackground else LightOnButtonSelected
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Poids estimé (sec)", color = contentColor.copy(alpha = 0.8f))
            Text("${range.min} - ${range.max} kg", color = contentColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Essence : $essence", color = contentColor)
        }
    }
}

@Composable
fun InfoCard() {
    val containerColor = if (isSystemInDarkTheme()) DarkInfoCard else LightInfoCard
    val contentColor = if(isSystemInDarkTheme()) DarkOnInfoCard else LightOnInfoCard
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, contentDescription = "Information", tint = contentColor)
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Pourquoi le volume change ?", fontWeight = FontWeight.Bold, color = contentColor)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Un stère correspond toujours à 1 m³ de bois coupé en 1 mètre. Quand on coupe les bûches plus court (ex: 33cm), elles s’imbriquent mieux et le tas prend moins de place (0,7 m³), mais vous avez toujours la même quantité de bois !",
                    fontSize = 14.sp, color = contentColor
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WoodConverterScreenPreview() {
    BucheTheme {
        WoodConverterScreen()
    }
}
