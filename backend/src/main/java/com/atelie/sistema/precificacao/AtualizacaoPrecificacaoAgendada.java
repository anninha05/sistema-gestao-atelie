package com.atelie.sistema.precificacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "precificacao.agendamento.ativo", havingValue = "true", matchIfMissing = true)
class AtualizacaoPrecificacaoAgendada {
    private static final Logger log = LoggerFactory.getLogger(AtualizacaoPrecificacaoAgendada.class);
    private final AtualizacaoDiariaPIDService atualizacoes;

    AtualizacaoPrecificacaoAgendada(AtualizacaoDiariaPIDService atualizacoes) {
        this.atualizacoes = atualizacoes;
    }

    // Verifica na inicialização e a cada hora. O serviço garante uma execução efetiva por dia.
    @Scheduled(initialDelay = 10000, fixedDelay = 3600000)
    void atualizar() {
        try {
            atualizacoes.atualizarAutomaticamente();
        } catch (RuntimeException falha) {
            log.error("Falha ao atualizar recomendações de preço; a rotina tentará novamente no próximo ciclo.", falha);
        }
    }
}
