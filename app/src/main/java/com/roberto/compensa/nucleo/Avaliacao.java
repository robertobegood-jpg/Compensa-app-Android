package com.roberto.compensa.nucleo;

import java.util.Collections;
import java.util.List;

/** Resultado do cálculo de uma chamada. */
public final class Avaliacao {
    public final Chamada chamada;
    public final Cor cor;
    public final double rsKm;
    /** NaN quando não há tempo para calcular. */
    public final double rsHora;
    public final double custo;
    public final double lucro;
    /** NaN quando não há tempo para calcular. */
    public final double lucroHora;
    /** Critérios que puxaram a cor para baixo, ex.: "R$/km abaixo de 1,50". */
    public final List<String> motivos;

    public Avaliacao(Chamada chamada, Cor cor, double rsKm, double rsHora, double custo,
                     double lucro, double lucroHora, List<String> motivos) {
        this.chamada = chamada;
        this.cor = cor;
        this.rsKm = rsKm;
        this.rsHora = rsHora;
        this.custo = custo;
        this.lucro = lucro;
        this.lucroHora = lucroHora;
        this.motivos = Collections.unmodifiableList(motivos);
    }
}
