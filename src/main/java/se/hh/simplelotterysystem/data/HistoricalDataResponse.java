package se.hh.simplelotterysystem.data;


import java.util.Map;

public record HistoricalDataResponse(Map<String, HistoricalDataDto> historicalData) {}
