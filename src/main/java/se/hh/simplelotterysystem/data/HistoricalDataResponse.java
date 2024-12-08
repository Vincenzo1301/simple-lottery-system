package se.hh.simplelotterysystem.data;

import java.time.LocalDateTime;
import java.util.Map;

public record HistoricalDataResponse(Map<LocalDateTime, HistoricalDataDto> historicalData) {}
