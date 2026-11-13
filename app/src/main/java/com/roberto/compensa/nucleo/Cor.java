package com.roberto.compensa.nucleo;

/** Cores do semáforo, da melhor para a pior. */
public enum Cor {
    VERDE("Compensa"),
    AMARELO("Avaliar"),
    VERMELHO("Não compensa");

    public final String rotulo;

    Cor(String rotulo) {
        this.rotulo = rotulo;
    }

    public static Cor pior(Cor a, Cor b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
