# Regras de negócio

> Documento vivo. É aqui que mora **o que o produto faz e por quê** — não como o código está
> organizado (isso é `ARQUITETURA.md`) nem o contrato das rotas (isso é `API.md`).
> Toda regra nova entra aqui no mesmo PR que a implementa.

<!-- skill:regras:inicio -->
<!--
    A skill de geração do projeto preenche esta seção a partir da entrevista
    de regras de negócio. O que precisa estar aqui, por seção:

    ## 1. Domínio
    O que o produto faz, em uma frase. Quem usa. Qual é a fase (PoC, MVP, produção)
    e o que está deliberadamente fora de escopo nela.

    ## 2. Atores e papéis
    Cada role do enum Role: quem é essa pessoa no negócio e o que ela pode fazer.

    ## 3. <Entidade central>
    Uma seção por conceito do domínio: o que é, quais campos importam para a regra,
    quem é dono do dado, o que é obrigatório e o que não é, e o que NÃO pode acontecer.

    ## 4. Fluxo principal
    O caminho crítico do produto, ponta a ponta, do gatilho ao resultado.

    ## 5. Decisões e seus motivos
    Escolha que não é óbvia pelo código: por que este endpoint é POST, por que este
    dado não é armazenado, por que este limite existe. É o que evita que alguém
    "conserte" de volta mais tarde.
-->
<!-- skill:regras:fim -->

## Autenticação e conta (vem do template)

O que o esqueleto já garante, antes de qualquer regra de produto:

- **Cadastro** (`POST /api/auth/register`) cria o usuário com `ROLE_USER` e, por padrão
  (`app.auth.email-verification-required=true`), **não** devolve tokens: manda e-mail de
  verificação e o login só passa depois de confirmado. E-mail já cadastrado **não** é revelado
  ao chamador — o dono do e-mail recebe um aviso de tentativa e a resposta é a mesma de sucesso.
- **Login** (`POST /api/auth/login`) é limitado por janela deslizante gravada no banco, por e-mail
  e por origem (`app.auth.rate-limit.*`). E-mail desconhecido ainda gasta o tempo de verificação
  de senha, para a resposta não revelar se a conta existe.
- **Tokens** são JWT stateless (access + refresh), assinados em HS256, com `issuer` validado.
  Cada token carrega `sessionVersion`; incrementar o do usuário **revoga todas as sessões dele**.
- **Senha** é BCrypt. Troca de senha autenticada exige a senha atual, revoga todas as sessões e
  devolve um par de tokens novo.
- **Recuperação de senha** usa token de uso único, guardado apenas como hash SHA-256, com validade
  configurável; pedir um novo invalida os pendentes. A resposta é sempre a mesma, exista a conta ou não.
- **Exclusão de conta** exige a senha, apaga os tokens de auth e a linha do usuário.
- **Contas inativas** são desativadas por scheduler após `app.users.inactivity-months` sem login.
- Identificadores usados para rate limit (e-mail, IP) são gravados como **HMAC-SHA256**
  (`PseudonymHasher`), nunca em claro.
