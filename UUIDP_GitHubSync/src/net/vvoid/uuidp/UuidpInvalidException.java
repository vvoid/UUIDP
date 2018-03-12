package net.vvoid.uuidp;

/**
 *
 * @author Dr. Daniel Georg Kirschner <Mail@vvoid.net>
 */
public class UuidpInvalidException extends Exception {

  public UuidpInvalidException() {
  }

  public UuidpInvalidException(String message) {
    super(message);
  }

  public UuidpInvalidException(Throwable cause) {
    super(cause);
  }

  public UuidpInvalidException(String message, Throwable cause) {
    super(message, cause);
  }

}
