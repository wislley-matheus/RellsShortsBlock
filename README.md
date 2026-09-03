# Bloqueador de Reels/Shorts

App Android (Kotlin) que bloqueia automaticamente a aba **Reels** do Instagram
e a aba/tela de **Shorts** do YouTube, usando um `AccessibilityService`.

## Como funciona

O Instagram e o YouTube não oferecem nenhuma configuração oficial para
desativar Reels/Shorts. A única forma de bloquear isso de dentro de um app
próprio é usando a **API de Acessibilidade do Android**: o serviço observa a
árvore de elementos da tela e, quando reconhece que a tela atual é a de
Reels/Shorts, aperta "voltar" automaticamente — quase instantaneamente,
antes que dê tempo de assistir ao conteúdo.

Como o Instagram e o YouTube mudam os identificadores internos das telas em
atualizações, a detecção usa várias pistas ao mesmo tempo (id do elemento,
descrição de acessibilidade, clique em abas conhecidas) em vez de depender
de um único identificador fixo. Isso torna o app mais resistente a
atualizações, mas não 100% à prova de mudanças — se algum dia parar de
funcionar depois de um update de um desses apps, normalmente basta abrir
`BlockerAccessibilityService.kt` e adicionar novos termos às listas
`*_KEYWORDS` no topo do arquivo.

## Estrutura do projeto

```
ReelsShortsBlocker/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/blocker/reelsshorts/
│       │   ├── MainActivity.kt              -> tela de configuração
│       │   ├── BlockerAccessibilityService.kt -> lógica de detecção/bloqueio
│       │   └── PrefsManager.kt              -> preferências e estatísticas
│       └── res/
│           ├── layout/activity_main.xml
│           ├── values/ (strings, cores, tema)
│           └── xml/accessibility_service_config.xml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Como abrir e rodar

1. Baixe/descompacte a pasta `ReelsShortsBlocker`.
2. Abra o **Android Studio** (versão recente, Koala ou superior recomendado)
   → **Open** → selecione a pasta `ReelsShortsBlocker`.
3. Deixe o Gradle sincronizar (ele vai baixar o Gradle Wrapper automaticamente
   na primeira sincronização, mesmo sem os arquivos do wrapper commitados —
   ou você pode rodar `gradle wrapper` uma vez se preferir gerar localmente).
4. Conecte um celular Android (ou use um emulador com o Instagram/YouTube
   instalados — em emulador sem Play Store é mais difícil instalar esses
   apps) e clique em **Run ▶**.

## Como usar no celular

1. Abra o app **Bloqueador de Reels/Shorts**.
2. Toque em **"Ativar nas Configurações de Acessibilidade"**.
3. Nas configurações do Android, procure o app na lista de **Serviços de
   Acessibilidade instalados** e ative-o.
4. O Android vai mostrar um aviso padrão de permissão (isso aparece para
   qualquer app que usa este tipo de serviço, pois ele tecnicamente tem
   acesso à tela) — toque em **Permitir/OK**.
5. Volte para o app: o status deve mudar para **"ATIVO"**.
6. Use os interruptores para ligar/desligar o bloqueio do Instagram e do
   YouTube separadamente.
7. Abra o Instagram ou o YouTube normalmente e tente acessar Reels/Shorts —
   a tela deve voltar sozinha em menos de 1 segundo.

## Limitações importantes

- **Não é infalível.** Depende de conseguir ler a árvore de elementos da
  tela; atualizações do Instagram/YouTube podem exigir ajustes nas
  palavras-chave do serviço.
- **Consumo de bateria.** Serviços de acessibilidade rodam em segundo plano
  o tempo todo; o impacto costuma ser pequeno, mas existe.
- **Privacidade.** Tecnicamente, um `AccessibilityService` pode ler o
  conteúdo da tela — este projeto só usa isso para checar nomes de
  elementos/descrições dos apps do Instagram e YouTube (via
  `packageNames` no XML de configuração) e não envia nenhum dado para
  fora do aparelho. Se for publicar este app na Play Store, a política de
  uso de Acessibilidade do Google exige uma declaração clara desse uso.
- **Alguns fabricantes** (Xiaomi, Samsung, etc.) têm otimizações de bateria
  agressivas que podem matar o serviço em segundo plano — pode ser
  necessário liberar o app da otimização de bateria nas configurações do
  sistema.

## Possíveis melhorias futuras

- Adicionar suporte a bloqueio por horário (ex.: só bloquear em horário de
  trabalho/estudo).
- Adicionar outros apps (TikTok, Facebook Reels, etc.) — a arquitetura já
  está pronta para isso, bastando adicionar o pacote e palavras-chave.
- Mostrar uma notificação persistente com o status do bloqueio.
