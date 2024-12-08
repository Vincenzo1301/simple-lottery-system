package se.hh.simplelotterysystem.service;

import java.time.LocalDateTime;
import org.springframework.http.ResponseEntity;
import se.hh.simplelotterysystem.data.DrawingRegistrationRequest;
import se.hh.simplelotterysystem.data.DrawingRegistrationResponse;
import se.hh.simplelotterysystem.data.HistoricalDataResponse;

public interface LotteryService {

  ResponseEntity<DrawingRegistrationResponse> drawingRegistration(
      DrawingRegistrationRequest registrationRequest);

  ResponseEntity<HistoricalDataResponse> retrieveHistoricalData(
      LocalDateTime startTimestamp, LocalDateTime endTimestamp);
}
