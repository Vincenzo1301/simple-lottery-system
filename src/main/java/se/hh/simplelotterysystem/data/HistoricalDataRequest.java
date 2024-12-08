package se.hh.simplelotterysystem.data;

import java.io.Serializable;
import java.time.LocalDateTime;

public record HistoricalDataRequest(LocalDateTime startTimestamp, LocalDateTime endTimestamp)
    implements Serializable {}
