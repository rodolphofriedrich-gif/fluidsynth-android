# FluidSynth Player para Android

Aplicativo de sintetizador de áudio para **Android**, baseado no motor nativo FluidSynth e compatível com bibliotecas SoundFont 2 (SF2).

> **Plataforma do aplicativo: somente Android.** Este projeto não oferece versões do aplicativo para Windows, Linux, macOS ou iOS. O fato de o código-fonte original do FluidSynth ser multiplataforma não significa que este aplicativo também seja.

O repositório mantém parte do código-fonte e dos arquivos de referência do FluidSynth original para compilar o motor utilizado pelo aplicativo. Os documentos identificados como upstream descrevem o projeto de origem, não outros destinos de distribuição do aplicativo.

## Recursos

- Motor FluidSynth nativo integrado ao Android por JNI.
- Até 16 canais MIDI independentes, cada um com SoundFont e preset próprios.
- Importação de bibliotecas SF2 em Configurações, com validação inicial e progresso de cópia.
- Ativação, desativação e exclusão de bibliotecas não utilizadas por canais.
- Persistência das bibliotecas, instrumentos, presets e configurações.
- Teclado virtual com Velocity ajustável de 1 a 127.
- Entrada MIDI por USB, dispositivos virtuais do serviço MIDI Android e BLE MIDI.
- Roteamento MIDI comum para teclado virtual, USB e BLE, com rastreamento de notas e sustain por origem.
- Mute/ativação e Solo por canal.
- Volume individual por canal e volume master.
- Controle de polifonia.
- Efeitos globais Reverb e Chorus, com presets.
- Interface responsiva: canais em grade 2 × 8 no retrato e 4 × 4 na paisagem.
- Painel de efeitos em tela inteira em telas estreitas e painel dividido em telas maiores.
- Saída de áudio estéreo pelo Android usando `AudioTrack`.

USB MIDI e BLE MIDI são entradas para o aplicativo. A saída de áudio do sintetizador é produzida no próprio Android. O projeto não deve ser descrito como um roteador de saída MIDI para instrumentos externos.

## Requisitos Android

- Android 6.0 (API 23) ou superior.
- Para MIDI USB: aparelho com USB host/OTG e controlador reconhecido pelo serviço MIDI do Android.
- Para BLE MIDI: Bluetooth LE e controlador compatível com o perfil BLE MIDI.
- Android 12 ou superior: permissões Bluetooth Scan e Connect.
- Android 6 a 11: permissão de localização precisa para busca BLE.

USB e BLE são recursos opcionais. A compatibilidade depende do aparelho, da versão do Android, do controlador e dos serviços de sistema disponíveis. O aplicativo solicita modo de baixa latência quando disponível, mas isso não garante a mesma latência em todos os dispositivos.

## Gerar o APK de teste

O fluxo de compilação é **Build Android APK**, executado pelo GitHub Actions. O runner pode usar Ubuntu como ambiente de compilação; o artefato produzido continua sendo um APK Android.

1. Abra a aba **Actions** deste repositório.
2. Selecione **Build Android APK**.
3. Execute **Run workflow** ou abra uma execução concluída com sucesso.
4. Na seção **Artifacts**, baixe `fluidsynth-player-debug-apk`.
5. Extraia o ZIP para obter `app-debug.apk`.

O APK de depuração não é uma versão de lançamento assinada para distribuição comercial.

### Compilação manual

Ambiente utilizado pelo fluxo automatizado:

- JDK 17.
- Gradle 8.9.
- Android SDK Platform 35 e Build Tools 35.0.0.
- CMake 3.22.1.
- Android NDK 27.2.12479018.

Inicialize os submódulos:

```bash
git submodule update --init --recursive
```

Com as ferramentas instaladas, execute:

```bash
cd android-app
gradle --no-daemon :app:assembleDebug
```

O APK será gerado em:

```
android-app/app/build/outputs/apk/debug/app-debug.apk
```

O módulo Android configura os ABIs `arm64-v8a`, `armeabi-v7a` e `x86_64`.

## Organização do código

- `android-app/`: aplicativo Android, interface, integração JNI, áudio, MIDI, SoundFonts e canais.
- `android-app/app/src/main/java/`: módulos Java do aplicativo.
- `MidiScreen.java` e `SettingsScreen.java`: construção independente das telas MIDI e Configurações.
- `EffectsScreen.java` e `EffectsWorkspaceController.java`: interface de efeitos e comportamento responsivo do painel.
- `AppUi.java`: estilos e componentes visuais compartilhados.
- `MainActivity.java`: coordenação das telas e integração dos recursos.
- `android-app/app/src/main/cpp/`: bridge JNI e integração nativa com FluidSynth.
- `src/` e `include/`: código e cabeçalhos do motor FluidSynth.
- `android/`: arquivos de suporte à compilação Android do motor.

O motor deve permanecer isolado da interface e dos adaptadores MIDI. Sempre que possível, cada responsabilidade deve ficar em seu próprio arquivo. Alterações no motor só devem ocorrer quando houver necessidade técnica demonstrada.

## Validação e situação do projeto

O usuário confirmou que todos os testes físicos do APK de referência #258 foram aprovados, incluindo teclado, canais, SoundFonts, USB, BLE, ausência de atrasos perceptíveis, mixer, efeitos, rotação, painel responsivo e persistência.

O build de integração mais recente, [GitHub Actions #44775429211](https://github.com/rodolphofriedrich-gif/fluidsynth-android/actions/runs/44775429211), também concluiu com sucesso. Ele inclui a correção de acessibilidade dos knobs e a consolidação da versão testada.

A aprovação de um build comprova a compilação do APK; a validação física é registrada separadamente em [android-app/STABLE_BASELINE.md](android-app/STABLE_BASELINE.md).

## Documentação

- [Arquitetura atual](ARCHITECTURE.md)
- [Estado e plano de validação](android-app/ROADMAP.md)
- [Referência funcional](android-app/STABLE_BASELINE.md)
- [Entrada USB MIDI](android-app/USB_MIDI_IMPLEMENTATION.md)
- [Entrada BLE MIDI](android-app/BLE_MIDI_IMPLEMENTATION.md)
- [Como contribuir](CONTRIBUTING.md)
- [Licenças e avisos de terceiros](LICENSES.md)

## Motor de origem e documentos preservados

O motor utilizado é baseado no [FluidSynth original](https://github.com/FluidSynth/fluidsynth), um sintetizador de software multiplataforma. Essa característica pertence ao projeto upstream; o aplicativo deste repositório tem como destino exclusivamente o Android.

- [README original do FluidSynth](README.FLUIDSYNTH.md)
- [Instruções CMake do FluidSynth original](README.FLUIDSYNTH-CMAKE.md)
- [Lista de tarefas histórica do FluidSynth original](TODO.FLUIDSYNTH.md)
- [Documentação oficial do FluidSynth](https://github.com/FluidSynth/fluidsynth/wiki)
- [Documentação da API](https://www.fluidsynth.org/api/)
