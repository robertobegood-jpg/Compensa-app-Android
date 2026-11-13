package com.roberto.compensa.nucleo;

/**
 * Uma chamada de corrida lida do card do Uber ou da 99.
 * Distâncias em km, tempos em minutos. Valores negativos = não informado no card.
 */
public final class Chamada {
    public final String app;
    public final double valor;
    public final double kmBusca;
    public final double minBusca;
    public final double kmViagem;
    public final double minViagem;
    /** true quando algum tempo foi estimado pela velocidade média (o card não mostrava). */
    public final boolean tempoEstimado;

    public Chamada(String app, double valor, double kmBusca, double minBusca,
                   double kmViagem, double minViagem, boolean tempoEstimado) {
        this.app = app;
        this.valor = valor;
        this.kmBusca = kmBusca;
        this.minBusca = minBusca;
        this.kmViagem = kmViagem;
        this.minViagem = minViagem;
        this.tempoEstimado = tempoEstimado;
    }

    public double kmTotal() {
        return Math.max(0, kmBusca) + Math.max(0, kmViagem);
    }

    public double minTotal() {
        return Math.max(0, minBusca) + Math.max(0, minViagem);
    }

    /** Assinatura para não repetir a mesma chamada várias vezes enquanto o card está na tela. */
    public String assinatura() {
        return app + "|" + Math.round(valor * 100) + "|" + Math.round(kmBusca * 10) + "|" + Math.round(kmViagem * 10);
    }

    @Override
    public String toString() {
        return "Chamada{" + app + " R$" + valor + " busca " + kmBusca + "km/" + minBusca + "min, viagem "
                + kmViagem + "km/" + minViagem + "min" + (tempoEstimado ? " (tempo estimado)" : "") + "}";
    }
}
