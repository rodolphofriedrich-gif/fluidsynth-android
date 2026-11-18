# FluidSynth Player para Android — referência funcional

## Base funcional anteriormente validada

**Branch de referência:** `feature/polyphony-control`  
**Commit:** `95bd9d876147bfd9e21944b16b9c6c5375df11a2`

Os seguintes comportamentos foram confirmados em aparelho real nessa base:

- Trocar entre dois presets no mesmo canal altera o timbre reproduzido.
- Dois canais podem utilizar instrumentos diferentes de forma independente.
- Após fechar completamente e reabrir o aplicativo, as bibliotecas e os instrumentos selecionados são restaurados.

Esses resultados comprovam os comportamentos listados na versão de referência. Eles não devem ser apresentados como validação física automática de todas as mudanças posteriores.

## Mudanças posteriores

O desenvolvimento avançou com:

- 16 canais MIDI independentes.
- Seleção de SoundFont e preset por canal.
- Persistência do estado dos canais e das bibliotecas.
- Controle de ativação/silenciamento e polifonia.
- Teclado virtual com Velocity ajustável.
- Entrada MIDI USB e BLE.
- Roteamento MIDI unificado com rastreamento de notas e sustain por origem.
- Separação da saída AudioTrack e das declarações JNI.
- Listagem de presets em instância temporária do FluidSynth.
- Melhorias no tratamento de Program Change, Bank Select, Aftertouch BLE e descarregamento de SoundFonts.

## Builds das fases 1 a 4

Os seguintes builds Android do desenvolvimento foram aprovados:

- Fase 1 — [execução #104](https://github.com/rodolphofriedrich-gif/fluidsynth-android/actions/runs/44100537474).
- Fase 2 — [execução #116](https://github.com/rodolphofriedrich-gif/fluidsynth-android/actions/runs/44103229462).
- Fase 3 — [execução #135](https://github.com/rodolphofriedrich-gif/fluidsynth-android/actions/runs/44104444299).
- Fase 4 — [execução final #171](https://github.com/rodolphofriedrich-gif/fluidsynth-android/actions/runs/44105701540).

Essas execuções comprovam a compilação do APK. Não comprovam, por si só, o funcionamento físico de USB, BLE, rotação ou compatibilidade com todos os dispositivos.

## Fase 5 — testes físicos pendentes

Os seguintes testes ainda precisam ser executados na versão atual:

1. Girar a tela durante a reprodução e verificar se o motor continua ativo.
2. Desconectar USB durante o uso e confirmar a retomada do teclado virtual.
3. Conectar, desconectar e reconectar um controlador BLE MIDI.
4. Tocar a mesma nota por origens diferentes e verificar Note Off e sustain.
5. Verificar Program Change e Bank Select recebidos de um controlador.
6. Testar os 16 canais com instrumentos independentes.
7. Importar e carregar SF2 durante a reprodução.
8. Manter uma sessão prolongada e observar interrupções, falhas e underruns.

Registre separadamente o resultado de cada teste. Não marque um teste como aprovado apenas porque o APK compilou.

## Regra para próximas alterações

Preservar os comportamentos físicos já confirmados. Alterações devem ser pequenas, modulares e acompanhadas de build Android. O motor FluidSynth e a renderização não devem ser modificados sem necessidade técnica demonstrada.
