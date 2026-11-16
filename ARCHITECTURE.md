# Arquitetura do FluidSynth Player para Android

## Princípio obrigatório

Sempre que tecnicamente possível, cada módulo deve ter uma responsabilidade clara e ficar em arquivo próprio. Evite arquivos que concentrem interface, regras de negócio, acesso a dispositivos e motor de áudio.

Uma alteração em uma camada não deve exigir mudanças nas demais, salvo por contratos e interfaces explícitos.

## Camadas e dependências

```text
Interface Android (Activity / Views / Resources)
                    |
                    v
Coordenação da aplicação
                    |
             interfaces/contratos
          /          |           \
         v           v            v
     Entrada MIDI  SoundFonts   Faixas/Estado
          \          |            /
           \         v           /
              Motor FluidSynth
                    |
              Saída de áudio
```

**Regra de dependência:** o motor não deve conhecer Android Views, Activity, XML, USB, BLE ou nomes de componentes visuais. As integrações MIDI devem encaminhar eventos por uma fronteira explícita; não devem implementar regras internas do sintetizador.

## Responsabilidades

| Módulo | Responsabilidade |
|---|---|
| Núcleo FluidSynth (C/C++) | Instância do sintetizador, canais, SoundFonts, notas, controladores e renderização |
| JNI | Ponte entre o código Android e o núcleo nativo |
| Áudio Android | Saída por AudioTrack, thread de áudio, buffers e ciclo de vida |
| Entrada MIDI USB | Descoberta, permissões, recepção e encerramento de dispositivos USB MIDI |
| Entrada MIDI BLE | Busca, conexão, permissões e recepção BLE MIDI |
| SoundFontManager | Importação, identificação, armazenamento e deduplicação de arquivos SF2 |
| Track / TrackManager | Estado dos 16 canais/faixas, bibliotecas e presets selecionados |
| MainActivity / interface | Ciclo de vida, navegação e interação com o usuário |

Os nomes acima descrevem responsabilidades; não significam que todas já estejam extraídas em diretórios independentes. A separação estrutural ainda é incremental.

## Regras para preservar a estabilidade

1. Não reescreva o motor de áudio estável durante refatorações da interface.
2. Não misture callbacks da interface com processamento de áudio.
3. Não permita que USB/BLE controlem diretamente detalhes internos do FluidSynth.
4. Mantenha as chamadas JNI concentradas na fronteira nativa existente; evite espalhá-las pela interface.
5. Cada módulo deve expor uma API pequena e explícita.
6. Faça alterações estruturais em etapas pequenas, com build Android obrigatório em cada etapa.
7. Teste regressões de importação SF2, teclado, 16 canais, seleção independente de presets e persistência.
8. Teste USB e BLE com hardware real quando a alteração envolver essas integrações.
9. Não integre alterações sem build verde e validação adequada ao risco da mudança.

## Estado conhecido da implementação

- O núcleo nativo FluidSynth e a ponte JNI estão em arquivos separados.
- O estado das faixas está separado em `Track` e `TrackManager`.
- A importação e a identificação das bibliotecas SF2 estão concentradas em `SoundFontManager`.
- As entradas USB MIDI e BLE MIDI possuem classes próprias.
- A interface principal ainda coordena partes do ciclo de áudio e algumas chamadas JNI.
- A extração completa da saída de áudio e de um serviço/fachada de aplicação continua sendo uma possibilidade de evolução, não uma justificativa para refatorar o que já funciona.

## Critérios mínimos de regressão

Para mudanças que possam afetar o sintetizador ou os canais:

- Importar e carregar uma biblioteca SF2.
- Selecionar presets diferentes em um mesmo canal e confirmar a mudança de timbre.
- Selecionar presets diferentes em canais distintos e confirmar independência.
- Encerrar e reabrir o aplicativo e confirmar a restauração de biblioteca e preset.
- Tocar e soltar notas, inclusive em sequência rápida e com notas simultâneas.
- Confirmar que a saída de áudio continua funcionando.

Para alterações em USB/BLE, acrescente testes com o controlador físico correspondente.

## Regra de integração

O build verde é obrigatório, mas não substitui a validação funcional. Mantenha as alterações em branches e pull requests próprios, com escopo claro. Não faça merge na branch principal sem revisão e autorização explícita do responsável pelo projeto.
