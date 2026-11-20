# Implementação de entrada USB MIDI no Android

## Escopo

Este módulo implementa **entrada** MIDI de controladores USB e de dispositivos virtuais disponibilizados pelo serviço MIDI do Android. O controlador envia mensagens ao aplicativo; a saída de áudio do sintetizador continua sendo produzida pelo Android por meio de `AudioTrack`.

Não se trata de uma implementação de saída MIDI para enviar dados do aplicativo a instrumentos externos.

## Arquivos e fluxo

- `UsbMidiInput.java`: enumera dispositivos pelo serviço MIDI do Android, abre a porta de saída do controlador (porta pela qual ele envia dados) e acompanha conexão/desconexão.
- `MidiRouter.java`: recebe os eventos do adaptador USB junto com os eventos das demais origens e controla notas/sustain por origem e canal.
- `NativeBridge.java`: encaminha as operações MIDI ao motor nativo.
- `native_player.cpp` e `synth_engine.cpp`: bridge JNI e integração com FluidSynth.

A Activity coordena os módulos, mas o adaptador USB não chama diretamente as funções JNI nem implementa o motor de áudio.

## Mensagens e comportamento

- O parser trata o fluxo MIDI, incluindo running status e mensagens fragmentadas entre pacotes.
- As mensagens de canal suportadas pelo caminho USB são encaminhadas ao roteador MIDI e ao motor.
- A desconexão da origem solicita a liberação das notas e do sustain que ela mantinha ativos.
- O aplicativo abre o dispositivo selecionado; a presença de um dispositivo na lista não significa que ele esteja conectado como entrada ativa.

## Compatibilidade

O Android precisa disponibilizar o dispositivo pelo serviço `MidiManager`. Para controladores USB, o aparelho normalmente precisa oferecer USB host/OTG. Alguns controladores ou implementações específicas de fabricantes podem não ser reconhecidos pelo serviço MIDI da plataforma.

A funcionalidade depende do modelo do aparelho, da versão do Android, do cabo/adaptador OTG e do controlador.

## Referências

- [Visão geral de MIDI no Android](https://source.android.com/docs/core/audio/midi)
- [API MIDI do Android](https://developer.android.com/reference/android/media/midi/package-summary)
- [Projeto de referência USB-MIDI-Driver](https://github.com/kshoji/USB-MIDI-Driver) — referência de arquitetura; o aplicativo utiliza a API MIDI integrada ao Android, não incorpora esse driver completo.

## Validação

A compilação Android deve passar no GitHub Actions. Também é necessário testar no aparelho de destino com um controlador físico: conexão, seleção, notas, velocity, CC/sustain, desconexão e reconexão. Build aprovado não comprova compatibilidade universal.
