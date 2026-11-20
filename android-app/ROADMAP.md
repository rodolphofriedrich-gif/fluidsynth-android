# Plano de validação — FluidSynth Player para Android

**Atualizado em:** 20/11/2026  
**Estado:** funcionalidades consolidadas na branch principal; testes físicos do APK de referência aprovados.

## Fases implementadas

- [x] Estabilidade do ciclo de vida do motor e tratamento de desconexões.
- [x] Separação de JNI, AudioTrack e thread de renderização.
- [x] Roteamento MIDI comum para teclado virtual, USB e BLE.
- [x] Integridade MIDI, Program Change, Bank Select, Aftertouch e gestão de SoundFonts.
- [x] Tela MIDI e seleção de instrumentos por canal.
- [x] Mixer: Mute, Solo, volume master e volume individual.
- [x] Gestão de bibliotecas SF2 e persistência.
- [x] Efeitos globais Reverb e Chorus com presets.
- [x] Interface responsiva, rotação e painel de efeitos.
- [x] Acabamento de navegação e acessibilidade.

## Testes físicos

O usuário confirmou que todos os testes realizados com o APK #258 foram aprovados:

- [x] Instalação e inicialização.
- [x] Importação de bibliotecas SF2 e seleção de presets.
- [x] 16 canais independentes e reprodução simultânea.
- [x] Entrada USB MIDI.
- [x] Entrada BLE MIDI, inclusive no canal 10.
- [x] Ausência de atrasos perceptíveis e de notas presas.
- [x] Mute, Solo e volumes individuais.
- [x] Volume master e Velocity.
- [x] Reverb, Chorus e presets.
- [x] Rotação retrato/paisagem.
- [x] Painel responsivo de efeitos.
- [x] Persistência após fechar e reabrir o aplicativo.

## Builds de referência

| Build | Commit | Resultado |
|---|---|---|
| [#258](https://github.com/rodolphofriedrich-gif/fluidsynth-android/actions/runs/44753847627) | `e07c7fa1156480539f0324c475adb54e22aa09ea` | APK usado nos testes físicos; todos aprovados pelo usuário |
| [#44775429211](https://github.com/rodolphofriedrich-gif/fluidsynth-android/actions/runs/44775429211) | `788699cf3fe4061cb9e60ed162e5e4a7d6dc4b96` | Build de integração concluído com sucesso após preservar a acessibilidade dos knobs |

O build #44775429211 comprova a compilação da árvore consolidada. O APK #258 é o registro da rodada de testes físicos completa. Mudanças futuras que afetem comportamento devem receber novos testes de regressão.

## Próximas verificações recomendadas

- Fazer uma instalação limpa do APK gerado a partir da branch `master`.
- Verificar a ação de acessibilidade dos knobs com TalkBack/controle de acessibilidade.
- Repetir testes de regressão após qualquer alteração em áudio, MIDI, SoundFonts, volumes, efeitos ou ciclo de vida.

## Critério de estabilidade

Não considerar uma alteração pronta apenas porque compilou. Registrar separadamente o resultado do build e os testes físicos. Preservar o motor FluidSynth, o mutex de sincronização e a renderização estável; não remover proteções sem uma solução técnica demonstrada.
