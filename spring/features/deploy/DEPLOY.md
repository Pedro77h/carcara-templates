# Deploy (AWS EC2 + GitHub Actions)

O serviço roda como `systemd` unit atrás do nginx, e o jar é publicado por SSH pelo workflow
`.github/workflows/deploy.yml` a cada push em `main`.

## 1. Provisionar a instância

Ubuntu 22.04+, com as portas 22 (SSH, restrita ao seu IP) e 80/443 (HTTP) abertas.

## 2. Bootstrap do servidor

```bash
scp -r deploy ubuntu@<host>:~/
ssh ubuntu@<host> 'bash ~/deploy/setup-ec2.sh'
```

Instala Java 21, Docker, nginx; cria o usuário de serviço `core`, o diretório `/opt/core-app` e
publica o `nginx-core-app.conf`.

## 3. Banco e variáveis de ambiente

```bash
scp docker-compose.yml ubuntu@<host>:/tmp/
ssh ubuntu@<host> 'sudo mv /tmp/docker-compose.yml /opt/core-app/ && cd /opt/core-app && sudo docker compose up -d'
```

Crie `/opt/core-app/.env` (dono `core`, permissão `600`) com, no mínimo:

```
SPRING_PROFILES_ACTIVE=dev
APP_NAME=core
POSTGRES_HOST=localhost
POSTGRES_DB=core
POSTGRES_USER=...
POSTGRES_PASSWORD=...
JWT_SECRET=...                 # base64, >= 32 bytes
JWT_ISSUER=core
PSEUDONYM_SECRET=...           # obrigatorio em prod
CORS_ALLOWED_ORIGINS=https://...
FRONTEND_RESET_PASSWORD_URL=https://.../reset-password
FRONTEND_VERIFY_EMAIL_URL=https://.../verificar-email
MAIL_HOST=...
MAIL_PORT=587
MAIL_USERNAME=...
MAIL_PASSWORD=...
SEED_ADMIN_EMAIL=...           # só nos perfis local/dev
SEED_ADMIN_PASSWORD_HASH=...   # hash BCrypt, nunca a senha
```

Gerar o segredo JWT: `openssl rand -base64 48`.

## 4. Instalar o serviço

```bash
ssh ubuntu@<host> '
  sudo cp ~/deploy/core-app.service /etc/systemd/system/
  sudo systemctl daemon-reload
  sudo systemctl enable --now core-app.service
'
```

## 5. Ligar o deploy automático

Secrets do repositório (Settings → Secrets → Actions):

| Secret | Conteúdo |
|---|---|
| `EC2_HOST` | IP ou DNS da instância |
| `EC2_USER` | usuário de SSH (ex.: `ubuntu`) |
| `EC2_SSH_KEY` | chave privada com acesso à instância |
| `ANTHROPIC_API_KEY` | opcional, e não é do deploy: habilita a revisão arquitetural em `pr-review.yml` |

## Verificação

```bash
curl http://<host>/api/ping
ssh ubuntu@<host> 'sudo systemctl status core-app.service --no-pager'
ssh ubuntu@<host> 'sudo journalctl -u core-app.service -n 50 --no-pager'
```

## Cabeçalhos de proxy são obrigatórios

O rate limit de login e de recuperação de senha conta tentativas por **origem**. Atrás do nginx,
`request.getRemoteAddr()` devolve `127.0.0.1` para todo mundo — o que faz o limite por origem
valer para o servidor inteiro, não por cliente.

Por isso o `nginx-core-app.conf` envia `X-Forwarded-For`/`X-Real-IP` e os perfis `dev` e `prod`
ligam `server.forward-headers-strategy=framework`. **Mexer num sem o outro quebra o rate limit em
silêncio.**

## Logs do nginx: IP é dado pessoal

O `access_log` grava a URL completa ao lado do IP, e trava de logger da aplicação não alcança o
servidor web. Nada sensível pode ir em query param ou path de URL — use corpo de `POST`.

## Cuidados

- Migration já aplicada **nunca** é editada: o Flyway recusa o boot por divergência de checksum.
- `ddl-auto` fica em `validate`. Entidade sem migration correspondente derruba a aplicação no start
  — o que é o comportamento desejado.
- Perfil `prod` não carrega `db/migration-dev`: nada de seed de demonstração em produção.
- Variável nova no `.env` precisa de `sudo systemctl restart core-app.service`.
