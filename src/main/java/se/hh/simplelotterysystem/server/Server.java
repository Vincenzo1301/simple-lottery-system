package se.hh.simplelotterysystem.server;

import static se.hh.simplelotterysystem.enums.LoggingType.ERROR;
import static se.hh.simplelotterysystem.enums.LoggingType.INFO;
import static se.hh.simplelotterysystem.util.Logger.log;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import se.hh.simplelotterysystem.data.DrawingRegistrationRequest;
import se.hh.simplelotterysystem.data.DrawingRegistrationResponse;
import se.hh.simplelotterysystem.data.HistoricalDataRequest;
import se.hh.simplelotterysystem.service.LotteryService;

public class Server implements ClientSessionEventHandler {

  private final ServerSocket serverSocket;
  private final List<ClientSession> clientSessions;
  private final LotteryService lotteryService;

  public Server(int port, LotteryService lotteryService) {
    try {
      this.serverSocket = new ServerSocket(port);
      this.clientSessions = new ArrayList<>();
      this.lotteryService = lotteryService;
    } catch (IOException e) {
      throw new RuntimeException("[ERROR]: Failed to start server on port " + port, e);
    }
  }

  public void start() {
    while (true) {
      try {
        ClientSession clientSession = new ClientSession(serverSocket.accept(), this);
        clientSessions.add(clientSession);
        clientSession.start();
      } catch (IOException e) {
        log(ERROR, "Failed to accept client connection: " + e.getMessage());
      }
    }
  }

  @Override
  public DrawingRegistrationResponse onDrawingRegistration(ClientSession session, DrawingRegistrationRequest request) {
    return lotteryService.drawingRegistration(request);
  }

  @Override
  public void onRequestHistoricalData(ClientSession session, HistoricalDataRequest request) {
    // TODO: Handle historical data request
  }

  @Override
  public void onClientSessionClosed(ClientSession session) {
    clientSessions.remove(session);
  }
}
