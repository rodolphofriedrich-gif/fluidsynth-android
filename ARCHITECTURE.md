# Arquitetura do FluidSynth Player

## Princípio obrigatório

Sempre que tecnicamente possível, cada módulo deve ter uma responsabilidade clara e ficar em arquivo próprio. Evitar arquivos que concentrem interface, regras de negócio, acesso a dispositivos e motor de áudio.

Uma alteração em uma camada não deve exigir mudanças nas demais, salvo por contratos/interfaces explícitos.

## Camadas e dependências

```
Interface (Activity / Views / Resources)
              |
              v
Coordenação da aplicação (ViewModels ou Controllers)
              |
       interfaces/contratos
       /       |        \
      v        v         v
  MIDI I/O  SoundFonts  Faixas/Estado
      \        |         /
       \       v        /
        Motor FluidSynth
              |
       Saída de áudio
```

Regra de dependência: o motor não conhece Android Views, Activity, XML, USB, BLE ou nomes de componentes visuais. A interface não chama funções JNI diretamente; deve usar um serviço/controlador de aplicação.

## Responsabilidades pretendidas

| Módulo | Responsabilidade |
|---|---|
| `engine/` (C++) | Instância FluidSynth, canais, SoundFonts, notas, CC e renderização |
| `audio/` | AudioTrack, thread de áudio, buffers e ciclo de vida da saída |
| `midi/usb/` | Descoberta, seleção e recepção de MIDI USB/virtual |
| `midi/ble/` | Permissões, busca, conexão e recepção BLE MIDI |
| `soundfont/` | Seleção, importação, validação, armazenamento e deduplicação de SF2 |
| `tracks/` | Estado das 16 faixas e canal selecionado |
| `ui/activities/` | Ciclo de vida Android e navegação |
| `ui/views/` | Componentes visuais reutilizáveis |
| `res/layout/` | Estrutura XML das telas |
| `res/values/` | Cores, dimensões, strings e temas |

## Regras para preservar estabilidade

1. Não reescrever o motor de áudio estável durante refatorações visuais.
2. Não misturar callbacks de UI com processamento de áudio.
3. Não permitir que USB/BLE controlem diretamente detalhes internos do FluidSynth; ambos devem usar uma interface MIDI comum.
4. Não deixar a UI acessar JNI diretamente após a extração do serviço de áudio/motor.
5. Cada módulo deve expor uma API pequena e explícita.
6. Alterações estruturais devem ser feitas em etapas pequenas, com build Android obrigatório em cada etapa.
7. Testar regressões de importação SF2, teclado, 16 canais, mute, USB e BLE após as respectivas mudanças.
8. Não fazer merge das branches de funcionalidades sem build verde e testes adequados.

## Plano de refatoração

- [x] Separar o núcleo C++ do bridge JNI em arquivos próprios.
- [x] Separar estado das faixas em `Track` e `TrackManager`.
- [x] Separar importação/deduplicação de SF2 em `SoundFontManager`.
- [x] Separar entrada USB MIDI e BLE MIDI em classes próprias.
- [ ] Extrair a saída AudioTrack e sua thread para o módulo `audio/`.
- [ ] Criar uma fachada/serviço de aplicação para o motor e remover chamadas JNI diretas da Activity.
- [ ] Mover a estrutura da tela de Java para XML em `res/layout/`.
- [ ] Separar adaptador/visualização da grade de 16 faixas.
- [ ] Separar teclado virtual em componente próprio.
- [ ] Centralizar cores, dimensões, textos e tema em recursos Android.
- [ ] Definir contratos comuns para entradas MIDI USB, BLE e futuras entradas.
- [ ] Adicionar testes automatizados para regras de faixas e roteamento MIDI.

## Estado atual

A interface principal ainda é montada programaticamente em `MainActivity.java`, e a Activity ainda coordena diretamente partes do ciclo de áudio e chamadas JNI. Portanto, a separação está incompleta. A migração deve preservar os comportamentos já testados e ocorrer por etapas, sem alterar a branch estável `android-apk-player`.
