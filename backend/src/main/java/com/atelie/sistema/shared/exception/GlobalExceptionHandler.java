package com.atelie.sistema.shared.exception;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErroResponse(int status, String mensagem, LocalDateTime timestamp) {

        public static ErroResponse de(HttpStatusCode status, String mensagem) {
            return new ErroResponse(status.value(), mensagem, LocalDateTime.now());
        }
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> tratarRegraNegocio(RegraNegocioException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErroResponse> tratarAtualizacaoConcorrente() {
        return responder(HttpStatus.CONFLICT, "O registro foi alterado. Consulte os dados atuais e tente novamente.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> tratarDadosEmConflito() {
        return responder(HttpStatus.CONFLICT,
                "Os dados conflitam com um registro existente. Atualize a página e confira as informações.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarErroInesperado(Exception ex) {
        LOG.error("Erro inesperado ao processar a requisição", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado. Tente novamente em instantes.");
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return ResponseEntity.status(status).headers(headers).body(ErroResponse.de(status, mensagemPara(ex, status)));
    }

    private String mensagemPara(Exception ex, HttpStatusCode status) {
        if (ex instanceof MethodArgumentTypeMismatchException tipoInvalido) {
            return "O valor informado em '" + tipoInvalido.getName() + "' é inválido.";
        }
        if (ex instanceof MissingServletRequestParameterException ausente) {
            return "Informe o parâmetro '" + ausente.getParameterName() + "'.";
        }
        return switch (status.value()) {
            case 400 -> "Os dados enviados são inválidos. Confira os campos e tente novamente.";
            case 404 -> "Recurso não encontrado.";
            case 405 -> "Operação não permitida para este endereço.";
            case 415 -> "Formato de envio não suportado.";
            default -> "Não foi possível processar a requisição.";
        };
    }

    private ResponseEntity<ErroResponse> responder(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(ErroResponse.de(status, mensagem));
    }
}