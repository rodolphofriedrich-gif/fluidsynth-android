# Política de segurança

## Versões acompanhadas

A manutenção de segurança acompanha a branch de desenvolvimento ativa e as versões publicadas pelo responsável pelo projeto. Builds de depuração e branches experimentais podem conter alterações ainda não validadas.

## Como comunicar uma vulnerabilidade

Não publique detalhes de uma vulnerabilidade explorável em uma issue pública.

Use o recurso **Private vulnerability reporting** na página **Security** deste repositório, caso esteja habilitado. Se o recurso não estiver disponível, entre em contato com o mantenedor por um canal privado antes de divulgar os detalhes.

Ao comunicar um problema, inclua, quando possível:

- versão do aplicativo e versão do Android;
- modelo do dispositivo;
- etapas para reproduzir;
- impacto observado;
- logs sem senhas, tokens, dados pessoais ou arquivos privados.

## Escopo

São especialmente relevantes problemas envolvendo:

- carregamento e análise de arquivos SF2;
- memória nativa e interface JNI;
- permissões e acesso a arquivos;
- conexões MIDI USB e Bluetooth LE;
- dependências de terceiros e processo de compilação.

## Divulgação responsável

Solicitamos que o mantenedor tenha oportunidade razoável de analisar e corrigir o problema antes da divulgação pública. Não envie arquivos SoundFont protegidos por direitos autorais, credenciais ou dados pessoais em relatos de segurança.
