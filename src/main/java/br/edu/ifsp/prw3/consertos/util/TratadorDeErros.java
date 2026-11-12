package br.edu.ifsp.prw3.consertos.util;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity tratarErro401() {
        return ResponseEntity.status(401).body("Login ou senha inválidos");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity tratarErro404() {

        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity tratarErro400( MethodArgumentNotValidException ex ) {

        var erros = ex.getFieldErrors();

        var lista = erros.stream().map(DadosErroValidacao::new).toList();

        return ResponseEntity.badRequest().body(lista);
    }

    private record DadosErroValidacao(String campo, String msgErro) {

        public DadosErroValidacao(FieldError erro) {
            this(erro.getField(), erro.getDefaultMessage());
        }
    }


}

