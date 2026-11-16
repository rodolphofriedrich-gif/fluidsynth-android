# Licenças e créditos

Este documento resume as licenças identificadas na árvore de código e nas dependências declaradas do projeto Android. Ele não substitui os textos integrais das licenças nem constitui aconselhamento jurídico.

## 1. FluidSynth

- Projeto original: [FluidSynth](https://github.com/FluidSynth/fluidsynth).
- Descrição: sintetizador de software multiplataforma baseado na especificação SoundFont 2.
- Licença indicada no arquivo [LICENSE](LICENSE): **GNU Lesser General Public License, versão 2.1 (LGPL-2.1)**.
- O texto integral original está preservado no arquivo `LICENSE`, sem tradução ou alterações.
- Os créditos e colaboradores do projeto original estão preservados em [AUTHORS](AUTHORS).

A LGPL-2.1 possui condições próprias para cópia, modificação, distribuição e combinação com outros programas. Antes de distribuir APKs ou versões modificadas, verifique especialmente as obrigações relativas à disponibilização do código-fonte correspondente, avisos legais e possibilidade de substituição/religação da biblioteca, conforme a forma de integração utilizada.

Referência oficial: https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html

## 2. Dependências Android de MIDI

A configuração Gradle declara:

| Dependência | Versão declarada | Licença identificada | Origem |
|---|---:|---|---|
| `jp.kshoji:ble-midi` | `0.0.21` | Apache License 2.0 | [BLE-MIDI-for-Android](https://github.com/kshoji/BLE-MIDI-for-Android) |
| `javax.sound.midi-for-Android` | Dependência transitiva da biblioteca BLE MIDI; versão resolvida pelo metadado publicado | Apache License 2.0 | [javax.sound.midi-for-Android](https://github.com/kshoji/javax.sound.midi-for-Android) |

Os dois projetos upstream identificam suas licenças como Apache License 2.0. O texto integral dessa licença foi incluído em [android-app/licenses/Apache-2.0.txt](android-app/licenses/Apache-2.0.txt). Os avisos e créditos específicos de cada biblioteca devem ser preservados conforme as respectivas distribuições.

- https://github.com/kshoji/BLE-MIDI-for-Android/blob/develop/LICENSE
- https://github.com/kshoji/javax.sound.midi-for-Android

## 3. APIs do Android

O aplicativo utiliza APIs fornecidas pelo Android SDK, incluindo áudio, arquivos e MIDI. Essas APIs são componentes da plataforma Android; não são código incorporado do FluidSynth. Consulte os termos e avisos da distribuição Android/SDK aplicável.

## 4. Arquivos SoundFont (SF2)

O aplicativo permite que o usuário importe bibliotecas SoundFont. Este repositório não concede licença sobre arquivos SF2 de terceiros nem inclui autorização para redistribuí-los.

O usuário ou distribuidor deve verificar os direitos autorais e os termos de uso de cada SoundFont, especialmente antes de incluí-lo em um APK, pacote de instalação, demonstração ou distribuição comercial.

## 5. Créditos e atribuição

- **FluidSynth:** projeto original e seus colaboradores, relacionados no arquivo [AUTHORS](AUTHORS).
- **BLE MIDI for Android:** Kaoru Shoji (kshoji) e colaboradores, conforme os créditos do projeto upstream.
- **javax.sound.midi for Android:** Kaoru Shoji (kshoji) e colaboradores, conforme os créditos do projeto upstream.

## 6. Manutenção deste inventário

Este inventário foi preparado a partir do arquivo LICENSE do repositório, das dependências declaradas em `android-app/app/build.gradle` e `android-app/settings.gradle`, e dos arquivos de licença dos projetos upstream.

Quando uma dependência for adicionada, removida ou atualizada:

1. Verifique a licença da versão efetivamente utilizada.
2. Verifique também as dependências transitivas.
3. Preserve os arquivos de licença, avisos de copyright e atribuições exigidos.
4. Atualize este documento e, quando necessário, inclua os textos integrais em uma pasta de avisos de terceiros.

**Importante:** a presença da licença Apache 2.0 de uma dependência não altera a licença do FluidSynth. Da mesma forma, a LGPL do FluidSynth não concede direitos sobre SoundFonts importados pelo usuário.
