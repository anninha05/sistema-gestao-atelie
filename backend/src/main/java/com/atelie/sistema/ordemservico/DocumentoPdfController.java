package com.atelie.sistema.ordemservico;

import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ordens-servico")
public class DocumentoPdfController {
    private final DocumentoPdfService documentos;

    public DocumentoPdfController(DocumentoPdfService documentos) {
        this.documentos = documentos;
    }

    @GetMapping(value = "/{id}/contrato.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> contrato(@PathVariable UUID id) {
        return pdf(documentos.gerarContrato(id), "contrato-" + id + ".pdf");
    }

    @GetMapping(value = "/{id}/pagamentos/{recebimentoId}/recibo.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> recibo(@PathVariable UUID id, @PathVariable Long recebimentoId) {
        return pdf(documentos.gerarRecibo(id, recebimentoId), "recibo-" + recebimentoId + ".pdf");
    }

    private ResponseEntity<byte[]> pdf(byte[] conteudo, String nome) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nome).build().toString())
                .body(conteudo);
    }
}
