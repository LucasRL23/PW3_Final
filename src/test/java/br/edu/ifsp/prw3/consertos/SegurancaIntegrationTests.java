package br.edu.ifsp.prw3.consertos;

import br.edu.ifsp.prw3.consertos.security.PW3TokenService;
import br.edu.ifsp.prw3.consertos.usuario.Usuario;
import br.edu.ifsp.prw3.consertos.usuario.UsuarioRepository;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SegurancaIntegrationTests {

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository repository;
    @Autowired private PasswordEncoder encoder;
    @Autowired private PW3TokenService tokenService;
    @Value("${pw3.senha.principal.geracao.token}") private String secret;
    private Usuario usuario;

    @BeforeEach
    void criarUsuario() {
        usuario = repository.saveAndFlush(new Usuario("aluno", encoder.encode("senha-de-teste")));
    }

    @Test
    void bloquearTodosOsEndpointsDeConsertosSemToken() throws Exception {
        for (var request : List.of(get("/consertos"), get("/consertos/algunsdados"),
                get("/consertos/1"), post("/consertos"), put("/consertos"), delete("/consertos/1"))) {
            mvc.perform(request).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void loginRetornaJwtComValidadeDeDuasHorasSemSenhaNemSessao() throws Exception {
        var resposta = mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"aluno\",\"senha\":\"senha-de-teste\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andReturn();
        String token = JsonPath.read(resposta.getResponse().getContentAsString(), "$.token");
        assertEquals("aluno", tokenService.getSubject(token));
        var jwt = JWT.decode(token);
        assertEquals("DISCIPLINA PW3", jwt.getIssuer());
        long segundosRestantes = ChronoUnit.SECONDS.between(Instant.now(), jwt.getExpiresAtAsInstant());
        assertTrue(segundosRestantes >= 7190 && segundosRestantes <= 7200);
        assertNull(resposta.getRequest().getSession(false));
    }

    @Test
    void usuarioCriadoPelaMigrationConsegueFazerLogin() throws Exception {
        var salvo = repository.findByLogin("asdrubal@gmail.com").orElseThrow();
        assertNotEquals("123456", salvo.getSenha());
        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"asdrubal@gmail.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void senhaFicaArmazenadaComoBcrypt() {
        assertNotEquals("senha-de-teste", usuario.getSenha());
        assertTrue(encoder.matches("senha-de-teste", usuario.getSenha()));
    }

    @Test
    void rejeitarSenhaIncorretaEUsuarioInexistente() throws Exception {
        for (String login : List.of("aluno", "inexistente")) {
            mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"login\":\"" + login + "\",\"senha\":\"errada\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void validarCamposObrigatoriosDoLogin() throws Exception {
        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"\",\"senha\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejeitarJsonInvalidoNoLoginSemMascararComo401() throws Exception {
        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginNaoAutenticaARequisicaoSeguinteSemBearer() throws Exception {
        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"aluno\",\"senha\":\"senha-de-teste\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/consertos")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejeitarTokensMalformadosECabecalhoIncorreto() throws Exception {
        for (String header : List.of("Bearer invalido", "Bearer ", "Basic abc", "Bearer a.b.c")) {
            mvc.perform(get("/consertos").header("Authorization", header))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void rejeitarTokenExpirado() throws Exception {
        var token = JWT.create().withIssuer("DISCIPLINA PW3").withSubject("aluno")
                .withExpiresAt(Instant.now().minusSeconds(60)).sign(Algorithm.HMAC256(secret));
        verificarRejeicao(token);
    }

    @Test
    void rejeitarAssinaturaOuEmissorIncorretos() throws Exception {
        var token = JWT.create().withIssuer("DISCIPLINA PW3").withSubject("aluno")
                .withExpiresAt(Instant.now().plusSeconds(60)).sign(Algorithm.HMAC256("outra-chave"));
        verificarRejeicao(token);
        token = JWT.create().withIssuer("OUTRO EMISSOR").withSubject("aluno")
                .withExpiresAt(Instant.now().plusSeconds(60)).sign(Algorithm.HMAC256(secret));
        verificarRejeicao(token);
    }

    @Test
    void rejeitarTokenSemExpiracaoOuSemUsuario() throws Exception {
        verificarRejeicao(JWT.create().withIssuer("DISCIPLINA PW3").withSubject("aluno")
                .sign(Algorithm.HMAC256(secret)));
        verificarRejeicao(JWT.create().withIssuer("DISCIPLINA PW3")
                .withExpiresAt(Instant.now().plusSeconds(60)).sign(Algorithm.HMAC256(secret)));
    }

    @Test
    void rejeitarTokenDeUsuarioRemovido() throws Exception {
        var token = tokenService.gerarToken(usuario);
        repository.delete(usuario);
        repository.flush();
        verificarRejeicao(token);
    }

    @Test
    void apenasPostLoginEPublico() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isUnauthorized());
        mvc.perform(get("/h2-console/")).andExpect(status().isUnauthorized());
    }

    @Test
    void executarCrudComTokenObtidoNoLogin() throws Exception {
        var login = mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"aluno\",\"senha\":\"senha-de-teste\"}"))
                .andExpect(status().isOk()).andReturn();
        String token = JsonPath.read(login.getResponse().getContentAsString(), "$.token");
        String bearer = "Bearer " + token;
        var cadastro = mvc.perform(post("/consertos").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"dataEntrada":"11/11/2026","dataSaida":null,
                                 "mecanico":{"nome":"Lucas","anosExperiencia":2},
                                 "veiculo":{"marca":"Fiat","modelo":"Uno","ano":"2010","cor":"Branco"}}
                                """))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.ativo").value(true)).andReturn();
        Number id = JsonPath.read(cadastro.getResponse().getContentAsString(), "$.id");
        mvc.perform(get("/consertos").header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/consertos/algunsdados").header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id.intValue()));
        mvc.perform(get("/consertos/" + id).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mecanico.nome").value("Lucas"));
        mvc.perform(put("/consertos").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + id + ",\"dataSaida\":\"12/11/2026\","
                                + "\"nomeMecanico\":\"Guilherme\",\"anosExperiencia\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dataSaida").value("12/11/2026"))
                .andExpect(jsonPath("$.mecanico.nome").value("Guilherme"))
                .andExpect(jsonPath("$.mecanico.anosExperiencia").value(3));
        mvc.perform(delete("/consertos/" + id).header("Authorization", bearer))
                .andExpect(status().isNoContent());
        mvc.perform(get("/consertos/algunsdados").header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/consertos/" + id).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(get("/consertos/999999").header("Authorization", bearer))
                .andExpect(status().isNotFound());
    }

    private void verificarRejeicao(String token) throws Exception {
        mvc.perform(get("/consertos").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
