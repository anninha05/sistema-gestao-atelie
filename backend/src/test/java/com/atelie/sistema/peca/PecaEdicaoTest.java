package com.atelie.sistema.peca;

import com.atelie.sistema.peca.dto.PecaRequestDTO;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PecaEdicaoTest {
    @Mock PecaRepository pecaRepository;

    @Test
    void deveEditarCatalogoSemAlterarTipoOuEstadoFisico() {
        UUID id = UUID.randomUUID();
        Peca peca = new Peca("Vestido", TipoFluxo.LOCACAO);
        when(pecaRepository.buscarParaReserva(id)).thenReturn(Optional.of(peca));
        PecaService service = new PecaService(pecaRepository);

        var resposta = service.atualizar(id, new PecaRequestDTO("Vestido novo", TipoFluxo.SOB_MEDIDA,
                new BigDecimal("180.00"), "M", "Azul"));

        assertEquals("Vestido novo", resposta.descricao());
        assertEquals(TipoFluxo.LOCACAO, resposta.tipo());
        assertEquals("DISPONIVEL", resposta.status().name());
        assertEquals(new BigDecimal("180.00"), resposta.valorBase());
    }

    @Test
    void deveRejeitarValorBaseNegativo() {
        UUID id = UUID.randomUUID();
        PecaService service = new PecaService(pecaRepository);
        assertThrows(RegraNegocioException.class, () -> service.atualizar(id,
                new PecaRequestDTO("Vestido", TipoFluxo.LOCACAO, new BigDecimal("-1"), null, null)));
        verify(pecaRepository, never()).save(any());
    }
}
