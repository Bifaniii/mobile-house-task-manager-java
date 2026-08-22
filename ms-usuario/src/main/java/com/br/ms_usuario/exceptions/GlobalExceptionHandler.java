package com.br.ms_usuario.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailJaCadastrado(EmailJaCadastradoException ex) {
        var body = new ApiErrorResponse(HttpStatus.CONFLICT.value(), "Conflito", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> handleUsuarioNaoEncontrado(UsuarioNaoEncontradoException ex) {
        var body = new ApiErrorResponse(HttpStatus.NOT_FOUND.value(), "Não encontrado", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ApiErrorResponse> handleCredenciaisInvalidas(CredenciaisInvalidasException ex) {
        var body = new ApiErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Não autorizado", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidacao(MethodArgumentNotValidException ex) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        var body = new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), "Erro de validação",
                "Um ou mais campos estão inválidos", detalhes);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenerico(Exception ex) {
        var body = new ApiErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erro interno",
                "Ocorreu um erro inesperado");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}