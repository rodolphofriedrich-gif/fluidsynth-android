# FluidSynth Android — base estável

**Marco:** motor de áudio nativo validado  
**Branch:** `android-apk-player`  
**Commit-base validado:** `798c93a11be624c267e8df55f9ba8cffd6cca41f`

## Estado validado em dispositivo Android

O usuário confirmou os seguintes testes:

- Reprodução sem atraso perceptível.
- Pressionamento e soltura rápida de notas.
- Reprodução de várias notas simultâneas.
- Troca de SoundFont sem fechar o aplicativo.
- Fechamento e reabertura do aplicativo.

## Arquitetura preservada

- FluidSynth nativo integrado por JNI.
- Saída de áudio Android por `AudioTrack`.
- Renderização em thread dedicada com prioridade de áudio.
- Sincronização do acesso ao sintetizador e encerramento nativo.
- Importação de SoundFont pelo seletor de arquivos Android.

## Regra para próximas etapas

Esta versão é a referência funcional. Não modificar o motor de áudio durante a implementação de novas funções, salvo se um teste demonstrar necessidade.

## Próxima etapa planejada: controlador MIDI USB

Adicionar entrada MIDI por USB para que um controlador físico envie eventos MIDI ao FluidSynth e o aplicativo reproduza o som pelo próprio dispositivo Android.

Critérios mínimos para a próxima etapa:

1. Detectar conexão e desconexão de controladores MIDI USB.
2. Solicitar e gerenciar a permissão USB do Android.
3. Receber Note On, Note Off, velocity e canal MIDI.
4. Encaminhar os eventos ao FluidSynth nativo sem bloquear a thread de áudio.
5. Tratar desconexão com notas presas, enviando encerramento seguro das notas ativas.
6. Manter o teclado virtual e a reprodução atual funcionando.
7. Testar com controlador físico e cabo/adaptador USB OTG.

**Importante:** a conexão USB será uma entrada MIDI para tocar o sintetizador interno. O áudio continuará saindo pelo Android; não se trata de transmitir áudio pelo USB.
