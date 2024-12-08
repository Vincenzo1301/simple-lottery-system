package se.hh.simplelotterysystem.data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

public record HistoricalDataResponse(
    int status, String message, Map<LocalDateTime, HistoricalDataDto> historicalData)
    implements Serializable {}
