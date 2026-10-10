
## Deploy

O serviço roda como unidade `systemd` atrás do nginx, e o jar é publicado por SSH a cada push em
`main` pelo workflow `.github/workflows/deploy.yml`. Passo a passo (provisionamento, `.env` do
servidor, secrets do repositório e verificação) em [`DEPLOY.md`](DEPLOY.md).
