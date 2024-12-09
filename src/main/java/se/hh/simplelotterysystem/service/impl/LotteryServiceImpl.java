package se.hh.simplelotterysystem.service.impl;

import static java.time.LocalDateTime.now;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static se.hh.simplelotterysystem.enums.LoggingType.ERROR;
import static se.hh.simplelotterysystem.enums.LoggingType.INFO;
import static se.hh.simplelotterysystem.util.Logger.log;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import se.hh.simplelotterysystem.data.DrawingRegistrationRequest;
import se.hh.simplelotterysystem.data.DrawingRegistrationResponse;
import se.hh.simplelotterysystem.data.HistoricalDataDto;
import se.hh.simplelotterysystem.data.HistoricalDataResponse;
import se.hh.simplelotterysystem.email.GmailSender;
import se.hh.simplelotterysystem.job.DrawingJob;
import se.hh.simplelotterysystem.job.data.DrawingJobResult;
import se.hh.simplelotterysystem.model.LotteryHistory;
import se.hh.simplelotterysystem.service.LotteryService;

@Service
public class LotteryServiceImpl implements LotteryService {

  private static final double PARTICIPATION_FEE = 100.0;

  private final GmailSender gmailSender = new GmailSender();

  private final Scheduler scheduler;
  private final Map<LocalDateTime, List<Map<String, Set<Integer>>>> drawingSlots = new HashMap<>();
  private final Map<LocalDateTime, LotteryHistory> history = new HashMap<>();
  private final Map<LocalDateTime, Double> awardAmounts = new HashMap<>();

  public LotteryServiceImpl() {
    try {
      scheduler = initializeScheduler();
      scheduleDrawingJob();
      addJobListener();
    } catch (Exception e) {
      throw new RuntimeException("Could not start scheduler", e);
    }
  }

  @Override
  public ResponseEntity<DrawingRegistrationResponse> drawingRegistration(
      DrawingRegistrationRequest registrationRequest) {
    String email = registrationRequest.email();
    Set<Integer> numbers = registrationRequest.drawingNumbers();
    LocalDateTime dateTime = registrationRequest.dateTime();

    ResponseEntity<DrawingRegistrationResponse> validationResponse =
        validateRequest(email, numbers, dateTime);
    if (validationResponse != null) {
      return validationResponse;
    }

    registerDrawing(email, numbers, dateTime);

    return ResponseEntity.ok(new DrawingRegistrationResponse("Registration successful"));
  }

  @Override
  public ResponseEntity<HistoricalDataResponse> retrieveHistoricalData(
      LocalDateTime startTimestamp, LocalDateTime endTimestamp) {
    LocalDateTime startLocalDateTime = startTimestamp.truncatedTo(ChronoUnit.MINUTES);
    LocalDateTime endLocalDateTime = endTimestamp.truncatedTo(ChronoUnit.MINUTES);
    Map<String, HistoricalDataDto> historicalData =
        getHistoricalData(startLocalDateTime, endLocalDateTime);

    return ResponseEntity.ok(new HistoricalDataResponse(historicalData));
  }

  private Scheduler initializeScheduler() throws SchedulerException {
    Scheduler scheduler = new StdSchedulerFactory().getScheduler();
    scheduler.start();
    return scheduler;
  }

  private void scheduleDrawingJob() throws SchedulerException {
    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put("drawingSlots", drawingSlots);

    JobDetail job =
        JobBuilder.newJob(DrawingJob.class)
            .withIdentity("drawingJob", "lotteryGroup")
            .usingJobData(jobDataMap)
            .build();

    CronTrigger trigger =
        newTrigger()
            .withIdentity("drawingTrigger", "lotteryGroup")
            .withSchedule(cronSchedule("0 * * * * ?"))
            .forJob(job)
            .build();

    scheduler.scheduleJob(job, trigger);
  }

  private void addJobListener() throws SchedulerException {
    scheduler
        .getListenerManager()
        .addJobListener(
            new JobListener() {

              @Override
              public String getName() {
                return "DrawingJobListener";
              }

              @Override
              public void jobToBeExecuted(JobExecutionContext context) {
                // No-op
              }

              @Override
              public void jobExecutionVetoed(JobExecutionContext context) {
                // No-op
              }

              @Override
              public void jobWasExecuted(JobExecutionContext context, JobExecutionException e) {
                DrawingJobResult result = (DrawingJobResult) context.getResult();

                if (result != null) {
                  awardAmounts.putIfAbsent(result.timestamp().truncatedTo(ChronoUnit.MINUTES), 0.0);
                  handleDrawingJobResult(result);
                } else {
                  log(ERROR, "No result found for executed drawing job.");
                }
              }
            });
  }

  private void handleDrawingJobResult(DrawingJobResult result) {
    if (result.amountOfParticipants() != 0) {
      updateHistory(result.timestamp(), result.winners(), result.luckyNumber());
      if (!result.winners().isEmpty()) {
        notifyWinners(result.winners(), result.luckyNumber(), awardAmounts.get(result.timestamp()));
      }
      adjustAwardAmounts(result.timestamp(), result.winners().size());
    } else {
      log(INFO, "No participants for drawing at " + result.timestamp());
    }
  }

