# Regras permanentes de layout

Estas regras fazem parte dos requisitos de produto e devem ser preservadas em toda refatoração de interface, navegação ou responsividade.

## Painel de configuração/seleção de SoundFont

O seletor de SoundFont associado ao canal MIDI deve seguir sempre esta regra:

- **Celular em retrato:** abrir como tela cheia.
- **Celular em paisagem:** abrir como painel lateral retrátil à direita, mantendo a tela MIDI visível ao fundo.
- **Tablet, em qualquer orientação:** abrir como painel lateral retrátil à direita, mantendo a tela principal visível.
- O painel deve permitir rolagem própria, fechar/cancelar e retornar ao contexto do canal selecionado.
- Não substituir esse comportamento por bottom sheet ou diálogo centralizado.

## Responsividade

- A navegação inferior permanece fixa.
- Conteúdos roláveis reservam espaço inferior suficiente para que o último item fique totalmente visível.
- Em tablets, aproveitar a largura com divisão proporcional, sem sobrepor a lista MIDI e os controles de efeitos.
- Em telefones, evitar controles excessivamente largos e preservar áreas de toque acessíveis.

## Controles MIDI

- Todos os controles contínuos de CC na página MIDI usam knobs no padrão visual dos knobs de Reverb/Chorus.
- Preservar os valores por canal, envio MIDI, persistência e liberação de Sustain ao trocar de canal ou sair da página.
