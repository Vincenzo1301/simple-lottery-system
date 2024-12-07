package se.hh.simplelotterysystem.service;

import se.hh.simplelotterysystem.data.DrawingRegistrationRequest;
import se.hh.simplelotterysystem.data.DrawingRegistrationResponse;

public interface LotteryService {

  DrawingRegistrationResponse drawingRegistration(DrawingRegistrationRequest registrationRequest);

  void retrieveHistoricalData();
}
