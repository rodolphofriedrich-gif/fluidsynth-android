# Licenças e avisos de terceiros

Este repositório reúne o motor FluidSynth e uma implementação de aplicativo Android. Os componentes não devem ser tratados como se tivessem necessariamente uma única licença.

Este documento é um inventário informativo, não substitui os textos legais originais nem constitui aconselhamento jurídico.

## 1. FluidSynth

O código do motor FluidSynth está licenciado sob a **GNU Lesser General Public License, versão 2.1 (LGPL-2.1)**, conforme o arquivo [LICENSE](LICENSE).

- Texto oficial: https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
- Perguntas frequentes de licenciamento: https://github.com/FluidSynth/fluidsynth/wiki/LicensingFAQ
- Projeto original: https://github.com/FluidSynth/fluidsynth

O arquivo [LICENSE](LICENSE) foi mantido em sua forma original. Não o traduza nem o substitua por um resumo: em caso de dúvida, prevalece o texto oficial da licença.

A distribuição de um aplicativo que utiliza ou incorpora o FluidSynth deve cumprir as condições aplicáveis da LGPL-2.1. Conforme a forma de vinculação e distribuição, isso pode envolver disponibilização do código-fonte correspondente, identificação das modificações, preservação dos avisos e meios para que o destinatário substitua ou recompile e vincule novamente a biblioteca. Verifique a situação concreta da compilação Android antes de distribuir o aplicativo, especialmente em versões comerciais.

## 2. Bibliotecas BLE MIDI para Android

A implementação utiliza a dependência direta:

- Pacote: `jp.kshoji:ble-midi:0.0.21`
- Projeto: https://github.com/kshoji/BLE-MIDI-for-Android
- Licença upstream: Apache License 2.0

O projeto também publica a biblioteca complementar javax.sound.midi-for-Android, utilizada como dependência associada aos metadados AAR:

- Projeto: https://github.com/kshoji/javax.sound.midi-for-Android
- Licença upstream: Apache License 2.0

O texto integral da licença está preservado em [LICENSES/Apache-2.0.txt](LICENSES/Apache-2.0.txt). Os avisos de copyright e demais avisos distribuídos pelos projetos upstream devem ser mantidos.

## 3. APIs e ferramentas Android

O aplicativo utiliza APIs do Android e ferramentas de compilação, incluindo Android SDK, NDK, Gradle, Android Gradle Plugin e CMake. Cada ferramenta possui seus próprios termos de uso e licença. A utilização das APIs do Android não transfere a licença do FluidSynth para o Android SDK.

## 4. Código específico do aplicativo

Os arquivos da implementação Android e as alterações locais deste repositório não devem ser presumidos como cobertos automaticamente pela LGPL apenas porque o motor FluidSynth está sob LGPL. Este repositório ainda precisa de uma declaração explícita e revisada sobre a licença do código original do aplicativo antes de uma distribuição pública ou comercial.

Não foi adicionada uma licença permissiva ao código do aplicativo por suposição.

## 5. Fontes SoundFont (SF2)

O aplicativo permite que o usuário importe bibliotecas SF2. Essas bibliotecas são conteúdo fornecido pelo usuário e podem ter licenças próprias. A licença do FluidSynth não concede direitos de redistribuição sobre os arquivos SF2.

Distribua somente SoundFonts cuja licença permita expressamente a redistribuição pretendida.

## 6. Inventário de dependências

Este inventário identifica as dependências diretas visíveis na configuração atual e as bibliotecas Kshoji associadas. Antes de publicar ou comercializar o APK, gere e revise também o relatório completo de dependências transitivas do Gradle e verifique os avisos de licença de cada artefato incluído.

Atualize este arquivo sempre que uma nova dependência de terceiros for adicionada ou removida.
