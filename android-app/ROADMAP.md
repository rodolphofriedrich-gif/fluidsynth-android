# Plano de validação — FluidSynth Player para Android

**Atualizado em:** 18/11/2026  
**Estado do desenvolvimento:** fases 1 a 4 implementadas; fase 5 pendente.

## Fases concluídas em código

- [x] **Fase 1 — estabilidade imediata:** ciclo de vida do motor, sincronização do teclado virtual após remoção USB, fallback de preset e mensagens de erro.
- [x] **Fase 2 — camada de áudio:** declarações JNI centralizadas em `NativeBridge.java`; saída `AudioTrack` e thread em `AudioEngine.java`; ciclo de vida nativo em `NativeEngineLifecycle.java`.
- [x] **Fase 3 — roteamento MIDI:** `MidiRouter.java` centraliza teclado virtual, USB e BLE, rastreia notas/sustain por origem e limpa eventos pendentes.
- [x] **Fase 4 — integridade e desempenho:** listagem de presets isolada, descarregamento SF2 sem redefinir os demais canais, Bank Select MSB/LSB, conversão UTF-8, modo de baixa latência/diagnóstico de underruns e Aftertouch BLE.

## Builds Android aprovados

| Fase | Execução |
|---|---|
| 1 | [GitHub Actions #104](https://github.com/rodolphofriedrich-gif/fluidsynth-android/actions/runs/44100537474) |
| 2 | [GitHub Actions #116](https://github.com/rodolphofriedrich-gif/fluidsynth-android/actions/runs/44103229462) |
| 3 | [GitHub Actions #135](https://github.com/rodolphofriedrich-gif/fluidsynth-android/actions/runs/44104444299) |
| 4 | [GitHub Actions #171](https://github.com/rodolphofriedrich-gif/fluidsynth-android/actions/runs/44105701540) |

Build aprovado significa que o APK foi compilado. Não significa que todos os testes físicos foram aprovados.

## Fase 5 — validação física

- [ ] Rotacionar a tela durante a reprodução e verificar a continuidade do áudio.
- [ ] Desconectar USB durante a reprodução e verificar a liberação das notas e a retomada do teclado virtual.
- [ ] Conectar, desconectar e reconectar um controlador BLE MIDI.
- [ ] Testar notas simultâneas da origem virtual, USB e BLE, incluindo Note Off e sustain.
- [ ] Testar Program Change e Bank Select recebidos externamente.
- [ ] Testar os 16 canais com SoundFonts/presets independentes.
- [ ] Importar e carregar bibliotecas SF2 durante a reprodução.
- [ ] Manter o aplicativo em execução por período prolongado e observar falhas de áudio/underruns.

## Critério de conclusão

Cada item deve ser testado no aparelho e registrado como aprovado, reprovado ou não testado. Não declarar o aplicativo pronto para distribuição antes de analisar falhas e concluir os testes pertinentes.

As fases 1 a 4 não alteraram a necessidade de preservar o mutex do motor durante operações que exigem sincronização. A remoção desse bloqueio não faz parte desta validação e não deve ser feita sem uma solução técnica segura.
