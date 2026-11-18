package com.atelie.sistema.ordemservico;

import com.atelie.sistema.ordemservico.dto.OrdemServicoResponseDTO;
import com.atelie.sistema.ordemservico.dto.PagamentoResponse;
import com.atelie.sistema.ordemservico.state.StatusOrdemServico;
import com.atelie.sistema.peca.Peca;
import com.atelie.sistema.peca.PecaRepository;
import com.atelie.sistema.peca.TipoFluxo;
import com.atelie.sistema.shared.exception.RecursoNaoEncontradoException;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentoPdfService {
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final OrdemServicoRepository ordens;
    private final PecaRepository pecas;

    public DocumentoPdfService(OrdemServicoRepository ordens, PecaRepository pecas) {
        this.ordens = ordens;
        this.pecas = pecas;
    }

    @Transactional(readOnly = true)
    public byte[] gerarContrato(UUID ordemId) {
        OrdemServico os = buscarOrdem(ordemId);
        if (os.getTipoFluxo() != TipoFluxo.LOCACAO)
            throw new RegraNegocioException("Contrato de locação só pode ser emitido para ordem de locação.");
        List<Peca> catalogo = pecas.findAllById(os.getItens().stream().map(ItemOrdemServico::pecaId).toList());
        var porId = catalogo.stream().collect(java.util.stream.Collectors.toMap(Peca::getId, p -> p));
        List<String> linhas = new ArrayList<>(List.of(
                "LINHO - GESTAO DE ATELIE", "RESUMO DA ORDEM DE LOCACAO", "",
                "Ordem: " + os.getId(), "Emitido em: " + DATA_HORA.format(LocalDateTime.now()),
                "Cliente: " + os.getCliente().getNome(), "CPF: " + os.getCliente().getCpf(), "",
                "Periodo de locacao:", "Inicio: " + formatar(os.getInicioLocacao()),
                "Fim: " + formatar(os.getFimLocacao()), "Evento: " + formatar(os.getDataEvento()), "",
                "Pecas contratadas:"));
        for (ItemOrdemServico item : os.getItens()) {
            Peca peca = porId.get(item.pecaId());
            linhas.add("- " + (peca == null ? item.pecaId() : peca.getDescricao() + " / etiqueta " + peca.getCodigoEtiqueta())
                    + " / valor negociado: " + moeda(item.valorUnitario()));
        }
        linhas.add("");
        linhas.add("Valor total registrado: " + moeda(os.getValorTotal()));
        linhas.add("Desconto registrado: " + moeda(os.getDesconto()));
        linhas.add("Situacao da ordem: " + os.getStatusAtual());
        linhas.add("");
        linhas.add("Documento informativo gerado a partir dos dados persistidos da ordem.");
        linhas.add("O sistema nao possui clausulas comerciais adicionais cadastradas.");
        return criarPdf(linhas);
    }

    @Transactional(readOnly = true)
    public byte[] gerarRecibo(UUID ordemId, Long recebimentoId) {
        OrdemServico os = buscarOrdem(ordemId);
        Recebimento recebimento = os.getPagamento().getRecebimentos().stream()
                .filter(r -> java.util.Objects.equals(recebimentoId, r.getId())).findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Recebimento não encontrado nesta ordem."));
        return criarPdf(List.of("LINHO - GESTAO DE ATELIE", "RECIBO DE RECEBIMENTO", "",
                "Ordem: " + os.getId(), "Cliente: " + os.getCliente().getNome(),
                "Valor recebido: " + moeda(recebimento.getValor()), "Data: " + DATA.format(recebimento.getData()),
                "Metodo: " + recebimento.getMetodo(), "Tipo: " + (recebimento.isSinal() ? "Sinal" : "Pagamento"),
                "Situacao da ordem: " + os.getStatusAtual()));
    }

    private OrdemServico buscarOrdem(UUID id) {
        return ordens.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de serviço não encontrada: " + id));
    }

    private byte[] criarPdf(List<String> linhas) {
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            var fonte = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDPage pagina = new PDPage();
            documento.addPage(pagina);
            PDPageContentStream fluxo = new PDPageContentStream(documento, pagina);
            float y = 760;
            fluxo.beginText();
            fluxo.setFont(fonte, 11);
            fluxo.newLineAtOffset(50, y);
            for (String linha : linhas) {
                if (y < 55) {
                    fluxo.endText();
                    fluxo.close();
                    pagina = new PDPage();
                    documento.addPage(pagina);
                    fluxo = new PDPageContentStream(documento, pagina);
                    y = 760;
                    fluxo.beginText();
                    fluxo.setFont(fonte, 11);
                    fluxo.newLineAtOffset(50, y);
                }
                for (String parte : quebrar(linha, 92)) {
                    fluxo.showText(parte);
                    fluxo.newLineAtOffset(0, -16);
                    y -= 16;
                }
            }
            fluxo.endText();
            fluxo.close();
            documento.save(saida);
            return saida.toByteArray();
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("Não foi possível gerar o documento PDF.", ex);
        }
    }

    private List<String> quebrar(String texto, int maximo) {
        if (texto.length() <= maximo) return List.of(texto);
        var partes = new ArrayList<String>();
        String restante = texto;
        while (restante.length() > maximo) {
            int corte = restante.lastIndexOf(' ', maximo);
            if (corte < 1) corte = maximo;
            partes.add(restante.substring(0, corte));
            restante = restante.substring(corte).stripLeading();
        }
        partes.add(restante);
        return partes;
    }

    private String formatar(LocalDateTime valor) {
        return valor == null ? "Nao informado" : DATA_HORA.format(valor);
    }

    private String moeda(BigDecimal valor) {
        return Currency.getInstance("BRL").getSymbol(Locale.forLanguageTag("pt-BR")) + " "
                + valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
