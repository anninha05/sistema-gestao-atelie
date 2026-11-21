package com.atelie.sistema.precificacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

public final class PrecificacaoDTO {

    private PrecificacaoDTO() {
    }

    public record MetaRequest(BigDecimal valorMeta) {
    }

    public record MetaResponse(YearMonth mes, BigDecimal valorMeta, String responsavelNome) {

        static MetaResponse de(MetaMensal meta) {
            return new MetaResponse(meta.getMes(), meta.getValorMeta(), meta.getResponsavel().getNome());
        }
    }

    public record PoliticaRequest(double ganhoProporcional, double ganhoIntegral, double ganhoDerivativo,
            double ajusteMinimo, double ajusteMaximo, BigDecimal descontoFidelidade, Integer quantidadeOsFidelidade) {
    }

    public record PoliticaResponse(double ganhoProporcional, double ganhoIntegral, double ganhoDerivativo,
            double ajusteMinimo, double ajusteMaximo, BigDecimal descontoFidelidade, Integer quantidadeOsFidelidade,
            String responsavelNome, LocalDateTime atualizadaEm) {

        static PoliticaResponse de(PoliticaPrecificacao politica) {
            return new PoliticaResponse(politica.getGanhoProporcional(), politica.getGanhoIntegral(),
                    politica.getGanhoDerivativo(), politica.getAjusteMinimo(), politica.getAjusteMaximo(),
                    politica.getDescontoFidelidade(), politica.getQuantidadeOsFidelidade(),
                    politica.getResponsavel().getNome(), politica.getAtualizadaEm());
        }
    }

    public record CalibracaoResponse(double kpAnterior, double kiAnterior, double kdAnterior, double custoAnterior,
            double kp, double ki, double kd, double custo, int iteracoes) {
    }

    public record ExecucaoResponse(Long id, YearMonth mes, LocalDate dataApuracao, long deltaDias,
            BigDecimal valorMeta, BigDecimal resultadoRealizado, BigDecimal referenciaAcumulada,
            double erroNormalizado, double termoProporcional, double termoIntegral, double termoDerivativo,
            double saidaBruta, double ajusteLimitado, double integralAcumulada, double ganhoProporcional,
            double ganhoIntegral, double ganhoDerivativo, double ajusteMinimo, double ajusteMaximo,
            String responsavelNome) {

        static ExecucaoResponse de(ExecucaoPIDDiaria e) {
            return new ExecucaoResponse(e.getId(), e.getMes(), e.getDataApuracao(), e.getDeltaDias(),
                    e.getValorMeta(), e.getResultadoRealizado(), e.getReferenciaAcumulada(), e.getErroNormalizado(),
                    e.getTermoProporcional(), e.getTermoIntegral(), e.getTermoDerivativo(), e.getSaidaBruta(),
                    e.getAjusteLimitado(), e.getIntegralAcumulada(), e.getGanhoProporcional(), e.getGanhoIntegral(),
                    e.getGanhoDerivativo(), e.getAjusteMinimo(), e.getAjusteMaximo(), e.getResponsavelNome());
        }
    }

    public record SugestaoRequest(UUID ordemServicoId, BigDecimal pisoMinimo) {
    }

    public record SugestaoResponse(Long id, UUID ordemServicoId, UUID clienteId, Long execucaoPidId,
            BigDecimal precoBase, double ajustePid, BigDecimal precoAjustado, BigDecimal descontoFidelidade,
            BigDecimal precoAposFidelidade, BigDecimal pisoMinimo, BigDecimal precoSugerido,
            int ordensConcluidas, int ordensCanceladas, boolean elegivelFidelidade, String justificativa,
            String responsavelNome, LocalDateTime criadaEm, String aprovadaPorNome, LocalDateTime aprovadaEm) {

        static SugestaoResponse de(SugestaoPreco s) {
            return new SugestaoResponse(s.getId(), s.getOrdemServicoId(), s.getClienteId(), s.getExecucaoPidId(),
                    s.getPrecoBase(), s.getAjustePid(), s.getPrecoAjustado(), s.getDescontoFidelidade(),
                    s.getPrecoAposFidelidade(), s.getPisoMinimo(), s.getPrecoSugerido(), s.getOrdensConcluidas(),
                    s.getOrdensCanceladas(), s.isElegivelFidelidade(), s.getJustificativa(),
                    s.getResponsavel().getNome(), s.getCriadaEm(),
                    s.getAprovadaPor() == null ? null : s.getAprovadaPor().getNome(), s.getAprovadaEm());
        }
    }
}
