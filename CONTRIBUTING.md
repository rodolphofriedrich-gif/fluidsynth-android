# Como contribuir

Obrigado pelo interesse em contribuir com o FluidSynth Player para Android.

## Antes de começar

- Leia [README.md](README.md), [ARCHITECTURE.md](ARCHITECTURE.md) e [LICENSES.md](LICENSES.md).
- Confira as alterações recentes e os problemas conhecidos antes de iniciar uma mudança.
- Trabalhe em uma branch específica para cada correção ou funcionalidade.
- Não faça alterações no motor FluidSynth quando a necessidade puder ser resolvida na camada do aplicativo.

## Diretrizes de desenvolvimento

1. Mantenha cada módulo com uma responsabilidade clara.
2. Prefira arquivos e classes separados para interface, persistência, dispositivos MIDI e integração nativa.
3. Preserve o comportamento do motor de áudio e do roteamento MIDI já validado.
4. Não remova sincronização nativa ou mutex sem uma estratégia técnica segura e testável.
5. Evite dependências novas quando a API Android ou os módulos existentes forem suficientes.
6. Mantenha mensagens da interface e documentação em português do Brasil.
7. Atualize a documentação e os avisos de licença ao adicionar dependências ou recursos.

## Processo recomendado

1. Crie uma branch a partir da branch principal atualizada.
2. Faça uma alteração pequena e com escopo bem definido.
3. Revise o diff para confirmar que arquivos não relacionados não foram modificados.
4. Execute o build Android pelo GitHub Actions.
5. Teste no aparelho quando a mudança afetar áudio, MIDI, SoundFonts, permissões, rotação ou interface responsiva.
6. Descreva no pull request o que mudou, o que foi preservado e quais testes foram executados.

## Pull requests

A descrição do pull request deve informar:

- Objetivo e comportamento esperado.
- Arquivos e módulos alterados.
- Impacto potencial no motor FluidSynth, JNI, áudio e roteamento MIDI.
- Resultado do build Android.
- Testes físicos realizados e respectivos resultados.
- Dependências novas e licenças aplicáveis.

Não declare um teste físico como aprovado apenas porque o APK compilou.
