package com.atelie.sistema.ordemservico;

import com.atelie.sistema.cliente.Cliente;
import com.atelie.sistema.peca.PecaRepository;
import com.atelie.sistema.peca.TipoFluxo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentoPdfServiceTest {
    @Test
    void geraContratoDeLocacaoComDadosPersistidosDaOrdem() throws Exception {
        UUID id = UUID.randomUUID();
        UUID pecaId = UUID.randomUUID();
        var cliente = new Cliente("João da Silva", "22999999999", "joao@email.com", "52998224725");
        var inicio = LocalDateTime.of(2026, 12, 10, 9, 30);
        var os = new OrdemServico(cliente, TipoFluxo.LOCACAO, new BigDecimal("250.00"), null,
                List.of(new ItemOrdemServico(pecaId, new BigDecimal("250.00"))), BigDecimal.ZERO,
                inicio, inicio.plusDays(2), null, inicio.plusDays(1), null, null);
        var ordens = mock(OrdemServicoRepository.class);
        var pecas = mock(PecaRepository.class);
        when(ordens.findById(id)).thenReturn(Optional.of(os));

        byte[] pdf = new DocumentoPdfService(ordens, pecas).gerarContrato(id);
        try (var documento = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("RESUMO DA ORDEM DE LOCACAO"));
            assertTrue(texto.contains("João da Silva"));
            assertTrue(texto.contains("250.00"));
            assertTrue(texto.contains("Documento informativo"));
        }
    }

    @Test
    void rejeitaContratoParaOrdemSobMedida() {
        UUID id = UUID.randomUUID();
        var os = new OrdemServico(new Cliente("Ana", "22999999999", "ana@email.com", "52998224725"),
                TipoFluxo.SOB_MEDIDA, new BigDecimal("250.00"), null);
        var ordens = mock(OrdemServicoRepository.class);
        when(ordens.findById(id)).thenReturn(Optional.of(os));
        assertThrows(com.atelie.sistema.shared.exception.RegraNegocioException.class,
                () -> new DocumentoPdfService(ordens, mock(PecaRepository.class)).gerarContrato(id));
    }

    @Test
    void geraReciboIndividualComValorDataMetodoClienteEOrdem() throws Exception {
        UUID id = UUID.randomUUID();
        var os = new OrdemServico(new Cliente("Ana", "22999999999", "ana@email.com", "52998224725"),
                TipoFluxo.LOCACAO, new BigDecimal("250.00"), null);
        os.getPagamento().registrarRecebimento(new BigDecimal("50.00"), java.time.LocalDate.of(2026, 11, 17),
                com.atelie.sistema.ordemservico.MetodoPagamento.PIX, true);
        var ordens = mock(OrdemServicoRepository.class);
        when(ordens.findById(id)).thenReturn(Optional.of(os));

        byte[] pdf = new DocumentoPdfService(ordens, mock(PecaRepository.class)).gerarRecibo(id, null);
        try (var documento = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("RECIBO DE RECEBIMENTO"));
            assertTrue(texto.contains("Ana"));
            assertTrue(texto.contains("50.00"));
            assertTrue(texto.contains("PIX"));
        }
    }
}
