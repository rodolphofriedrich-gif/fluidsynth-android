# Arquitetura do FluidSynth Player para Android

## Escopo de plataforma

O produto deste repositório é um aplicativo **exclusivamente Android**. O código em `src/` e `include/` contém o motor FluidSynth de origem, que é multiplataforma; isso não significa que exista um aplicativo deste projeto para Windows, Linux, macOS ou iOS.

O build do aplicativo é feito pelo módulo Gradle em `android-app/`, com integração nativa por Android NDK/CMake.

## Princípios obrigatórios

1. Cada módulo deve ter uma responsabilidade clara e, sempre que possível, permanecer em arquivo próprio.
2. A interface não deve conter a implementação do motor nem o processamento de áudio.
3. USB e BLE devem encaminhar eventos por uma camada MIDI comum.
4. O motor FluidSynth não deve conhecer Activity, Views ou componentes visuais.
5. Alterações devem ser pequenas, isoladas e testáveis.
6. O motor de áudio estável não deve ser alterado por conveniência durante mudanças de interface.
7. Uma compilação verde não substitui testes físicos em aparelhos e controladores reais.

## Fluxo de áudio e MIDI

```
Teclado virtual ─┐
USB MIDI ────────┼──> MidiRouter ──> NativeBridge ──> SynthEngine / FluidSynth
BLE MIDI ────────┘                                      │
                                                       v
AudioEngine <──────────── renderização nativa <────────┘
      │
      v
AudioTrack (Android)
```

O `MidiRouter` centraliza os eventos das diferentes origens. Ele rastreia notas e sustain por origem e canal para impedir que a soltura de uma origem interrompa uma nota ainda mantida por outra. Também libera notas pendentes ao desconectar uma origem ou encerrar a sessão e trata All Notes Off, All Sounds Off e Reset All Controllers.

## Módulos implementados

| Arquivo ou módulo | Responsabilidade atual |
|---|---|
| `MainActivity.java` | Ciclo de vida da Activity, coordenação das telas e integração dos módulos da aplicação |
| `MidiScreen.java` | Construção da interface da aba MIDI: canais, seleção de instrumento e teclado virtual |
| `SettingsScreen.java` | Construção da interface da aba Configurações: dispositivos MIDI, polifonia, BLE e bibliotecas SF2 |
| `AppUi.java` | Componentes visuais compartilhados, estilos, cores e painéis |
| `ScreenNavigator.java` | Navegação entre as telas MIDI e Configurações, preservando o estado dos controles |
| `Track.java` / `TrackManager.java` | Estado dos 16 canais, SoundFont/preset por canal, ativação e restauração persistente |
| `SoundFontManager.java` | Importação, validação, carregamento, listagem de presets e descarregamento de SF2 |
| `SoundFontLibraryStore.java` | Disponibilidade ativada/desativada das bibliotecas importadas |
| `PolyphonySettingsStore.java` | Persistência do limite de polifonia |
| `VelocitySettingsStore.java` | Persistência da Velocity do teclado virtual |
| `UsbMidiInput.java` | Descoberta e recepção de MIDI USB e dispositivos virtuais do serviço MIDI Android |
| `BleMidiInput.java` | Busca, permissões, conexão e recepção BLE MIDI |
| `MidiRouter.java` | Entrada MIDI comum, rastreamento de notas/sustain e limpeza por origem |
| `NativeBridge.java` | Declarações JNI e acesso Java ao motor nativo |
| `NativeEngineLifecycle.java` | Contagem de referências e coordenação do ciclo de vida do motor nativo |
| `AudioEngine.java` | AudioTrack, thread de renderização, escrita dos buffers e diagnóstico de underruns |
| `synth_engine.cpp/.h` | Implementação nativa que encapsula as operações do FluidSynth |
| `native_player.cpp` | Funções JNI exportadas e encaminhamento para o motor nativo |

Os arquivos Java ficam em `android-app/app/src/main/java/com/rodolphofriedrich/fluidsynth/`; os arquivos C++ específicos da integração Android ficam em `android-app/app/src/main/cpp/`.

## Decisões de estabilidade e desempenho

- Todas as declarações JNI estão centralizadas em `NativeBridge.java`; a Activity não é proprietária direta da interface JNI.
- `AudioEngine.java` administra a saída `AudioTrack` e sua thread, separadamente da interface.
- `NativeEngineLifecycle` impede que uma Activity antiga encerre o motor enquanto uma nova instância já o utiliza.
- A listagem de presets utiliza uma instância temporária do FluidSynth, separada do sintetizador ativo, para evitar bloquear a renderização durante essa consulta.
- O descarregamento de SoundFonts utiliza `reset_presets=0`, evitando redefinir os presets dos demais canais.
- A carga efetiva de um SF2 ainda utiliza o mutex do motor. Esse bloqueio não foi removido: fazê-lo sem uma estratégia de sincronização segura poderia introduzir corrupção de estado ou falhas de áudio.
- A listagem isolada de presets pode consumir memória adicional temporária para carregar o SF2.
- A saída solicita o modo de baixa latência quando disponível e monitora underruns; o resultado depende do hardware e da implementação de áudio do Android.

## Limites conhecidos da organização atual

A interface é construída programaticamente em classes próprias: `MidiScreen.java` e `SettingsScreen.java` montam cada tela, enquanto `AppUi.java` centraliza estilos e componentes visuais compartilhados. `MainActivity.java` coordena as telas e conecta os controles à lógica do aplicativo. A separação da construção visual não equivale a uma arquitetura MVVM completa nem a uma camada de serviço Android dedicada.

Essas separações podem evoluir em etapas posteriores, desde que não provoquem alterações desnecessárias no motor ou regressões nos recursos já validados.

## Estado das fases de trabalho

- [x] Fase 1 — correções imediatas de estabilidade e ciclo de vida.
- [x] Fase 2 — separação de JNI e saída de áudio.
- [x] Fase 3 — roteamento MIDI unificado.
- [x] Fase 4 — desempenho e integridade MIDI/áudio.
- [ ] Fase 5 — testes físicos completos no aparelho e com controladores reais.

Os builds das fases 1 a 4 foram aprovados. A validação física completa da fase 5 permanece necessária; consulte [android-app/ROADMAP.md](android-app/ROADMAP.md).
