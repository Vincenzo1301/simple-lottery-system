package se.hh.simplelotterysystem.server;

import se.hh.simplelotterysystem.data.DrawingRegistrationRequest;
import se.hh.simplelotterysystem.data.DrawingRegistrationResponse;
import se.hh.simplelotterysystem.data.HistoricalDataRequest;
import se.hh.simplelotterysystem.data.HistoricalDataResponse;

public interface ClientSessionEventHandler {

  DrawingRegistrationResponse onDrawingRegistration(
      ClientSession session, DrawingRegistrationRequest request);

  HistoricalDataResponse onRequestHistoricalData(
      ClientSession session, HistoricalDataRequest request);

  void onClientSessionClosed(ClientSession session);
}
