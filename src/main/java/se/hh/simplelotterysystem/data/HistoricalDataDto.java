package se.hh.simplelotterysystem.data;

import java.io.Serializable;

public record HistoricalDataDto(int luckyNumber, int amountOfWinners, double prizePool)
    implements Serializable {}
