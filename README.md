# FluidSynth Player para Android

Aplicativo de sintetizador de áudio para **Android**, baseado no motor nativo FluidSynth e compatível com bibliotecas SoundFont 2 (SF2).

> **Plataforma do aplicativo: somente Android.** Este projeto não oferece versões do aplicativo para Windows, Linux, macOS, iOS ou outros sistemas. O fato de o código-fonte original do FluidSynth ser multiplataforma não significa que este aplicativo também seja.

O repositório mantém parte do código-fonte e dos arquivos de referência do FluidSynth original para compilar o motor utilizado pelo aplicativo. Os documentos com o nome `FLUIDSYNTH` descrevem esse projeto de origem, não outros destinos de distribuição do aplicativo.

## Recursos da versão em desenvolvimento

- Motor FluidSynth nativo integrado ao Android por JNI.
- Importação de bibliotecas SoundFont no formato SF2, com validação inicial do arquivo e indicação do progresso da cópia.
- Até 16 canais MIDI independentes, cada um com seleção própria de SoundFont e preset.
- Persistência das bibliotecas importadas, dos instrumentos selecionados e do canal ativo.
- Ativação e desativação de bibliotecas sem interromper os canais que já as utilizam.
- Controle de ativação/silenciamento por canal.
- Teclado virtual com Velocity ajustável de 1 a 127 (padrão 100).
- Controle ajustável de polifonia.
- Entrada MIDI por USB e por dispositivos virtuais disponibilizados pelo serviço MIDI do Android.
- Entrada BLE MIDI por controladores compatíveis.
- Roteamento interno comum para teclado virtual, USB e BLE, com rastreamento de notas e sustain por origem.
- Tratamento de desconexões e de comandos MIDI de limpeza de notas.
- Recepção BLE de Polyphonic Aftertouch e Channel Aftertouch.
- Atualização e persistência do canal ao receber Program Change externo.
- Saída de áudio estéreo pelo Android usando `AudioTrack`.

USB MIDI e BLE MIDI são entradas para o aplicativo. O projeto não deve ser descrito como uma versão Windows/Linux/macOS nem como um roteador de saída MIDI para instrumentos externos.

## Requisitos Android

- Android 6.0 (API 23) ou superior.
- Dispositivo com suporte à saída de áudio PCM float estéreo em 48 kHz.
- Para MIDI USB: aparelho com USB host/OTG e controlador reconhecido pelo serviço MIDI do Android.
- Para BLE MIDI: Bluetooth LE e controlador compatível com o perfil BLE MIDI.
- Android 12 ou superior: permissões Bluetooth Scan e Connect.
- Android 6 a 11: permissão de localização precisa para a busca BLE.

USB e BLE são recursos opcionais. A compatibilidade depende do aparelho, da versão do Android, do controlador e dos serviços de sistema disponíveis. O aplicativo solicita modo de baixa latência quando o Android oferece suporte, mas isso não garante a mesma latência em todos os dispositivos.

## Gerar o APK de teste

O fluxo de compilação do aplicativo é **Build Android APK**, executado pelo GitHub Actions. O runner pode usar Ubuntu como ambiente de compilação; o artefato produzido continua sendo um APK Android.

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

Inicialize os submódulos do repositório:

```bash
git submodule update --init --recursive
```

Com as ferramentas instaladas, execute na raiz do repositório:

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
- `android-app/app/src/main/cpp/`: bridge JNI e integração nativa com o FluidSynth.
- `src/` e `include/`: código e cabeçalhos do motor FluidSynth.
- `android/`: arquivos de suporte à compilação Android do motor.
- `ARCHITECTURE.md`: arquitetura implementada e limites conhecidos.

O motor deve permanecer isolado da interface e dos adaptadores MIDI. Sempre que possível, cada responsabilidade deve ficar em seu próprio arquivo. Alterações no motor só devem ocorrer quando houver necessidade técnica demonstrada.

## Validação e situação do projeto

Uma compilação bem-sucedida comprova que o APK foi gerado, mas não substitui os testes físicos de áudio, USB, BLE e controladores. O estado dos builds e a lista de testes pendentes estão em [android-app/ROADMAP.md](android-app/ROADMAP.md) e [android-app/STABLE_BASELINE.md](android-app/STABLE_BASELINE.md).

O aplicativo permanece em desenvolvimento. Não declare compatibilidade universal nem prontidão para distribuição comercial antes da conclusão dos testes físicos e da revisão dos requisitos de licenciamento.

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
