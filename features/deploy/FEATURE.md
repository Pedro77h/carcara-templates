# feature: deploy

Deploy em instância única na AWS EC2: `systemd` + nginx, com o jar publicado por SSH a cada push
em `main`.

É uma escolha de infraestrutura, não parte do esqueleto — por isso é opcional. Projeto que vai
para ECS, Kubernetes, Render, Fly.io ou que ainda não tem destino definido simplesmente não leva
esta feature, e o base continua completo sem ela.

## O que entra

| Arquivo | Papel |
|---|---|
| `DEPLOY.md` | provisionamento, `.env` do servidor, secrets, verificação e armadilhas |
| `deploy/setup-ec2.sh` | bootstrap da instância: Java 21, Docker, nginx, usuário de serviço |
| `deploy/core-app.service` | unidade `systemd` (perfil `dev` por default, `EnvironmentFile`) |
| `deploy/nginx-core-app.conf` | proxy de `/api/`, `/v3/api-docs` e `/swagger-ui/`, com os cabeçalhos de encaminhamento |
| `.github/workflows/deploy.yml` | build + `scp` do jar + restart do serviço, a cada push em `main` |
| `appends/README.md` | acrescenta a seção **Deploy** ao `README.md` do projeto |

O `pr-review.yml` **não** faz parte desta feature: fica no base, porque revisão de PR não depende
de para onde o projeto vai.

## Secrets necessários

`EC2_HOST`, `EC2_USER` e `EC2_SSH_KEY` no repositório (Settings → Secrets → Actions). Sem eles o
workflow falha no primeiro push em `main` — configure antes de ligar a feature, ou desabilite o
workflow até ter a instância.

## Atenção

O nginx envia `X-Forwarded-For`/`X-Real-IP` e os perfis `dev`/`prod` ligam
`server.forward-headers-strategy=framework`. Isso **não é cosmético**: o rate limit de login conta
tentativas por origem, e sem os cabeçalhos `getRemoteAddr()` devolve o IP do proxy para todo
mundo — o limite por origem viraria um limite global, em silêncio.

Se você trocar o nginx por outro proxy (ALB, Cloudflare, Traefik), confira que o cabeçalho continua
chegando.
