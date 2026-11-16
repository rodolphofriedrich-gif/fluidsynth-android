# FluidSynth Player para Android

Aplicativo Android de síntese de áudio baseado no **[FluidSynth](https://github.com/FluidSynth/fluidsynth)**, com suporte a bibliotecas SoundFont 2 (SF2) e recursos MIDI.

> Este repositório é uma adaptação/fork do projeto original FluidSynth. O aplicativo Android e suas integrações são mantidos neste repositório; o motor FluidSynth continua sendo o projeto upstream, com seus créditos e termos de licença preservados.

## Recursos

- Motor de áudio nativo FluidSynth integrado ao Android por JNI.
- Importação de arquivos SoundFont (SF2) pelo seletor de arquivos do Android.
- 16 canais/faixas MIDI independentes.
- Seleção de biblioteca SoundFont e de preset por canal, mantendo as bibliotecas separadas na navegação.
- Exibição do nome do instrumento/preset selecionado em cada canal.
- Persistência das bibliotecas e dos presets selecionados ao fechar e reabrir o aplicativo.
- Controle de polifonia.
- Teclado virtual e saída de áudio pelo próprio dispositivo Android.
- Implementações de entrada MIDI USB e BLE. A compatibilidade prática depende do dispositivo, da versão do Android e do controlador; consulte os documentos específicos antes de considerar esses recursos validados em hardware.

## Estado de validação

Na versão documentada neste repositório, foram confirmados em dispositivo Android:

- Carregamento e seleção de bibliotecas SF2.
- Troca de presets e alteração audível do timbre.
- Uso de instrumentos diferentes em canais distintos, sem interferência entre eles.
- Restauração das bibliotecas e dos instrumentos após encerrar e reabrir o aplicativo.
- Compilação Android concluída com sucesso no GitHub Actions para o commit de referência desta documentação.

A validação de recursos MIDI USB/BLE com controladores físicos deve ser considerada separadamente. Não interprete a presença do código como garantia de compatibilidade com todo dispositivo.

## Requisitos de compilação

- Android Studio ou ambiente equivalente com Gradle.
- Android SDK Platform 35.
- Android NDK 27.2.12479018.
- CMake 3.22.1.
- JDK compatível com a versão do Android Gradle Plugin utilizada pelo projeto.

Configuração atual do aplicativo:

| Item | Valor |
|---|---|
| Nome Gradle do projeto | `FluidSynthPlayer` |
| ID do aplicativo | `com.rodolphofriedrich.fluidsynth` |
| Versão | `1.0.0` (versionCode 1) |
| Android mínimo | API 23 |
| Android alvo | API 35 |
| ABIs configuradas | `arm64-v8a`, `armeabi-v7a`, `x86_64` |

## Compilar o APK

Na pasta `android-app`, execute:

```bash
gradle --no-daemon :app:assembleDebug
```

O APK de depuração é gerado em:

```text
android-app/app/build/outputs/apk/debug/app-debug.apk
```

Também é possível executar o workflow **Build Android APK** na aba **Actions** do GitHub. Quando a execução terminar com sucesso, baixe o artefato APK disponibilizado pelo workflow. Um build bem-sucedido não substitui os testes em aparelho físico.

## Documentação do projeto

- [Arquitetura e separação de responsabilidades](ARCHITECTURE.md)
- [Como contribuir](CONTRIBUTING.md)
- [Licenças e créditos de terceiros](LICENCAS_E_CREDITOS.md)
- [Política de segurança](SECURITY.md)
- [Base funcional validada](android-app/STABLE_BASELINE.md)
- [Implementação MIDI USB](android-app/USB_MIDI_IMPLEMENTATION.md)
- [Implementação MIDI Bluetooth LE](android-app/BLE_MIDI_IMPLEMENTATION.md)

## Diretriz de manutenção

A prioridade é preservar o comportamento já validado. Sempre que possível:

1. Mantenha o motor FluidSynth isolado da interface Android e das integrações MIDI.
2. Separe cada responsabilidade em seu próprio módulo/arquivo.
3. Evite alterações no motor de áudio quando a tarefa puder ser resolvida em outra camada.
4. Faça mudanças pequenas e revisáveis.
5. Execute o build Android e os testes relevantes antes de integrar alterações.
6. Não considere uma funcionalidade concluída apenas porque compila; valide também o comportamento em dispositivo quando houver dependência de hardware.

Consulte [ARCHITECTURE.md](ARCHITECTURE.md) para as regras detalhadas.

## Projeto original e créditos

O motor de síntese utilizado é o [FluidSynth](https://github.com/FluidSynth/fluidsynth), um sintetizador de software multiplataforma baseado na especificação SoundFont 2. Os créditos históricos e a relação de colaboradores do projeto original estão preservados no arquivo [AUTHORS](AUTHORS).

Este projeto não é uma distribuição oficial do FluidSynth. Para informações sobre o projeto upstream, consulte:

- https://www.fluidsynth.org/
- https://github.com/FluidSynth/fluidsynth
- https://github.com/FluidSynth/fluidsynth/wiki

## Licença

O repositório contém o arquivo [LICENSE](LICENSE), com o texto integral da **GNU Lesser General Public License versão 2.1 (LGPL-2.1)**, aplicável ao código FluidSynth distribuído neste projeto. A dependência BLE MIDI e sua biblioteca auxiliar Java são distribuídas sob Apache License 2.0.

Leia [LICENCAS_E_CREDITOS.md](LICENCAS_E_CREDITOS.md) antes de redistribuir o aplicativo, modificar o motor ou incorporar bibliotecas adicionais. Os textos legais originais são mantidos no idioma e na forma publicados pelos respectivos titulares; as explicações do projeto estão em português do Brasil.
