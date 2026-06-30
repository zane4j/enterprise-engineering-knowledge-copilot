package io.github.zane4j.copilot.api.error;

import io.github.zane4j.copilot.common.DomainException;
import io.github.zane4j.copilot.storage.ObjectStorageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> handleDomainException(DomainException exception) {
        HttpStatus status = switch (exception.getCode()) {
            case "KNOWLEDGE_BASE_ACCESS_DENIED", "KNOWLEDGE_BASE_WRITE_FORBIDDEN" -> HttpStatus.FORBIDDEN;
            case "INGESTION_JOB_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "FILE_EMPTY", "FILE_TOO_LARGE", "FILE_TYPE_UNSUPPORTED", "FILE_MEDIA_TYPE_UNSUPPORTED",
                 "FILE_NAME_MISSING", "FILE_NAME_INVALID", "FILE_EXTENSION_MISSING", "FILE_READ_FAILED" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle("Document API request failed");
        problem.setProperty("code", exception.getCode());
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ProblemDetail> handleMaxUploadSizeExceeded() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Document file exceeds the configured upload limit");
        problem.setTitle("Document upload rejected");
        problem.setProperty("code", "FILE_TOO_LARGE");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(problem);
    }

    @ExceptionHandler(ObjectStorageException.class)
    ResponseEntity<ProblemDetail> handleObjectStorageException(ObjectStorageException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Document storage is temporarily unavailable");
        problem.setTitle("Object storage unavailable");
        problem.setProperty("code", "OBJECT_STORAGE_UNAVAILABLE");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }
}
