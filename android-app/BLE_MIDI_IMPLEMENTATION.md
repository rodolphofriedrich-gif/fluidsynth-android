# Entrada MIDI por Bluetooth Low Energy (BLE)

## Implementação

A entrada BLE utiliza a biblioteca **Kshoji BLE-MIDI-for-Android 0.0.21** para transporte e implementação do protocolo.

- **Papel BLE:** Central; o aplicativo recebe MIDI de periféricos BLE MIDI.
- **Biblioteca:** `jp.kshoji:ble-midi:0.0.21`, obtida do repositório Maven upstream da Kshoji.
- **Licença:** Apache License 2.0.
- **Adaptador:** `BleMidiInput.java`; encaminha Note On/Off, Control Change, Program Change e Pitch Bend para a entrada JNI existente.
- **Temporização MIDI:** configurada em `LOW_LATENCY`, com limite de agendamento antecipado de 40 ms. Isso evita o acúmulo sem limite que pode ocorrer no modo padrão `SCHEDULED` da biblioteca.
- O motor FluidSynth, a thread nativa de renderização de áudio, a implementação USB MIDI, o carregamento de SoundFonts e o teclado virtual permanecem separados dessa camada.
- Android 12 ou superior solicita permissões de busca e conexão Bluetooth; Android 6 a 11 solicita localização precisa para a busca BLE.
- O aplicativo não utiliza o fluxo de configuração Companion Device; a busca ocorre pelo scanner BLE.

## Repositórios das dependências

As configurações Gradle incluem o repositório Maven upstream da biblioteca BLE MIDI e o repositório complementar de `javax.sound.midi-for-Android`, necessário pelos metadados AAR publicados pela biblioteca. A dependência BLE está fixada na versão 0.0.21, em vez de acompanhar uma versão de desenvolvimento variável.

As duas bibliotecas são distribuídas sob Apache License 2.0. O texto integral está em [licenses/Apache-2.0.txt](licenses/Apache-2.0.txt).

## Estado de validação

- A integração está presente no código e o build Android do commit de referência foi concluído com sucesso no GitHub Actions.
- A validação física com controlador BLE deve ser feita separadamente. Este documento não declara aprovação de teste físico.
- Antes de considerar a compatibilidade BLE validada em um aparelho, teste Note On/Off, velocity, acordes, sustain/CC, notas repetidas, desconexão com notas pressionadas e reconexão.
- O rádio BLE e o agendamento de pacotes podem adicionar latência; avalie esse atraso separadamente da renderização do FluidSynth.
- O despachante MIDI nativo do FluidSynth recebe Note On/Off, CC, Program Change e Pitch Bend. SysEx, aftertouch e mensagens de tempo real do sistema não são encaminhados, pois o despachante nativo atual não possui manipuladores para esses eventos.

## Referências upstream

- https://github.com/kshoji/BLE-MIDI-for-Android
- https://github.com/kshoji/BLE-MIDI-for-Android/releases/tag/v0.0.21
- https://github.com/kshoji/BLE-MIDI-for-Android/blob/develop/README.md
- https://source.android.com/docs/core/audio/midi
