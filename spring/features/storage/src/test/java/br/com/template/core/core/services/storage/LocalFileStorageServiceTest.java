package br.com.template.core.core.services.storage;

import br.com.template.core.core.exceptions.InvalidFileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {

    @TempDir
    Path baseDir;

    private LocalFileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new LocalFileStorageService(baseDir.resolve("uploads").toString());
    }

    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf", "content".getBytes());
    }

    @Test
    @DisplayName("should store the file under the prefix with a unique key and load it back")
    void shouldStoreAndLoad() throws Exception {
        StoredFile stored = fileStorageService.store(pdf("contrato.pdf"), "users/1");

        assertThat(stored.key()).startsWith("users/1/").endsWith("-contrato.pdf");
        assertThat(stored.fileName()).isEqualTo("contrato.pdf");
        assertThat(stored.size()).isEqualTo(7);
        assertThat(fileStorageService.load(stored.key()).getContentAsByteArray()).isEqualTo("content".getBytes());
    }

    @Test
    @DisplayName("should keep only the last segment of a client supplied file name")
    void shouldStripDirectoriesFromFileName() {
        StoredFile unix = fileStorageService.store(pdf("../../etc/passwd"), "p");
        StoredFile windows = fileStorageService.store(pdf("..\\..\\boot.ini"), "p");

        assertThat(unix.fileName()).isEqualTo("passwd");
        assertThat(windows.fileName()).isEqualTo("boot.ini");
        assertThat(baseDir.resolve("uploads").resolve(unix.key())).exists();
    }

    @Test
    @DisplayName("should refuse a prefix or key that escapes the storage directory")
    void shouldRefusePathTraversal() {
        assertThatThrownBy(() -> fileStorageService.store(pdf("a.pdf"), "../fora"))
                .isInstanceOf(InvalidFileException.class);
        assertThatThrownBy(() -> fileStorageService.load("../../etc/passwd"))
                .isInstanceOf(InvalidFileException.class);
        assertThatThrownBy(() -> fileStorageService.delete("users/../../fora.pdf"))
                .isInstanceOf(InvalidFileException.class);
        assertThatThrownBy(() -> fileStorageService.load(""))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    @DisplayName("should refuse an empty file")
    void shouldRefuseEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> fileStorageService.store(empty, "p")).isInstanceOf(InvalidFileException.class);
    }

    @Test
    @DisplayName("should delete a stored file and answer 'not found' when loading it afterwards")
    void shouldDelete() {
        StoredFile stored = fileStorageService.store(pdf("a.pdf"), "p");

        fileStorageService.delete(stored.key());

        assertThat(baseDir.resolve("uploads").resolve(stored.key())).doesNotExist();
        assertThatThrownBy(() -> fileStorageService.load(stored.key())).isInstanceOf(InvalidFileException.class);
    }
}
