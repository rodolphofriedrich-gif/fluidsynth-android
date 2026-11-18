# Implementação de entrada BLE MIDI no Android

## Escopo

A entrada BLE MIDI utiliza a biblioteca **Kshoji BLE-MIDI-for-Android 0.0.21** para descoberta, transporte GATT e interpretação do protocolo.

- Papel BLE do aplicativo: **Central**.
- O aplicativo recebe mensagens de periféricos BLE MIDI.
- A saída BLE MIDI não é utilizada.
- A busca é habilitada pelo usuário e depende das permissões do Android.

## Arquivos e fluxo

- `BleMidiInput.java`: permissões, busca, conexão, recepção e eventos de desconexão.
- `MidiRouter.java`: recebe os eventos BLE e coordena notas/sustain por origem e canal.
- `NativeBridge.java`: interface Java com o motor nativo.
- `native_player.cpp` e `synth_engine.cpp`: bridge JNI e integração com FluidSynth.

O adaptador BLE não contém a implementação do motor nem a thread de renderização de áudio.

## Eventos MIDI encaminhados

O adaptador encaminha:

- Note On e Note Off.
- Control Change, incluindo sustain.
- Program Change.
- Pitch Bend.
- Polyphonic Aftertouch.
- Channel Aftertouch.

SysEx e mensagens de sistema não implementadas pelo despachante nativo não são encaminhadas ao motor.

O agendamento BLE utiliza a configuração de baixa latência da biblioteca com limite de antecipação de 40 ms. Isso não elimina a latência de rádio, do sistema operacional ou do controlador.

## Permissões

- Android 12 ou superior: `BLUETOOTH_SCAN` e `BLUETOOTH_CONNECT`.
- Android 6 a 11: permissão de localização precisa para realizar a busca BLE.

O aparelho precisa oferecer Bluetooth LE e o controlador precisa implementar o perfil BLE MIDI.

## Dependências e referências

- Dependência: `jp.kshoji:ble-midi:0.0.21`.
- [BLE-MIDI-for-Android](https://github.com/kshoji/BLE-MIDI-for-Android).
- [Versão 0.0.21](https://github.com/kshoji/BLE-MIDI-for-Android/releases/tag/v0.0.21).
- [API MIDI do Android](https://source.android.com/docs/core/audio/midi).

A licença upstream da biblioteca é Apache License 2.0; consulte [LICENSES.md](../LICENSES.md).

## Validação

A compilação automatizada deve passar a cada alteração. A compatibilidade real exige testes com controladores BLE físicos: descoberta, conexão, notas, velocity, CC/sustain, Aftertouch, desconexão e reconexão. Build aprovado não comprova compatibilidade com todos os controladores.
