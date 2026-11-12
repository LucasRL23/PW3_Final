package br.edu.ifsp.prw3.consertos.controller;

import br.edu.ifsp.prw3.consertos.security.DadosTokenJWT;
import br.edu.ifsp.prw3.consertos.security.PW3TokenService;
import br.edu.ifsp.prw3.consertos.usuario.DadosAutenticacao;
import br.edu.ifsp.prw3.consertos.usuario.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/login")
public class AutenticacaoController {

    private final AuthenticationManager manager;
    private final PW3TokenService tokenService;

    public AutenticacaoController(AuthenticationManager manager, PW3TokenService tokenService) {
        this.manager = manager;
        this.tokenService = tokenService;
    }

    @PostMapping
    public ResponseEntity<DadosTokenJWT> efetuarLogin(@RequestBody @Valid DadosAutenticacao dados) {
        var credenciais = new UsernamePasswordAuthenticationToken(dados.login(), dados.senha());
        var authentication = manager.authenticate(credenciais);
        var token = tokenService.gerarToken((Usuario) authentication.getPrincipal());
        return ResponseEntity.ok(new DadosTokenJWT(token));
    }
}
