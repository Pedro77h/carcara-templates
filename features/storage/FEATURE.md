# feature: storage

Armazenamento de arquivo atrás de uma interface, com a implementação em disco local. Veio do imob,
com três correções.

## O que entra

| Arquivo | Papel |
|---|---|
| `core/services/storage/FileStorageService` | `store`, `load`, `delete` — o ponto de troca para S3/GCS |
| `core/services/storage/LocalFileStorageService` | disco local, com guarda de path traversal |
| `core/services/storage/StoredFile` | `record (key, fileName, contentType, size)` |
| `core/exceptions/{InvalidFileException, FileStorageException}` | erro do cliente (`400`) vs. erro de I/O (`500`) |
| `config/advices/StorageAdvice` | `InvalidFileException` → `400`, upload grande demais → `413` |
| `properties-snippet.properties` | provider, diretório, limites de multipart |
| `appends/docs/arquitetura/ARQUITETURA.md` | a seção "Arquivos" |

Não substitui nenhum arquivo do base. Não traz controller: upload e download pertencem ao módulo
dono do arquivo, que é quem sabe quem pode ver o quê.

## O que mudou em relação ao imob

- **Sem `url` em `StoredFile`.** O imob devolvia `file:///tmp/imob-uploads/...` — o caminho
  absoluto do servidor, inútil para o cliente e informação de graça para quem ataca. Download
  passa a ser `load(key)` atrás de um endpoint com permissão.
- **`load(key)`** existe. Sem ele, o arquivo guardado não tinha como voltar.
- **Erro do cliente separado de erro nosso.** Arquivo vazio ou chave que escapa do diretório era
  `FileStorageException` como uma falha de disco; agora é `InvalidFileException` (`400`).
- Nome de arquivo com `\` (vindo de Windows) também é reduzido ao último segmento.

## Path traversal

A guarda que vale é uma só: o caminho final, normalizado, precisa continuar **dentro** do diretório
base — em `store`, `load` e `delete`. O `replace("..", "")` que o imob fazia no prefixo saiu:
mexer na string não é guarda e só dá falsa sensação de segurança. A checagem do caminho final já
recusa qualquer escape, venha de onde vier.

## Produção

`app.storage.local-dir` default é `${java.io.tmpdir}/<app>-uploads`: **some no reboot**. Em
produção aponte `STORAGE_LOCAL_DIR` para um volume persistente, ou implemente o provider de nuvem
(`@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")`) antes de ir ao ar.
Com a feature `deploy`, inclua o diretório no backup.

O `@Value` traz um default próprio (`${java.io.tmpdir}/uploads`) porque o
`src/test/resources/application.properties` substitui o principal e o snippet não chega aos
testes — ver `ACH-20261009-001`.

## Testes

`LocalFileStorageServiceTest`: guarda e lê de volta, nome com diretório (Unix e Windows) reduzido,
prefixo e chave que escapam recusados nos três métodos, arquivo vazio recusado, apagar e não achar
mais. `StorageAdviceTest` cobre a tradução de status.
