package com.roberto.compensa.nucleo;

/**
 * Limites do semáforo e custos do carro. Todos editáveis no app; aqui ficam os valores de fábrica.
 * Cada critério tem um limite para verde e outro para amarelo. Abaixo do amarelo = vermelho.
 */
public final class Criterios {
    // R$ por km rodado (busca + viagem): maior é melhor
    public boolean usarKm = true;
    public double kmVerde = 2.00;
    public double kmAmarelo = 1.50;

    // R$ por hora (tempo de busca + viagem): maior é melhor
    public boolean usarHora = true;
    public double horaVerde = 40.0;
    public double horaAmarelo = 30.0;

    // Distância até o passageiro (km): menor é melhor
    public boolean usarBusca = true;
    public double buscaVerde = 3.0;
    public double buscaAmarelo = 6.0;

    // Valor da corrida (R$): maior é melhor
    public boolean usarValor = true;
    public double valorVerde = 10.0;
    public double valorAmarelo = 7.0;

    // Custos do carro
    /** km por litro (ou por m³ no GNV). */
    public double consumoKmL = 11.0;
    /** R$ por litro. */
    public double precoLitro = 6.30;
    /** Manutenção, pneus, desgaste etc. em R$ por km. */
    public double outrosCustosKm = 0.20;

    /** Para estimar o tempo quando o card só mostra a distância. */
    public double velocidadeMediaKmH = 25.0;

    public Criterios copia() {
        Criterios c = new Criterios();
        c.usarKm = usarKm; c.kmVerde = kmVerde; c.kmAmarelo = kmAmarelo;
        c.usarHora = usarHora; c.horaVerde = horaVerde; c.horaAmarelo = horaAmarelo;
        c.usarBusca = usarBusca; c.buscaVerde = buscaVerde; c.buscaAmarelo = buscaAmarelo;
        c.usarValor = usarValor; c.valorVerde = valorVerde; c.valorAmarelo = valorAmarelo;
        c.consumoKmL = consumoKmL; c.precoLitro = precoLitro; c.outrosCustosKm = outrosCustosKm;
        c.velocidadeMediaKmH = velocidadeMediaKmH;
        return c;
    }
}
