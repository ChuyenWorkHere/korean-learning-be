package edu.language.kbee.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class ResourceNotFoundException extends RuntimeException {
  private String message;

  private HttpStatus status;

  private String[] params;

  public ResourceNotFoundException(String message) {
    super(message);
    this.status = HttpStatus.NOT_FOUND;
    this.message = message;
  }

  public ResourceNotFoundException(String message, HttpStatus status) {
    super(message);
    this.status = status;
    this.message = message;
  }

  public ResourceNotFoundException(String message, String[] params) {
    super(message);
    this.status = HttpStatus.NOT_FOUND;
    this.message = message;
    this.params = params;
  }

  public ResourceNotFoundException(HttpStatus status, String message, String[] params) {
    super(message);
    this.status = status;
    this.message = message;
    this.params = params;
  }
}
