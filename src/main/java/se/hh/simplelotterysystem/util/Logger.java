package se.hh.simplelotterysystem.util;

import static java.time.OffsetDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;

import java.time.format.DateTimeFormatter;
import se.hh.simplelotterysystem.enums.LoggingType;

public class Logger {

  private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

  /**
   * Logs a message with a timestamp and a logging type.
   *
   * @param type the type of the log message
   * @param message the message to log
   */
  public static void log(LoggingType type, String message) {
    DateTimeFormatter formatter = ofPattern(TIME_FORMAT);
    String timestamp = now().format(formatter);
    System.out.printf("[%s] %s: %s%n", timestamp, type, message);
  }
}
