# FluidSynth Player para Android — referência funcional

## Versão fisicamente testada

**APK de referência:** build #258  
**Commit testado:** `e07c7fa1156480539f0324c475adb54e22aa09ea`  
**Build:** [GitHub Actions #258](https://github.com/rodolphofriedrich-gif/fluidsynth-android/actions/runs/44753847627)  
**Artefato:** `fluidsynth-player-debug-apk`

O usuário confirmou que todos os testes realizados com esse APK foram aprovados.

## Testes físicos confirmados

- Instalação e inicialização.
- Importação e seleção de bibliotecas SF2.
- Seleção de instrumentos nos 16 canais.
- Reprodução simultânea e troca de presets.
- Entrada MIDI por USB.
- Entrada MIDI por BLE, inclusive no canal 10.
- Reprodução sem atrasos perceptíveis e sem notas presas.
- Mute e Solo.
- Volume individual dos canais.
- Volume master e Velocity.
- Reverb, Chorus e presets de efeitos.
- Rotação entre retrato e paisagem.
- Painel de efeitos responsivo em telas estreitas e largas.
- Persistência após fechar completamente e reabrir o aplicativo.

## Consolidação na branch principal

As alterações do APK testado foram integradas à branch `master` pelo PR #17.

- PR: [#17 — Consolidação da versão Android testada](https://github.com/rodolphofriedrich-gif/fluidsynth-android/pull/17).
- Commit de merge: `7dc598db550074688eb628b9395a84f1c453cc79`.
- Build de integração: [GitHub Actions #44775429211](https://github.com/rodolphofriedrich-gif/fluidsynth-android/actions/runs/44775429211).
- Resultado do build de integração: **sucesso**.

O build de integração inclui a preservação e correção da ação de acessibilidade dos knobs. O build #258 é o registro dos testes físicos completos; o build #44775429211 comprova a compilação da árvore consolidada.

## Regra para próximas alterações

Preservar os comportamentos físicos já confirmados. Alterações devem ser pequenas, modulares e acompanhadas de build Android. O motor FluidSynth, a renderização e o roteamento estável não devem ser modificados sem necessidade técnica demonstrada.

Uma mudança futura que afete áudio, MIDI, SoundFonts, volumes, efeitos, orientação ou acessibilidade deve receber build e teste de regressão antes de ser considerada estável.
