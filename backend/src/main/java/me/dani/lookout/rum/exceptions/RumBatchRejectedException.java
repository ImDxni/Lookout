package me.dani.lookout.rum.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RumBatchRejectedException extends RuntimeException {
    private final HttpStatus status;

    public RumBatchRejectedException(HttpStatus status, String reason) {
        super(reason);
        this.status = status;
    }
}