  private ResponseEntity<DrawingRegistrationResponse> validateRequest(
      String email, Set<Integer> numbers, LocalDateTime dateTime) {
    if (dateTime.isBefore(now())) {
      return ResponseEntity.badRequest()
          .body(new DrawingRegistrationResponse("Cannot register for past drawing"));
    }

    if (numbers.stream().anyMatch(number -> number < 1 || number > 255)) {
      return ResponseEntity.badRequest()
          .body(new DrawingRegistrationResponse("Number out of range"));
    }

    List<Map<String, Set<Integer>>> slots = drawingSlots.get(dateTime);
    if (slots != null) {
      if (slots.stream()
          .anyMatch(
              slot ->
                  slot.containsKey(email)
                      && slot.get(email).stream().anyMatch(numbers::contains))) {
        return ResponseEntity.badRequest()
            .body(new DrawingRegistrationResponse("Number(s) already registered"));
      }
    }

    return null; // No validation issues
  }

  private void registerDrawing(String email, Set<Integer> numbers, LocalDateTime dateTime) {
    drawingSlots.computeIfAbsent(dateTime, k -> new ArrayList<>());

    List<Map<String, Set<Integer>>> slots = drawingSlots.get(dateTime);
    Optional<Map<String, Set<Integer>>> existingSlot =
        slots.stream().filter(slot -> slot.containsKey(email)).findFirst();

    if (existingSlot.isPresent()) {
      existingSlot.get().get(email).addAll(numbers);
    } else {
      Map<String, Set<Integer>> participant = new HashMap<>();
      participant.put(email, new HashSet<>(numbers));
      slots.add(participant);
    }

    awardAmounts.putIfAbsent(dateTime, 0.0);
    awardAmounts.put(dateTime, awardAmounts.get(dateTime) + PARTICIPATION_FEE);
  }

  private Map<String, HistoricalDataDto> getHistoricalData(
      LocalDateTime startLocalDateTime, LocalDateTime endLocalDateTime) {
    Map<String, HistoricalDataDto> historicalData = new HashMap<>();
    List<LocalDateTime> localDateTimes = getLocalDateTimes(startLocalDateTime, endLocalDateTime);
    for (LocalDateTime localDateTime : localDateTimes) {
      if (history.containsKey(localDateTime)) {
        LotteryHistory lotteryHistory = history.get(localDateTime);
        HistoricalDataDto historicalDataDto =
            new HistoricalDataDto(
                lotteryHistory.drawnLuckyNumber() == null ? -1 : lotteryHistory.drawnLuckyNumber(),
                lotteryHistory.winners().size(),
                lotteryHistory.prizePool());
        historicalData.put(localDateTime.toString(), historicalDataDto);
      }
    }
    return historicalData;
  }

  private List<LocalDateTime> getLocalDateTimes(
      LocalDateTime startLocalDateTime, LocalDateTime endLocalDateTime) {
    List<LocalDateTime> localDateTimes = new ArrayList<>();
    LocalDateTime currentLocalDateTime = startLocalDateTime;
    while (currentLocalDateTime.isBefore(endLocalDateTime)) {
      localDateTimes.add(currentLocalDateTime);
      // TODO: currentLocalDateTime = currentLocalDateTime.plusHours(1);
      currentLocalDateTime = currentLocalDateTime.plusMinutes(1); // For testing purposes!!!
    }
    return localDateTimes;
  }

  private void updateHistory(
      LocalDateTime timestamp, List<String> winners, Integer drawnLuckyNumber) {
    history.put(
        timestamp, new LotteryHistory(winners, drawnLuckyNumber, awardAmounts.get(timestamp)));
    log(INFO, "History updated!");
  }

  private void adjustAwardAmounts(LocalDateTime timestamp, int amountOfWinner) {
    if (awardAmounts.containsKey(timestamp) && amountOfWinner > 0) {
      awardAmounts.put(timestamp, 0.0);
    } else if (awardAmounts.containsKey(timestamp) && amountOfWinner == 0) {
      log(INFO, "No winners this time! The money will be carried over to the next drawing.");

      double awardAmount = awardAmounts.get(timestamp);

      // TODO: LocalDateTime updatedLocalDateTime = timestamp.plusHours(1);
      LocalDateTime updatedLocalDateTime = timestamp.plusMinutes(1); // test purposes
      awardAmounts.putIfAbsent(updatedLocalDateTime, 0.0);
      awardAmounts.put(updatedLocalDateTime, awardAmounts.get(updatedLocalDateTime) + awardAmount);
    }
  }

  private void notifyWinners(List<String> winners, Integer drawnLuckyNumber, double prizePool) {
    log(
        INFO,
        "The winners are: "
            + winners
            + " with the lucky number: "
            + drawnLuckyNumber
            + " and the prize pool per person: "
            + prizePool / winners.size());
    // TODO: Notify winners via email

    for (String winner : winners) {
      try {
        gmailSender.sendEmail(
            winner,
            "🎉 Congratulations! You're a Winner! 🎉",
            "Hello "
                + winner
                + ",\n\n"
                + "Congratulations! You are one of the lucky winners of our contest!\n\n"
                + "The winning number was: "
                + drawnLuckyNumber
                + ".\n"
                + "Your share of the prize pool is: $"
                + (prizePool / winners.size())
                + ".\n\n"
                + "Thank you for participating, and we hope you enjoy your prize!\n\n"
                + "Best regards,\n"
                + "Your Simple Lottery team");
      } catch (Exception e) {
        throw new RuntimeException("Sending email failed: " + e.getMessage(), e);
      }
    }
    log(INFO, "Winners notified via email!");
  }
}
