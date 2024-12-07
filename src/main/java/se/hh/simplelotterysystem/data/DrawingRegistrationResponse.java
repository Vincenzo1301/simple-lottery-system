package se.hh.simplelotterysystem.data;

import java.io.Serializable;

public record DrawingRegistrationResponse(int status, String message) implements Serializable {}
