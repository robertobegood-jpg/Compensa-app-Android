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

    /** Card real do Uber (Envios Carro, nov/2026): total em cima, botão "Tenho interesse". */
    @Test
    public void uberCardNovoComTotalEmCima() {
        Chamada c = ler("Envios Carro", "R$ 7,09", "23 min • 5.5 km", "4,96 (709)", "Verificado",
                "11 min (3.2 km)", "R. Dr. Paulo de Barros Whitaker, Cidade Dutra, São Paulo",
                "12 minutos (2.3 km)", "R. Joaquim Rodrigues de Moraes, 250, Vila Rubi, São Paulo",
                "Tenho interesse");
        assertNotNull(c);
        assertEquals(7.09, c.valor, 0.001);
        assertEquals(3.2, c.kmBusca, 0.001);
        assertEquals(11, c.minBusca, 0.001);
        assertEquals(2.3, c.kmViagem, 0.001);
        assertEquals(12, c.minViagem, 0.001);
    }

    /** O mesmo card, mas com todo o texto num elemento só (como alguns apps expõem). */
    @Test
    public void uberCardNovoTextoJunto() {
        Chamada c = ler("Envios Carro R$ 7,09 23 min • 5.5 km 4,96 (709) Verificado 11 min (3.2 km) "
                + "R. Dr. Paulo de Barros Whitaker 12 minutos (2.3 km) R. Joaquim Rodrigues de Moraes, 250 Tenho interesse");
        assertNotNull(c);
        assertEquals(3.2, c.kmBusca, 0.001);
        assertEquals(2.3, c.kmViagem, 0.001);
    }

    /** Como a leitura por imagem (OCR) devolve o card do Uber: linha a linha, com pequenos erros. */
    @Test
    public void uberLidoPorImagem() {
        Chamada c = ler("Envios Carro", "Exclusivo", "RS 10,02", "27 min • 5.8 km", "* 4,68 (274)", "Verificado",
                "10 min (2.7 km)", "Rua Olívia Guedes Penteado,", "Veleiros, São Paulo",
                "17 minutos (3.1 km)", "R. Nora Ney, 80, Socorro, São Paulo", "Aceitar");
        assertNotNull(c);
        assertEquals(10.02, c.valor, 0.001);
        assertEquals(2.7, c.kmBusca, 0.001);
        assertEquals(10, c.minBusca, 0.001);
        assertEquals(3.1, c.kmViagem, 0.001);
        assertEquals(17, c.minViagem, 0.001);
    }

    @Test
    public void uberCardSemSelos() {
        Chamada c = ler("Envios Carro", "R$ 10,04", "19 min • 6.0 km", "4,65 (2484)",
                "5 min (1.5 km)", "Rua Pedro Santa Lúcia, Cidade Dutra, São Paulo",
                "14 minutos (4.5 km)", "Rua Maria Clara Martins da Silveira, 192, Campo Grande, São Paulo", "Tenho interesse");
        assertNotNull(c);
        assertEquals(10.04, c.valor, 0.001);
        assertEquals(1.5, c.kmBusca, 0.001);
        assertEquals(4.5, c.kmViagem, 0.001);
        assertEquals(14, c.minViagem, 0.001);
    }

    /** Card real da 99 (Entrega Carro, nov/2026): sem botão "Aceitar", com R$/km e preço dinâmico. */
    @Test
    public void noventaENoveCardReal() {
        Chamada c = LeitorCard.ler("99", Arrays.asList("Entrega Carro", "R$38,60", "R$3,86/km", "Preço x2",
                "4,90", "Cartão verif.", "8 min (1,4 km)", "Rua Araguaia Vieira Ribeiro, 175,", "Jardim Satelite",
                "24 min (8,6 km)", "Rua Dr. Abelardo Vergueiro César,", "515 , Vila Alexandria"), 25);
        assertNotNull(c);
        assertEquals(38.60, c.valor, 0.001);
        assertEquals(1.4, c.kmBusca, 0.001);
        assertEquals(8, c.minBusca, 0.001);
        assertEquals(8.6, c.kmViagem, 0.001);
        assertEquals(24, c.minViagem, 0.001);
    }

    /** O mesmo card lido pela imagem, com o "x2" e o "/km" colados de jeitos diferentes. */
    @Test
    public void noventaENoveLidoPorImagem() {
        Chamada c = LeitorCard.ler("99", Arrays.asList("Entrega Carro", "RS38,60", "R$3,86/ km", "Preço x2",
                "4,90 • Cartão verif.", "8 min (1,4 km)", "Rua Araguaia Vieira Ribeiro, 175,",
                "24 min (8,6 km)", "Rua Dr. Abelardo Vergueiro César,"), 25);
        assertNotNull(c);
        assertEquals(38.60, c.valor, 0.001);
        assertEquals(8.6, c.kmViagem, 0.001);
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
