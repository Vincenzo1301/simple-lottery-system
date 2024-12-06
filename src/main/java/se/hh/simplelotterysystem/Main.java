package se.hh.simplelotterysystem;

import se.hh.simplelotterysystem.server.Server;
import se.hh.simplelotterysystem.service.LotteryService;
import se.hh.simplelotterysystem.service.LotteryServiceImpl;

public class Main {

  private static final int PORT = 8080;

  public static void main(String[] args) {
    LotteryService lotteryService = new LotteryServiceImpl();
    Server server = new Server(PORT, lotteryService);

    server.start();
  }
}
