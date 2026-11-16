# Entrada MIDI por USB

A implementação desta funcionalidade fica separada do motor de áudio.

## Arquivos envolvidos

- `UsbMidiInput.java`: localiza dispositivos USB MIDI pelo serviço MIDI do Android, abre as portas de saída dos controladores, interpreta mensagens MIDI de canal e encerra as conexões com segurança.
- `MainActivity.java`: gerencia o ciclo de vida do gerenciador de entrada USB e encaminha os eventos MIDI recebidos para JNI. O teclado virtual e a thread de áudio existentes permanecem preservados.
- `native_player.cpp`: recebe eventos MIDI de canal e os encaminha ao FluidSynth — Note On, Note Off, Control Change, Program Change e Pitch Bend — usando o mutex existente do sintetizador.
- `AndroidManifest.xml`: declara os recursos USB Host e MIDI como opcionais.

## Decisão de implementação e referência

A implementação considerou a arquitetura e as notas de compatibilidade do projeto [kshoji/USB-MIDI-Driver](https://github.com/kshoji/USB-MIDI-Driver), distribuído sob Apache License 2.0. O aplicativo utiliza a API `MidiManager` integrada ao Android, em vez de incorporar toda a biblioteca de terceiros. Isso evita adicionar uma pilha USB separada ou uma dependência desnecessária.

Documentação oficial do Android:

- [Visão geral de MIDI no Android](https://source.android.com/docs/core/audio/midi)
- [API MIDI do Android](https://developer.android.com/reference/android/media/midi/package-summary)

## Escopo atual

- Entrada de controladores USB MIDI, por meio das portas de saída do controlador.
- Mensagens MIDI 1.0 de voz de canal, incluindo running status.
- Note On/Off, velocity, canal, sustain/control changes, program changes e pitch bend.
- Liberação de notas ativas quando o dispositivo é removido.
- O áudio continua sendo reproduzido pela saída AudioTrack do próprio telefone.

## Compatibilidade dos dispositivos

O serviço MIDI do Android disponibiliza dispositivos USB MIDI quando a versão do Android e o kernel oferecem suporte a USB Host MIDI. Alguns dispositivos ou implementações USB específicas de fabricantes podem não ser expostos pelo serviço da plataforma.

Se um controlador físico não for detectado no aparelho de destino, avalie a integração do driver kshoji como alternativa, preservando a separação desta classe em relação ao motor de áudio.

## Validação necessária

O código deve passar pelo build do GitHub Actions e ser testado no aparelho Android de destino com um controlador MIDI USB-OTG real. Este documento não declara que um teste físico foi realizado ou aprovado.
