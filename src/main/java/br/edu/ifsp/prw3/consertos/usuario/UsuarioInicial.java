package br.edu.ifsp.prw3.consertos.usuario;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UsuarioInicial implements CommandLineRunner {

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final String login;
    private final String senha;

    public UsuarioInicial(UsuarioRepository repository, PasswordEncoder encoder,
                          @Value("${OFICINA_LOGIN:}") String login,
                          @Value("${OFICINA_SENHA:}") String senha) {
        this.repository = repository;
        this.encoder = encoder;
        this.login = login;
        this.senha = senha;
    }

    @Override
    public void run(String... args) {
        // Cria o primeiro usuário sem deixar uma senha padrão no código.
        if (login.isBlank() && senha.isBlank()) {
            return;
        }
        if (login.isBlank() || login.length() > 100 || senha.isBlank()) {
            throw new IllegalArgumentException("Informe OFICINA_LOGIN (até 100 caracteres) e OFICINA_SENHA.");
        }
        if (repository.findByLogin(login).isEmpty()) {
            repository.save(new Usuario(login, encoder.encode(senha)));
        }
    }
}
