package com.roberto.compensa.nucleo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import org.junit.Test;

/**
 * Textos montados no formato que os cards costumam ter.
 * Quando o "modo captura" trouxer cards reais, eles entram aqui também.
 */
public class LeitorCardTest {

    private static Chamada ler(String... textos) {
        return LeitorCard.ler("Uber", Arrays.asList(textos), 25);
    }

    @Test
    public void uberPadrao() {
        Chamada c = ler("UberX", "R$ 18,40", "4,95 ★", "8 min (2,8 km) de distância",
                "Rua Augusta, São Paulo", "Viagem de 22 min (9,6 km)", "Av. Paulista", "Aceitar");
        assertNotNull(c);
        assertEquals(18.40, c.valor, 0.001);
        assertEquals(2.8, c.kmBusca, 0.001);
        assertEquals(8, c.minBusca, 0.001);
        assertEquals(9.6, c.kmViagem, 0.001);
        assertEquals(22, c.minViagem, 0.001);
        assertFalse(c.tempoEstimado);
    }

    @Test
    public void uberComPontoDecimalEEspacoEspecial() {
        Chamada c = ler("Uber Comfort", "R$ 25.90", "5 min (1.9 km) away", "Trip: 31 min (14.2 km)", "Accept");
        assertNotNull(c);
        assertEquals(25.90, c.valor, 0.001);
        assertEquals(1.9, c.kmBusca, 0.001);
        assertEquals(14.2, c.kmViagem, 0.001);
        assertEquals(31, c.minViagem, 0.001);
    }

    @Test
    public void viagemLongaComHoras() {
        Chamada c = ler("R$ 1.234,50", "12 min (5 km)", "1 h 10 min (68,5 km)", "Aceitar");
        assertNotNull(c);
        assertEquals(1234.50, c.valor, 0.001);
        assertEquals(70, c.minViagem, 0.001);
        assertEquals(68.5, c.kmViagem, 0.001);
    }

    @Test
    public void horasSemMinutos() {
        Chamada c = ler("R$ 150,00", "10 min (4 km)", "2 h (120 km)", "Aceitar");
        assertNotNull(c);
        assertEquals(120, c.minViagem, 0.001);
    }

    @Test
    public void noventaENoveComRsPorKmEDistanciaEmMetros() {
        Chamada c = LeitorCard.ler("99", Arrays.asList(
                "99Pop", "R$ 12,30", "R$ 1,85/km", "800 m • 3 min", "Embarque", "6,6 km • 17 min", "Destino", "Aceitar"), 25);
        assertNotNull(c);
        assertEquals(12.30, c.valor, 0.001);
        assertEquals(0.8, c.kmBusca, 0.001);
        assertEquals(3, c.minBusca, 0.001);
        assertEquals(6.6, c.kmViagem, 0.001);
        assertEquals(17, c.minViagem, 0.001);
    }

    @Test
    public void bonusMenorNaoViraValor() {
        Chamada c = ler("R$ 22,00", "Inclui + R$ 3,50 de dinâmico", "6 min (2 km)", "18 min (8 km)", "Aceitar");
        assertNotNull(c);
        assertEquals(22.00, c.valor, 0.001);
    }

    @Test
    public void semTempoEstimaPelaVelocidade() {
        Chamada c = ler("R$ 15,00", "a 2,5 km", "Viagem de 10 km", "Aceitar");
        assertNotNull(c);
        assertEquals(2.5, c.kmBusca, 0.001);
        assertEquals(6, c.minBusca, 0.001);   // 2,5 km a 25 km/h
        assertEquals(24, c.minViagem, 0.001); // 10 km a 25 km/h
        assertTrue(c.tempoEstimado);
    }

    @Test
    public void soUmaDistanciaViraViagem() {
        Chamada c = ler("R$ 9,80", "12 min (4,1 km)", "Aceitar");
        assertNotNull(c);
        assertEquals(-1, c.kmBusca, 0.001);
        assertEquals(4.1, c.kmViagem, 0.001);
        assertEquals(12, c.minViagem, 0.001);
    }

    @Test
    public void telaDeGanhosNaoEChamada() {
        assertNull(ler("Ganhos de hoje", "R$ 152,40", "8 viagens", "Online 5 h 20 min"));
    }

    @Test
    public void navegacaoSemValorNaoEChamada() {
        assertNull(ler("Vire à direita em 300 m", "12 min", "4,2 km", "Chegada 14:35"));
    }

    @Test
    public void semPalavraMasComParesCompletosAceita() {
        Chamada c = ler("R$ 14,00", "4 min (1,5 km)", "15 min (6 km)");
        assertNotNull(c);
        assertEquals(6, c.kmViagem, 0.001);
    }

    @Test
    public void textosVaziosOuNulos() {
        assertNull(ler());
        assertNull(LeitorCard.ler("Uber", Arrays.asList(null, "", "  "), 25));
    }

    @Test
    public void numeros() {
        assertEquals(2.8, LeitorCard.numero("2,8"), 1e-9);
        assertEquals(2.8, LeitorCard.numero("2.8"), 1e-9);
        assertEquals(1234.5, LeitorCard.numero("1.234,5"), 1e-9);
        assertEquals(1234.5, LeitorCard.numero("1,234.5"), 1e-9);
        assertEquals(12, LeitorCard.numero("12"), 1e-9);
    }
}
