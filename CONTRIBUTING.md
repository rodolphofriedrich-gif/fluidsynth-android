# Como contribuir

Obrigado por considerar contribuir com este projeto. Este repositório reúne a adaptação Android e o código do FluidSynth utilizado como motor. Para mudanças no motor original, consulte também as orientações do projeto upstream: https://github.com/FluidSynth/fluidsynth.

## Antes de começar

1. Confira as issues e os pull requests abertos para evitar trabalho duplicado.
2. Explique o problema que pretende resolver e o comportamento esperado.
3. Para mudanças grandes, proponha primeiro a solução em uma issue.
4. Não altere o motor de áudio para resolver um problema que pertença à interface, à persistência ou à integração MIDI.

## Regras de arquitetura

- Mantenha cada responsabilidade em módulo/arquivo próprio sempre que viável.
- Preserve as interfaces existentes entre Java, JNI e C/C++.
- Evite misturar interface, armazenamento, comunicação MIDI e processamento de áudio.
- Não remova recursos existentes sem discussão e autorização.
- Prefira mudanças pequenas, isoladas e fáceis de revisar.

## Fluxo recomendado

1. Crie uma branch a partir da branch de trabalho indicada na issue.
2. Faça commits pequenos, com mensagens objetivas.
3. Atualize a documentação quando o comportamento, a arquitetura ou os requisitos mudarem.
4. Execute o build Android.
5. Faça testes funcionais no aparelho sempre que a alteração afetar áudio, SoundFonts, MIDI, permissões ou ciclo de vida.
6. Abra um pull request descrevendo o problema, a solução, os arquivos alterados, os testes executados e eventuais limitações.

## Verificações obrigatórias conforme a mudança

| Área alterada | Verificações |
|---|---|
| Motor / JNI | Compilação nativa, inicialização, notas, encerramento e ausência de regressões de áudio |
| SoundFonts | Importação, carregamento, troca de biblioteca, listagem e seleção de presets |
| Canais/faixas | Independência entre canais e restauração após reabrir o app |
| Áudio | Notas rápidas, acordes, encerramento de notas e reprodução contínua |
| USB MIDI | Permissões, conexão, eventos Note On/Off e desconexão com notas ativas |
| BLE MIDI | Busca, conexão, eventos MIDI, desconexão e reconexão |
| Interface | Navegação, estados vazios, mensagens de erro e telas em diferentes tamanhos |

## Descrição do pull request

Inclua:

- **Motivação:** qual problema está sendo resolvido?
- **Solução:** o que foi alterado e por quê?
- **Arquivos:** quais módulos foram modificados?
- **Testes:** quais comandos e testes físicos foram executados?
- **Riscos:** existe impacto possível no motor, na latência, nos presets ou na persistência?

Não declare um teste físico como aprovado se ele não tiver sido executado.

## Licenças e créditos

Ao adicionar código ou dependências de terceiros:

- Verifique a licença e as condições de redistribuição.
- Preserve avisos de copyright e arquivos de licença exigidos.
- Registre a dependência e sua versão em [LICENCAS_E_CREDITOS.md](LICENCAS_E_CREDITOS.md).
- Não copie código sem identificar sua origem e compatibilidade de licença.

Consulte também [ARCHITECTURE.md](ARCHITECTURE.md) e [SECURITY.md](SECURITY.md).
