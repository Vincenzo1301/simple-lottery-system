package se.hh.simplelotterysystem.util;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import se.hh.simplelotterysystem.enums.LoggingType;

public class Logger {
  public static void log(LoggingType type, String message) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    String timestamp = OffsetDateTime.now().format(formatter);
    System.out.printf("[%s] %s: %s%n", timestamp, type, message);
  }
}
