package com.atelie.sistema.ordemservico.state;

import com.atelie.sistema.usuario.Perfil;
import java.util.EnumSet;
import java.util.Set;

public enum AcaoOrdemServico {
    INICIAR_CONFECCAO(Setor.PRODUCAO, "Iniciar confecção", "Uma ordem %s não pode iniciar a confecção ou os ajustes."),
    ENVIAR_PARA_PROVA(Setor.PRODUCAO, "Enviar para prova", "Uma ordem %s não pode ser enviada para prova."),
    INICIAR_PASSADORIA(Setor.PRODUCAO, "Iniciar passadoria", "Uma ordem %s não pode ir para a passadoria."),
    CONCLUIR(Setor.PRODUCAO, "Concluir produção", "Uma ordem %s não pode ser concluída."),
    RETIRAR(Setor.ATENDIMENTO, "Registrar retirada", "Uma ordem %s não pode ser retirada ou entregue."),
    DEVOLVER(Setor.ATENDIMENTO, "Registrar devolução", "Uma ordem %s não pode ser devolvida."),
    REGISTRAR_RECEBIMENTO(Setor.ATENDIMENTO, "Registrar recebimento", "Uma ordem %s não aceita novos recebimentos."),
    REAGENDAR(Setor.ATENDIMENTO, "Reagendar", "Uma ordem %s não pode ser reagendada."),
    CANCELAR(Setor.ATENDIMENTO, "Cancelar ordem", "Uma ordem %s não pode mais ser cancelada.");

    public enum Setor {
        PRODUCAO(EnumSet.of(Perfil.ROLE_ADMIN, Perfil.ROLE_PRODUCAO)),
        ATENDIMENTO(EnumSet.of(Perfil.ROLE_ADMIN, Perfil.ROLE_ATENDENTE));

        private final Set<Perfil> perfis;

        Setor(Set<Perfil> perfis) {
            this.perfis = perfis;
        }
    }

    private final Setor setor;
    private final String rotulo;
    private final String recusa;

    AcaoOrdemServico(Setor setor, String rotulo, String recusa) {
        this.setor = setor;
        this.rotulo = rotulo;
        this.recusa = recusa;
    }

    public Setor getSetor() {
        return setor;
    }

    public String getRotulo() {
        return rotulo;
    }

    public boolean permitidaPara(Perfil perfil) {
        return setor.perfis.contains(perfil);
    }

    String mensagemDeRecusa(StatusOrdemServico status) {
        return recusa.formatted(status.getDescricao());
    }
}
