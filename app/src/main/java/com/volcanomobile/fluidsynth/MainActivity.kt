package com.volcanomobile.fluidsynth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    // Instância do motor nativo
    private val synthEngine = FluidSynthEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializa o FluidSynth nativo
        synthEngine.initSynth()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E),
                    primary = Color(0xFFBB86FC)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(synthEngine)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        synthEngine.closeSynth()
    }
}

data class MidiChannel(val id: Int, var presetName: String, var active: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(engine: FluidSynthEngine) {
    // Lista simulada dos 16 canais MIDI (como na interface clássica)
    val channels = remember {
        mutableStateListOf<MidiChannel>().apply {
            for (i in 1..16) {
                add(MidiChannel(i, if (i == 1) "Piano 1" else "No preset", i == 1))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FluidSynth Custom UI", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarDefaults(containerColor = Color(0xFF1F1F1F))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Status da porta MIDI ou SoundFont
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
            ) {
                Text(
                    text = "Input port not connected!",
                    color = Color.LightGray,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp
                )
            }

            // Lista de Canais
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(channels) { channel ->
                    ChannelRow(channel = channel) {
                        // Ação ao clicar para trocar o preset/instrumento
                        // Aqui você poderá chamar o seletor de SoundFont ou mudar o programa MIDI
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelRow(channel: MidiChannel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Número do Canal
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF333333), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${channel.id}",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Nome do Instrumento
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Canal MIDI ${channel.id}",
                color = Color.Gray,
                fontSize = 11.sp
            )
            Text(
                text = channel.presetName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Botão / Indicador de Ativo
        Switch(
            checked = channel.active,
            onCheckedChange = { channel.active = it }
        )
    }
}
