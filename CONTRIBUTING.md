# Como contribuir

Obrigado pelo interesse em contribuir com o FluidSynth Player para Android.

## Antes de começar

- Leia [README.md](README.md), [ARCHITECTURE.md](ARCHITECTURE.md) e [LICENSES.md](LICENSES.md).
- Confira as alterações recentes e os problemas conhecidos antes de iniciar uma mudança.
- Trabalhe em uma branch específica para cada correção ou funcionalidade.
- Não faça alterações no motor FluidSynth quando a necessidade puder ser resolvida na camada do aplicativo.

## Princípios obrigatórios

1. Mantenha cada módulo com uma responsabilidade clara.
2. Preserve o comportamento já validado do aplicativo.
3. Evite misturar interface, regras de negócio, MIDI, SoundFonts e motor de áudio.
4. Não altere o motor de áudio por conveniência; faça isso somente quando houver necessidade técnica demonstrada.
5. Faça mudanças pequenas, isoladas e fáceis de revisar.
6. Não inclua arquivos gerados, chaves, credenciais ou bibliotecas SoundFont protegidas por direitos autorais.
7. Não declare testes físicos que não tenham sido executados.

## Processo recomendado

1. Crie uma branch a partir da base de desenvolvimento atual.
2. Faça uma alteração por vez, mantendo o escopo limitado.
3. Execute a compilação Android pelo GitHub Actions.
4. Para recursos que dependam de hardware, teste também em um aparelho real e registre o modelo do dispositivo, o controlador e o resultado.
5. Descreva no pull request o motivo da mudança, os arquivos afetados, os testes realizados e eventuais limitações.
6. Aguarde a revisão e a confirmação antes de incorporar mudanças à branch principal de trabalho.

## Critérios para aceitar alterações

- Compilação Android concluída com sucesso.
- Nenhuma regressão nos recursos já existentes.
- Testes pertinentes documentados.
- Respeito às licenças e aos avisos de terceiros.
- Código organizado em módulos com responsabilidades bem definidas.

## Contribuições ao motor FluidSynth

O código do motor tem origem no projeto [FluidSynth](https://github.com/FluidSynth/fluidsynth). Alterações destinadas a serem incorporadas ao projeto original devem também respeitar as orientações e o processo de contribuição mantidos pelos desenvolvedores upstream.
