package se.hh.simplelotterysystem.model;

import java.util.List;

public record LotteryHistory(List<String> winners, Integer drawnLuckyNumber, double prizePool) {}
