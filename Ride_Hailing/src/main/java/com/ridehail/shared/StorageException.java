package com.ridehail.shared;

/** Infrastructure errors retain their cause and are not treated as business rejections. */
public final class StorageException extends RuntimeException {
  public StorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
