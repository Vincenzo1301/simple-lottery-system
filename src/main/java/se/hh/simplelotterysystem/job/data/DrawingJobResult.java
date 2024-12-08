package se.hh.simplelotterysystem.job.data;

import java.time.LocalDateTime;
import java.util.List;

public record DrawingJobResult(LocalDateTime timestamp, List<String> winners, Integer luckyNumber) {}
