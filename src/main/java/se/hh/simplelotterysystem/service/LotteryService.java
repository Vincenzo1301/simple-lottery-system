package se.hh.simplelotterysystem.service;

import se.hh.simplelotterysystem.data.DrawingRegistrationRequest;
import se.hh.simplelotterysystem.data.DrawingRegistrationResponse;
import se.hh.simplelotterysystem.data.HistoricalDataRequest;
import se.hh.simplelotterysystem.data.HistoricalDataResponse;

public interface LotteryService {

  DrawingRegistrationResponse drawingRegistration(DrawingRegistrationRequest registrationRequest);

  HistoricalDataResponse retrieveHistoricalData(HistoricalDataRequest request);
}
