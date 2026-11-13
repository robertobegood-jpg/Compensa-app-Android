package com.roberto.compensa.nucleo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CalculadoraTest {

    private static Chamada ch(double valor, double kmB, double minB, double kmV, double minV) {
        return new Chamada("Uber", valor, kmB, minB, kmV, minV, false);
    }

    @Test
    public void corridaBoaFicaVerde() {
        // 30 / 12 km = 2,50/km ; 30 / 0,5 h = 60/h ; busca 2 km ; valor 30
        Avaliacao a = Calculadora.avaliar(ch(30, 2, 5, 10, 25), new Criterios());
        assertEquals(Cor.VERDE, a.cor);
        assertEquals(2.5, a.rsKm, 1e-9);
        assertEquals(60, a.rsHora, 1e-9);
        assertTrue(a.motivos.isEmpty());
    }

    @Test
    public void custoELucro() {
        Criterios cr = new Criterios(); // 11 km/l, R$ 6,30, R$ 0,20/km
        Avaliacao a = Calculadora.avaliar(ch(30, 2, 5, 9, 25), cr);
        // 11 km: 1 litro = 6,30 + 11 * 0,20 = 2,20 → custo 8,50
        assertEquals(8.50, a.custo, 1e-9);
        assertEquals(21.50, a.lucro, 1e-9);
        assertEquals(43.0, a.lucroHora, 1e-9);
    }

    @Test
    public void rsKmNoLimiteDoAmarelo() {
        // 15 / 10 km = 1,50 → amarelo (limite inclusivo)
        Avaliacao a = Calculadora.avaliar(ch(15, 1, 3, 9, 12), new Criterios());
        assertEquals(Cor.AMARELO, a.cor);
        assertEquals(1, a.motivos.size());
    }

    @Test
    public void buscaLongaDerrubaParaVermelho() {
        Avaliacao a = Calculadora.avaliar(ch(40, 8, 15, 10, 15), new Criterios());
        assertEquals(Cor.VERMELHO, a.cor);
        assertTrue(a.motivos.get(0).startsWith("Busca"));
    }

    @Test
    public void piorCriterioVence() {
        // R$/km ótimo, mas R$/h baixo (trânsito): 20 / 1 h = 20/h → vermelho
        Avaliacao a = Calculadora.avaliar(ch(20, 1, 10, 7, 50), new Criterios());
        assertEquals(Cor.VERMELHO, a.cor);
    }

    @Test
    public void criterioDesligadoNaoConta() {
        Criterios cr = new Criterios();
        cr.usarHora = false;
        Avaliacao a = Calculadora.avaliar(ch(20, 1, 10, 7, 50), cr);
        assertEquals(Cor.VERDE, a.cor);
    }

    @Test
    public void semBuscaIgnoraCriterioDeBusca() {
        Avaliacao a = Calculadora.avaliar(ch(25, -1, -1, 10, 20), new Criterios());
        assertEquals(Cor.VERDE, a.cor);
        assertEquals(2.5, a.rsKm, 1e-9);
    }

    @Test
    public void distanciaZeroNaoQuebra() {
        Avaliacao a = Calculadora.avaliar(ch(10, 0, 0, 0, 0), new Criterios());
        assertEquals(0, a.rsKm, 1e-9);
        assertTrue(Double.isNaN(a.rsHora));
    }

    @Test
    public void consumoZeroNaoDivide() {
        Criterios cr = new Criterios();
        cr.consumoKmL = 0;
        Avaliacao a = Calculadora.avaliar(ch(30, 2, 5, 10, 25), cr);
        assertEquals(12 * 0.20, a.custo, 1e-9);
    }
}
