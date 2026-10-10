package br.com.template.core.config.advices;

import br.com.template.core.core.exceptions.InvalidFileException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class StorageAdvice {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidFileException.class)
    public String handleInvalidFile(InvalidFileException exception) {
        return exception.getMessage();
    }

    @ResponseStatus(HttpStatus.CONTENT_TOO_LARGE)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleTooLarge() {
        return "File exceeds the maximum upload size";
    }
}
