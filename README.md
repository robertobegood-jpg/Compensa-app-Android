# Compensa?

App Android para motorista de aplicativo. Ele lê o card de chamada do **Uber Driver** e do **99 Motorista** e mostra na hora um **semáforo** dizendo se a corrida compensa, com base no valor, na distância e no tempo.

Também tem **gravação de segurança** (vídeo e áudio em segundo plano), para servir de prova em caso de golpe ou acusação falsa.

Gratuito, sem anúncios e de código aberto (GPL-3.0).

## O que ele faz

- **Semáforo por cima do Uber/99.** Verde = compensa, amarelo = avaliar, vermelho = não compensa. Mostra o valor, R$/km, R$/hora, a distância total e o lucro já descontando o combustível. Toque para fechar e arraste para mudar de lugar.
- **Critérios editáveis**, já preenchidos: R$/km, R$/hora, distância até o passageiro e valor mínimo, cada um com limite para verde e amarelo. Também dá para ligar ou desligar cada critério.
- **Custos do carro**: consumo (km/l), preço do combustível e outros custos por km. Com isso o app calcula o lucro.
- **Histórico** das chamadas vistas, com resumo do dia e média de R$/km por horário.
- **Gravação de segurança**: câmera frontal (cabine) ou traseira, com ou sem áudio. Funciona com a tela apagada. Pode ser iniciada pelo app ou pelo botão REC flutuante. Os vídeos ficam na galeria, em Filmes › Compensa.
- **Testar cálculo**: digite os dados de uma corrida ou cole o texto de um card.
- **Modo captura**: guarda o texto lido dos cards num arquivo, para calibrar a leitura quando o Uber ou a 99 mudam o visual.
- **Cafezinho**: apoio ao desenvolvedor via Pix.

## Como instalar

1. Baixe o APK mais recente em **Releases**.
2. Instale e abra o Compensa.
3. No Painel, toque em **Ligar o leitor** e ative o Compensa em Acessibilidade.
4. Abra o Uber Driver ou o 99 Motorista e fique online.

## Privacidade

- O app só lê a tela do Uber Driver (`com.ubercab.driver`) e do 99 Motorista (`com.app99.driver`).
- Ele nunca toca na tela e nunca aceita nem recusa corridas.
- Não pede permissão de internet. Tudo fica no celular.

## Limites conhecidos

- A leitura depende do texto que o Uber e a 99 colocam no card. Se eles mudarem o layout, o semáforo pode parar de aparecer até a leitura ser ajustada. Se isso acontecer, use o **modo captura** e abra uma *issue* com o arquivo.
- O Android sempre mostra um aviso enquanto grava: a notificação "Proteção ativa" e, a partir do Android 12, o ponto verde de câmera. Nenhum app consegue esconder isso.
- Os termos de uso do Uber e da 99 podem restringir apps que leem a tela. Use por sua conta.

## Para desenvolvedores

- Kotlin + Jetpack Compose (visual no sistema Oficina) e CameraX.
- A leitura e o cálculo ficam em `app/src/main/java/com/roberto/compensa/nucleo/`, em Java puro e com testes em `app/src/test/`.
- Cada push na `main` roda os testes e gera o APK assinado (Actions › Artifacts). Uma tag `v*` publica a Release.
- Secrets necessários: `KEYSTORE_BASE64` e `KEYSTORE_PASSWORD`.
