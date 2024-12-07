package se.hh.simplelotterysystem.server;

import se.hh.simplelotterysystem.data.DrawingRegistrationRequest;
import se.hh.simplelotterysystem.data.DrawingRegistrationResponse;
import se.hh.simplelotterysystem.data.HistoricalDataRequest;

public interface ClientSessionEventHandler {

  DrawingRegistrationResponse onDrawingRegistration(ClientSession session, DrawingRegistrationRequest request);

  void onRequestHistoricalData(ClientSession session, HistoricalDataRequest request);

  void onClientSessionClosed(ClientSession session);
}
