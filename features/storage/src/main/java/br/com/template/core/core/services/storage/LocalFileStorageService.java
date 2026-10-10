package br.com.template.core.core.services.storage;

import br.com.template.core.core.exceptions.FileStorageException;
import br.com.template.core.core.exceptions.InvalidFileException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final Path baseDirectory;

    public LocalFileStorageService(@Value("${app.storage.local-dir:${java.io.tmpdir}/uploads}") String baseDir) {
        this.baseDirectory = Paths.get(baseDir).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(MultipartFile file, String pathPrefix) {
        if (Objects.isNull(file) || file.isEmpty()) {
            throw new InvalidFileException("Cannot store an empty file");
        }

        String fileName = sanitizeFileName(file.getOriginalFilename());
        String key = buildKey(pathPrefix, fileName);
        Path destination = resolveDestination(key);

        try {
            Files.createDirectories(destination.getParent());
            file.transferTo(destination);
        } catch (IOException exception) {
            throw new FileStorageException("Failed to store file " + key, exception);
        }

        return new StoredFile(key, fileName, file.getContentType(), file.getSize());
    }

    @Override
    public Resource load(String key) {
        Path path = resolveDestination(key);

        if (!Files.isRegularFile(path)) {
            throw new InvalidFileException("File not found");
        }

        return new PathResource(path);
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolveDestination(key));
        } catch (IOException exception) {
            throw new FileStorageException("Failed to delete file " + key, exception);
        }
    }

    private String sanitizeFileName(String originalFilename) {
        if (Objects.isNull(originalFilename) || originalFilename.isBlank()) {
            return "file";
        }

        Path name = Paths.get(originalFilename.replace('\\', '/')).getFileName();

        return Objects.isNull(name) ? "file" : name.toString();
    }

    private String buildKey(String pathPrefix, String fileName) {
        String uniqueName = UUID.randomUUID() + "-" + fileName;

        return Objects.isNull(pathPrefix) || pathPrefix.isBlank() ? uniqueName : pathPrefix.strip() + "/" + uniqueName;
    }

    // A única guarda que importa contra path traversal: o caminho final, já normalizado,
    // precisa continuar dentro do diretório base. Vale para store, load e delete.
    private Path resolveDestination(String key) {
        Path destination = baseDirectory.resolve(key).normalize();

        if (!destination.startsWith(baseDirectory) || destination.equals(baseDirectory)) {
            throw new InvalidFileException("Resolved path escapes the storage directory");
        }

        return destination;
    }
}
