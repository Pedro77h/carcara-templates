
## Arquivos (feature `storage`)

Upload passa pelo `FileStorageService` (`core/services/storage`), chamado pelo **service** do
módulo dono do arquivo. A entidade guarda a `key` devolvida — nunca caminho de disco nem URL — e é
ela que decide quem pode ler: o download sai por um endpoint do módulo, que confere a permissão e
devolve `fileStorageService.load(key)`.

`fileName` e `contentType` de `StoredFile` vêm do cliente: servem para exibir. Se o tipo importa
(só PDF, só imagem), valide o conteúdo. Limite de tamanho em `spring.servlet.multipart.*`; estouro
vira `413`.

Apagar a entidade e o arquivo não é atômico: apague o arquivo **depois** do commit
(`@TransactionalEventListener(AFTER_COMMIT)`), senão um rollback deixa a linha apontando para nada.
