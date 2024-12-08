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
import se.hh.simplelotterysystem.data.DrawingRegistrationRequest;
import se.hh.simplelotterysystem.data.DrawingRegistrationResponse;
import se.hh.simplelotterysystem.data.HistoricalDataDto;
import se.hh.simplelotterysystem.data.HistoricalDataRequest;
import se.hh.simplelotterysystem.data.HistoricalDataResponse;
import se.hh.simplelotterysystem.job.DrawingJob;
import se.hh.simplelotterysystem.job.data.DrawingJobResult;
import se.hh.simplelotterysystem.model.LotteryHistory;
import se.hh.simplelotterysystem.service.LotteryService;

public class LotteryServiceImpl implements LotteryService {

  private static final double PARTICIPATION_FEE = 100.0;

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
  public DrawingRegistrationResponse drawingRegistration(
      DrawingRegistrationRequest registrationRequest) {
    String email = registrationRequest.email();
    Set<Integer> numbers = registrationRequest.drawingNumbers();
    LocalDateTime dateTime = registrationRequest.dateTime();

    DrawingRegistrationResponse validationResponse = validateRequest(email, numbers, dateTime);
    if (validationResponse != null) {
      return validationResponse;
    }

    registerDrawing(email, numbers, dateTime);

    return new DrawingRegistrationResponse(200, "Registration successful");
  }

  @Override
  public HistoricalDataResponse retrieveHistoricalData(HistoricalDataRequest request) {
    LocalDateTime startLocalDateTime = request.startTimestamp().truncatedTo(ChronoUnit.MINUTES);
    LocalDateTime endLocalDateTime = request.endTimestamp().truncatedTo(ChronoUnit.MINUTES);
    Map<LocalDateTime, HistoricalDataDto> historicalData =
        getHistoricalData(startLocalDateTime, endLocalDateTime);

    return new HistoricalDataResponse(200, "Historical data retrieved", historicalData);
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

    // eyery hour: 0 0 * * * ?
    // every minute: 0 * * * * ?

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
    updateHistory(result.timestamp(), result.winners(), result.luckyNumber());
    adjustAwardAmounts(result.timestamp(), result.winners().size());
    notifyWinners(result.timestamp(), result.winners());
  }

  private DrawingRegistrationResponse validateRequest(
      String email, Set<Integer> numbers, LocalDateTime dateTime) {
    if (dateTime.isBefore(now())) {
      return new DrawingRegistrationResponse(400, "Cannot register for past drawing");
    }

    if (numbers.stream().anyMatch(number -> number < 1 || number > 255)) {
      return new DrawingRegistrationResponse(400, "Number out of range");
    }

    List<Map<String, Set<Integer>>> slots = drawingSlots.get(dateTime);
    if (slots != null) {
      if (slots.stream()
          .anyMatch(
              slot ->
                  slot.containsKey(email)
                      && slot.get(email).stream().anyMatch(numbers::contains))) {
        return new DrawingRegistrationResponse(400, "Number(s) already registered");
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

  private Map<LocalDateTime, HistoricalDataDto> getHistoricalData(
      LocalDateTime startLocalDateTime, LocalDateTime endLocalDateTime) {
    Map<LocalDateTime, HistoricalDataDto> historicalData = new HashMap<>();
    List<LocalDateTime> localDateTimes = getLocalDateTimes(startLocalDateTime, endLocalDateTime);
    for (LocalDateTime localDateTime : localDateTimes) {
      if (history.containsKey(localDateTime)) {
        LotteryHistory lotteryHistory = history.get(localDateTime);
        HistoricalDataDto historicalDataDto =
            new HistoricalDataDto(
                lotteryHistory.drawnLuckyNumber() == null ? -1 : lotteryHistory.drawnLuckyNumber(),
                lotteryHistory.winners().size(),
                lotteryHistory.prizePool());
        historicalData.put(localDateTime, historicalDataDto);
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

  private void updateHistory(LocalDateTime timestamp, List<String> winners, Integer drawnLuckyNumber) {
    history.put(timestamp, new LotteryHistory(winners, drawnLuckyNumber, awardAmounts.get(timestamp)));
    log(INFO, "History updated!");
  }

  private void adjustAwardAmounts(LocalDateTime timestamp, int amountOfWinner) {
    if (awardAmounts.containsKey(timestamp) && amountOfWinner > 0) {
      log(INFO, "The winners are: " + amountOfWinner);

    } else if (awardAmounts.containsKey(timestamp) && amountOfWinner == 0) {
      log(INFO, "No winners this time! The money will be carried over to the next drawing.");

      double awardAmount = awardAmounts.get(timestamp);

      LocalDateTime updatedLocalDateTime = timestamp.plusHours(1);

      awardAmounts.put(updatedLocalDateTime, awardAmounts.get(updatedLocalDateTime) + awardAmount);
    } else {
      log(INFO, "No people registered for this time slot and no money to carry over.");
    }
  }

  private void notifyWinners(LocalDateTime timestamp, List<String> winners) {
    // TODO: Notify winners via email
  }
}
