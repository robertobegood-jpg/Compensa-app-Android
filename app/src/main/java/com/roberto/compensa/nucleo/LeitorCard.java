package com.roberto.compensa.nucleo;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lê os textos de um card de chamada (Uber, 99) e monta a {@link Chamada}.
 *
 * Os apps mudam o layout com frequência, então a leitura não depende de posições fixas:
 * procura o valor em R$, os pares "tempo + distância" e palavras de chamada ("Aceitar").
 * O primeiro par é a busca (até o passageiro) e o segundo é a viagem.
 */
public final class LeitorCard {
    private LeitorCard() {
    }

    /** "R$ 12,34", "R$12.34", "R$ 1.234,56". Não pega "R$ 1,85/km" nem "R$ 30/h". */
    private static final Pattern VALOR = Pattern.compile(
            "R\\$\\s*(\\d{1,3}(?:\\.\\d{3})+(?:,\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)(?![.,]?\\d)(?!\\s*/\\s*(?:km|h|min|hora))",
            Pattern.CASE_INSENSITIVE);

    /** "2,8 km", "2.8km", "800 m". */
    private static final Pattern DIST = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(km|m)(?![a-z])", Pattern.CASE_INSENSITIVE);

    /** "8 min", "1 h 10 min", "1h05", "2 h". */
    private static final Pattern DUR = Pattern.compile(
            "(?:(\\d+)\\s*h(?:oras?|rs?)?\\s*(\\d{1,2})?\\s*(?:min(?:utos?)?)?)(?![a-z])|(\\d+)\\s*min(?:utos?)?(?![a-z])",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PALAVRA_CHAMADA = Pattern.compile(
            "\\b(aceitar|aceite|toque para aceitar|nova (?:corrida|viagem|solicitacao)|accept|recusar)\\b",
            Pattern.CASE_INSENSITIVE);

    /** Tudo em minúsculas, sem acento e com espaços normais (o Uber usa espaços especiais). */
    static String normalizar(String s) {
        String t = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        t = t.replace(' ', ' ').replace(' ', ' ').replace(' ', ' ');
        return t.toLowerCase(Locale.ROOT);
    }

    static double numero(String s) {
        String t = s.replace(" ", "");
        int virgula = t.lastIndexOf(',');
        int ponto = t.lastIndexOf('.');
        if (virgula >= 0 && ponto >= 0) {
            // O que vier por último é o decimal.
            if (virgula > ponto) t = t.replace(".", "").replace(',', '.');
            else t = t.replace(",", "");
        } else if (virgula >= 0) {
            t = t.replace(',', '.');
        } else if (ponto >= 0) {
            // "1.234" com 3 casas depois do ponto = milhar; "2.8" = decimal.
            String depois = t.substring(ponto + 1);
            if (depois.length() == 3 && t.indexOf('.') == ponto && t.length() > 4) t = t.replace(".", "");
        }
        return Double.parseDouble(t);
    }

    private static final class Achado {
        final int ini, fim;
        final double v;

        Achado(int ini, int fim, double v) {
            this.ini = ini;
            this.fim = fim;
            this.v = v;
        }
    }

    /**
     * @param app     "Uber" ou "99"
     * @param textos  textos e descrições dos elementos da tela, na ordem em que aparecem
     * @param velocidadeMedia km/h para estimar o tempo quando o card não mostra
     * @return a chamada, ou null se a tela não for um card de chamada
     */
    public static Chamada ler(String app, List<String> textos, double velocidadeMedia) {
        StringBuilder sb = new StringBuilder();
        for (String s : textos) {
            if (s == null || s.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(" | ");
            sb.append(s.trim());
        }
        String texto = normalizar(sb.toString());
        if (texto.isEmpty()) return null;

        // Valor: o maior R$ da tela (descarta "R$/km" e bônus menores que aparecem junto).
        double valor = -1;
        Matcher mv = VALOR.matcher(texto);
        while (mv.find()) {
            try {
                double v = numero(mv.group(1));
                if (v > valor && v < 5000) valor = v;
            } catch (NumberFormatException ignorada) {
                // segue procurando
            }
        }
        if (valor <= 0) return null;

        List<Achado> dists = new ArrayList<>();
        Matcher md = DIST.matcher(texto);
        while (md.find()) {
            try {
                double v = numero(md.group(1));
                if (md.group(2).equalsIgnoreCase("m")) v = v / 1000.0;
                if (v >= 0 && v < 1000) dists.add(new Achado(md.start(), md.end(), v));
            } catch (NumberFormatException ignorada) {
                // segue
            }
        }
        if (dists.isEmpty()) return null;

        List<Achado> durs = new ArrayList<>();
        Matcher mt = DUR.matcher(texto);
        while (mt.find()) {
            double minutos;
            if (mt.group(3) != null) {
                minutos = Double.parseDouble(mt.group(3));
            } else {
                minutos = Double.parseDouble(mt.group(1)) * 60
                        + (mt.group(2) != null ? Double.parseDouble(mt.group(2)) : 0);
            }
            if (minutos >= 0 && minutos < 24 * 60) durs.add(new Achado(mt.start(), mt.end(), minutos));
        }

        boolean temPalavra = PALAVRA_CHAMADA.matcher(texto).find();

        // Junta cada distância ao tempo mais próximo (até 30 caracteres de distância).
        List<double[]> pares = new ArrayList<>(); // {km, min ou -1}
        boolean[] usado = new boolean[durs.size()];
        for (Achado d : dists) {
            int melhor = -1;
            int menorGap = Integer.MAX_VALUE;
            for (int i = 0; i < durs.size(); i++) {
                if (usado[i]) continue;
                Achado t = durs.get(i);
                int gap = t.fim <= d.ini ? d.ini - t.fim : (d.fim <= t.ini ? t.ini - d.fim : 0);
                if (gap < menorGap) {
                    menorGap = gap;
                    melhor = i;
                }
            }
            if (melhor >= 0 && menorGap <= 30) {
                usado[melhor] = true;
                pares.add(new double[]{d.v, durs.get(melhor).v});
            } else {
                pares.add(new double[]{d.v, -1});
            }
        }

        // Sem palavra de chamada, só aceita se tiver o par completo de busca e viagem.
        if (!temPalavra && !(pares.size() >= 2 && pares.get(0)[1] >= 0 && pares.get(1)[1] >= 0)) {
            return null;
        }

        double kmBusca, minBusca, kmViagem, minViagem;
        if (pares.size() >= 2) {
            kmBusca = pares.get(0)[0];
            minBusca = pares.get(0)[1];
            kmViagem = pares.get(1)[0];
            minViagem = pares.get(1)[1];
        } else {
            // Só uma distância: é a viagem; busca desconhecida.
            kmBusca = -1;
            minBusca = -1;
            kmViagem = pares.get(0)[0];
            minViagem = pares.get(0)[1];
        }

        boolean estimado = false;
        double vel = velocidadeMedia > 0 ? velocidadeMedia : 25;
        if (kmBusca >= 0 && minBusca < 0) {
            minBusca = kmBusca / vel * 60;
            estimado = true;
        }
        if (minViagem < 0) {
            minViagem = kmViagem / vel * 60;
            estimado = true;
        }
        return new Chamada(app, valor, kmBusca, minBusca, kmViagem, minViagem, estimado);
    }
}
