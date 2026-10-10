package br.com.template.core.core.services.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Ponto de troca do armazenamento de arquivo. Hoje só existe {@link LocalFileStorageService};
 * um provider de nuvem (S3, GCS) entra como outra implementação ligada por {@code app.storage.provider}.
 * Grave a {@code key} devolvida na entidade dona do arquivo — nunca o caminho em disco.
 */
public interface FileStorageService {

    StoredFile store(MultipartFile file, String pathPrefix);

    Resource load(String key);

    void delete(String key);
}
