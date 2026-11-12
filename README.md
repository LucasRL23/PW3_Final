# PW3 Final — Oficina mecânica

Avaliação 4 de Programação para a Web III: adicionar segurança por token à API de consertos da Avaliação 3, conforme os conteúdos 12 e 13.

**Integrantes:** Guilherme Ozaki e Lucas Rodrigues.

## Tecnologias

Java 21, Spring Boot 4.0.3, Spring Security, JWT (Auth0), BCrypt, Spring Data JPA, Flyway e H2.

## Executar

É necessário ter um **JDK 21**. Basta abrir o projeto no IntelliJ e rodar a classe `Application`, ou pelo terminal:

```powershell
git clone https://github.com/LucasRL23/PW3_Final.git
cd PW3_Final
.\mvnw.cmd spring-boot:run
```

A API inicia em `http://localhost:8080`. As migrations criam/atualizam o banco automaticamente (pasta `DATA`, que não é enviada ao GitHub). A migration de usuários é V4 porque o projeto anterior tinha V1, V2 e V3; a migration de UUID mencionada nos slides não faz parte desta base.

### Usuário para testes

A migration `V5__insere-usuario-asdrubal.sql` cria o mesmo usuário usado nos slides, com a senha gravada em BCrypt:

- login: `asdrubal@gmail.com`
- senha: `123456`

### Chave do JWT (variável de ambiente)

Como mostrado no conteúdo 13, a senha principal que assina os tokens não fica no código-fonte. O `application.properties` lê a variável de ambiente `JWT_SECRET` e, se ela não existir, usa um valor padrão para poder rodar localmente:

```properties
pw3.senha.principal.geracao.token=${JWT_SECRET:chave-local-padrao-pw3-oficina-mecanica}
```

Para usar outra chave, crie a variável `JWT_SECRET` (no IntelliJ: *Edit Configurations → Environment variables*). Recomenda-se ao menos 32 caracteres. Trocar a chave invalida os tokens já gerados.

### Outros usuários (opcional)

Também é possível criar outro usuário ao iniciar, definindo `OFICINA_LOGIN` e `OFICINA_SENHA` no ambiente. Ele só é criado se o login ainda não existir, com a senha codificada em BCrypt. Não há cadastro público de usuários.

## Fazer login e usar o token

No Postman, envie `POST http://localhost:8080/login`, com body JSON:

```json
{
  "login": "asdrubal@gmail.com",
  "senha": "123456"
}
```

Resposta `200 OK`:

```json
{
  "token": "JWT-gerado-pela-api"
}
```

Copie o valor de `token`. Nos endpoints de consertos, selecione **Authorization → Bearer Token** e cole esse valor, ou envie o cabeçalho:

```http
Authorization: Bearer JWT-gerado-pela-api
```

O token dura **duas horas**. Depois disso, faça login novamente. A API não cria sessão: cada requisição de consertos precisa enviar o token.

## Endpoints

| Método | URL | Autenticação | Resultado de sucesso |
| --- | --- | --- | --- |
| POST | `/login` | Login e senha no JSON | 200 + token |
| POST | `/consertos` | Bearer JWT | 201 + dados e cabeçalho Location |
| GET | `/consertos?page=0&size=5&sort=id` | Bearer JWT | 200 + página com todos os dados |
| GET | `/consertos/algunsdados` | Bearer JWT | 200 + dados parciais dos ativos, sem paginação |
| GET | `/consertos/{id}` | Bearer JWT | 200 + dados do conserto |
| PUT | `/consertos` | Bearer JWT | 200 + dados atualizados |
| DELETE | `/consertos/{id}` | Bearer JWT | 204, exclusão lógica |

Somente `POST /login` é público. Sem token, com token inválido/expirado ou com credenciais incorretas, a resposta é **401**. Campos obrigatórios inválidos retornam **400**. Um ID inexistente retorna **404** quando a requisição está autenticada. O console web do H2 está desabilitado.

### Cadastrar conserto

`POST /consertos`, com Bearer Token e este JSON:

```json
{
  "dataEntrada": "11/11/2026",
  "dataSaida": null,
  "mecanico": {
    "nome": "Lucas",
    "anosExperiencia": 2
  },
  "veiculo": {
    "marca": "Fiat",
    "modelo": "Uno",
    "ano": "2010",
    "cor": "Branco"
  }
}
```

### Atualizar conserto

`PUT /consertos`, com Bearer Token:

```json
{
  "id": 1,
  "dataSaida": "12/11/2026",
  "nomeMecanico": "Guilherme",
  "anosExperiencia": 3
}
```

Substitua o ID pelo retornado no cadastro. Somente esses três dados podem ser alterados. Os campos de atualização omitidos preservam o valor anterior.

### Excluir logicamente

Envie `DELETE /consertos/1` com Bearer Token. O registro permanece no banco com `ativo=false` e deixa de aparecer em `/consertos/algunsdados`. A consulta completa e a consulta por ID mantêm o comportamento da Avaliação 3, incluindo registros inativos.

## Organização

- `usuario`: entidade `UserDetails`, repository, dados de login e criação do primeiro usuário.
- `security/AutenticacaoService`: consulta o usuário para o Spring autenticar a senha.
- `security/SecurityConfigurations`: configura BCrypt, sessão stateless, rotas e ordem do filtro.
- `security/PW3TokenService`: assina e valida JWT com emissor, login e expiração.
- `security/SecurityFilter`: lê `Authorization: Bearer ...`, valida o token e autentica o usuário da requisição.
- `controller/AutenticacaoController`: recebe o login e retorna `DadosTokenJWT`.
- `conserto`, `mecanico`, `veiculo`: preservam o CRUD anterior.

## Testes

```powershell
.\mvnw.cmd verify
```

Os testes usam H2 em memória e uma chave exclusiva de teste; não alteram o banco da aplicação. Verificam login (inclusive do usuário da migration), BCrypt, validade do JWT, ausência de sessão, acesso sem token, tokens inválidos/expirados, usuário removido e o CRUD completo autenticado.

O funcionamento segue o fluxo das aulas: `POST /login` → autenticação BCrypt → geração de JWT → envio do Bearer → validação do filtro → acesso ao controller.

Referências técnicas: [Spring Security](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/index.html) e [Auth0 Java JWT](https://github.com/auth0/java-jwt).
