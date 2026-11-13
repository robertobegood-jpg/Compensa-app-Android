package com.roberto.compensa.nucleo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Calcula R$/km, R$/h, custo e lucro, e decide a cor do semáforo. */
public final class Calculadora {
    private Calculadora() {
    }

    public static Avaliacao avaliar(Chamada ch, Criterios cr) {
        double km = ch.kmTotal();
        double min = ch.minTotal();
        double rsKm = km > 0 ? ch.valor / km : 0;
        double rsHora = min > 0 ? ch.valor / (min / 60.0) : Double.NaN;

        double litros = cr.consumoKmL > 0 ? km / cr.consumoKmL : 0;
        double custo = litros * Math.max(0, cr.precoLitro) + km * Math.max(0, cr.outrosCustosKm);
        double lucro = ch.valor - custo;
        double lucroHora = min > 0 ? lucro / (min / 60.0) : Double.NaN;

        Cor cor = Cor.VERDE;
        List<String> motivos = new ArrayList<>();

        if (cr.usarKm && km > 0) {
            Cor c = maiorMelhor(rsKm, cr.kmVerde, cr.kmAmarelo);
            cor = registrar(cor, c, motivos, "R$/km", rsKm, cr.kmVerde, cr.kmAmarelo, true);
        }
        if (cr.usarHora && !Double.isNaN(rsHora)) {
            Cor c = maiorMelhor(rsHora, cr.horaVerde, cr.horaAmarelo);
            cor = registrar(cor, c, motivos, "R$/h", rsHora, cr.horaVerde, cr.horaAmarelo, true);
        }
        if (cr.usarBusca && ch.kmBusca >= 0) {
            Cor c = menorMelhor(ch.kmBusca, cr.buscaVerde, cr.buscaAmarelo);
            cor = registrar(cor, c, motivos, "Busca (km)", ch.kmBusca, cr.buscaVerde, cr.buscaAmarelo, false);
        }
        if (cr.usarValor) {
            Cor c = maiorMelhor(ch.valor, cr.valorVerde, cr.valorAmarelo);
            cor = registrar(cor, c, motivos, "Valor", ch.valor, cr.valorVerde, cr.valorAmarelo, true);
        }
        return new Avaliacao(ch, cor, rsKm, rsHora, custo, lucro, lucroHora, motivos);
    }

    static Cor maiorMelhor(double v, double verde, double amarelo) {
        if (v >= verde - 1e-9) return Cor.VERDE;
        if (v >= amarelo - 1e-9) return Cor.AMARELO;
        return Cor.VERMELHO;
    }

    static Cor menorMelhor(double v, double verde, double amarelo) {
        if (v <= verde + 1e-9) return Cor.VERDE;
        if (v <= amarelo + 1e-9) return Cor.AMARELO;
        return Cor.VERMELHO;
    }

    private static Cor registrar(Cor atual, Cor c, List<String> motivos, String nome, double v,
                                 double verde, double amarelo, boolean maiorMelhor) {
        if (c != Cor.VERDE) {
            double limite = c == Cor.AMARELO ? verde : amarelo;
            String rel = maiorMelhor ? "abaixo de" : "acima de";
            motivos.add(nome + " " + num(v) + " " + rel + " " + num(limite));
        }
        return Cor.pior(atual, c);
    }

    static String num(double v) {
        return String.format(Locale.forLanguageTag("pt-BR"), "%.2f", v);
    }
}
