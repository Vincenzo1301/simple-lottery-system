package se.hh.simplelotterysystem.data;

import java.time.LocalDateTime;
import java.util.Set;

public record DrawingRegistrationRequest(
    String email, LocalDateTime dateTime, Set<Integer> drawingNumbers) {}
