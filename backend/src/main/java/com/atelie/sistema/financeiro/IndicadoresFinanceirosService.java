package com.atelie.sistema.financeiro;

import com.atelie.sistema.ordemservico.PagamentoRepository;
import com.atelie.sistema.ordemservico.RecebimentoRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class IndicadoresFinanceirosService {
    private final RecebimentoRepository recebimentos;
    private final DespesaRepository despesas;
    private final PagamentoRepository pagamentos;

    IndicadoresFinanceirosService(RecebimentoRepository recebimentos, DespesaRepository despesas,
            PagamentoRepository pagamentos) {
        this.recebimentos = recebimentos;
        this.despesas = despesas;
        this.pagamentos = pagamentos;
    }

    @Transactional(readOnly = true)
    IndicadoresFinanceirosResponse consultar(YearMonth mes) {
        YearMonth periodo = mes == null ? YearMonth.now() : mes;
        var inicio = periodo.atDay(1);
        var fim = periodo.plusMonths(1).atDay(1);
        BigDecimal entradas = zerar(recebimentos.somarRecebimentos(inicio, fim));
        BigDecimal saidas = zerar(despesas.somarPagamentos(inicio, fim));
        BigDecimal saldo = zerar(pagamentos.somarSaldoEmAberto());
        return new IndicadoresFinanceirosResponse(periodo, entradas, saidas, entradas.subtract(saidas), saldo,
                "O resultado é de caixa. Não representa lucro; recebimentos registrados continuam incluídos mesmo se a ordem foi cancelada, pois o sistema ainda não registra estornos.");
    }

    private BigDecimal zerar(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO.setScale(2) : valor.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
