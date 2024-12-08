package se.hh.simplelotterysystem.resource;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.hh.simplelotterysystem.data.DrawingRegistrationRequest;
import se.hh.simplelotterysystem.data.DrawingRegistrationResponse;
import se.hh.simplelotterysystem.data.HistoricalDataResponse;
import se.hh.simplelotterysystem.service.LotteryService;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/lottery")
public class LotteryRestController {

  private final LotteryService lotteryService;

  public LotteryRestController(LotteryService lotteryService) {
    this.lotteryService = lotteryService;
  }

  @PostMapping
  public ResponseEntity<DrawingRegistrationResponse> registerDrawing(
      @RequestBody DrawingRegistrationRequest request) {
    return lotteryService.drawingRegistration(request);
  }

  @GetMapping
  public ResponseEntity<HistoricalDataResponse> retrieveHistoricalData(
      @RequestParam LocalDateTime startTimestamp, @RequestParam LocalDateTime endTimestamp) {
    return lotteryService.retrieveHistoricalData(startTimestamp, endTimestamp);
  }
}
