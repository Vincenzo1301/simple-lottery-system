package se.hh.simplelotterysystem.server;

import static se.hh.simplelotterysystem.enums.LoggingType.ERROR;
import static se.hh.simplelotterysystem.enums.LoggingType.INFO;
import static se.hh.simplelotterysystem.enums.LoggingType.WARNING;
import static se.hh.simplelotterysystem.util.Logger.log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import se.hh.simplelotterysystem.data.DrawingRegistrationRequest;
import se.hh.simplelotterysystem.data.HistoricalDataRequest;

public class ClientSession extends Thread {

  private final Socket clientSocket;
  private final ObjectOutputStream out;
  private final ObjectInputStream in;
  private final ClientSessionEventHandler eventHandler;

  public ClientSession(Socket accept, ClientSessionEventHandler eventHandler) {
    try {
      this.clientSocket = accept;
      this.out = new ObjectOutputStream(clientSocket.getOutputStream());
      this.in = new ObjectInputStream(clientSocket.getInputStream());
      this.eventHandler = eventHandler;
    } catch (Exception e) {
      throw new RuntimeException("[ERROR]: Failed to create client session", e);
    }
  }

  @Override
  public void run() {
    try {
      while (true) {
        Object input = in.readObject();
        if (input instanceof DrawingRegistrationRequest request) {
          eventHandler.onDrawingRegistration(this, request);
        } else if (input instanceof HistoricalDataRequest request) {
          eventHandler.onRequestHistoricalData(this, request);
        } else {
          log(WARNING, "Unknown request received from client.");
        }
      }
    } catch (IOException e) {
      log(INFO, "Client disconnected.");
      eventHandler.onClientSessionClosed(this);
    } catch (Exception e) {
      log(ERROR, "Something went wrong: " + e.getMessage());
    }
  }
}
