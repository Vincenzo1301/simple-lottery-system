package se.hh.simplelotterysystem.service.impl;

import static java.time.LocalDateTime.now;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static se.hh.simplelotterysystem.enums.LoggingType.INFO;
import static se.hh.simplelotterysystem.util.Logger.log;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import se.hh.simplelotterysystem.data.DrawingRegistrationRequest;
import se.hh.simplelotterysystem.data.DrawingRegistrationResponse;
import se.hh.simplelotterysystem.job.DrawingJob;
import se.hh.simplelotterysystem.job.DrawingJobResult;
import se.hh.simplelotterysystem.service.LotteryService;

public class LotteryServiceImpl implements LotteryService {

  private static final double PARTICIPATION_FEE = 100.0;

  private final Scheduler scheduler;
  private final Map<LocalDateTime, List<Map<String, Set<Integer>>>> drawingSlots = new HashMap<>();
  private final Map<LocalDateTime, Double> awardAmounts = new HashMap<>();

  public LotteryServiceImpl() {
    try {
      scheduler = new StdSchedulerFactory().getScheduler();
      scheduler.start();

      JobDataMap jobDataMap = new JobDataMap();
      jobDataMap.put("drawingSlots", drawingSlots);

      JobDetail job =
          JobBuilder.newJob(DrawingJob.class)
              .withIdentity("drawingJob", "lotteryGroup")
              .usingJobData(jobDataMap)
              .build();

      // every minute - 0 * * * * ?
      // every hour - 0 0 * * * ?
      CronTrigger trigger =
          newTrigger()
              .withIdentity("drawingTrigger", "lotteryGroup")
              .withSchedule(cronSchedule("0 * * * * ?"))
              .forJob(job)
              .build();

      scheduler.scheduleJob(job, trigger);

      scheduler
          .getListenerManager()
          .addJobListener(
              new JobListener() {

                @Override
                public String getName() {
                  return "MyJobListener";
                }

                @Override
                public void jobToBeExecuted(JobExecutionContext context) {}

                @Override
                public void jobExecutionVetoed(JobExecutionContext context) {}

                @Override
                public void jobWasExecuted(JobExecutionContext context, JobExecutionException e) {
                  DrawingJobResult result = (DrawingJobResult) context.getResult();

                  adjustAwardAmounts(result.timestamp(), result.winners().size());
                  notifyWinners(result.timestamp(), result.winners());
                }
              });
    } catch (Exception e) {
      throw new RuntimeException("Could not start scheduler", e);
    }
  }

  private void adjustAwardAmounts(LocalDateTime timestamp, int amountOfWinner) {
    if (awardAmounts.containsKey(timestamp) && amountOfWinner > 0) {
      log(INFO, "The winners are: " + amountOfWinner);

      awardAmounts.put(timestamp, 0.0);
    } else if (awardAmounts.containsKey(timestamp) && amountOfWinner == 0) {
      log(INFO, "No winners this time! The money will be carried over to the next drawing.");

      double awardAmount = awardAmounts.get(timestamp);
      awardAmounts.put(timestamp, 0.0);

      LocalDateTime updatedLocalDateTime = timestamp.plusHours(1);

      awardAmounts.putIfAbsent(updatedLocalDateTime, 0.0);
      awardAmounts.put(updatedLocalDateTime, awardAmounts.get(updatedLocalDateTime) + awardAmount);
      System.out.println(awardAmounts);

    } else {
      log(INFO, "No people registered for this time slot and no money to carry over.");
    }
  }

  private void notifyWinners(LocalDateTime timestamp, List<String> winners) {
    // TODO: Notify winners via email
  }

  @Override
  public DrawingRegistrationResponse drawingRegistration(
      DrawingRegistrationRequest registrationRequest) {
    String email = registrationRequest.email();
    Set<Integer> numbers = registrationRequest.drawingNumbers();
    LocalDateTime dateTime = registrationRequest.dateTime();

    //
    // Validation of drawing
    //
    if (dateTime.isBefore(now())) {
      return new DrawingRegistrationResponse(400, "Cannot register for past drawing");
    }

    if (numbers.stream().anyMatch(number -> number < 1 || number > 255)) {
      return new DrawingRegistrationResponse(400, "Number out of range");
    }

    List<Map<String, Set<Integer>>> slots = drawingSlots.get(dateTime);
    if (slots != null) {
      if (slots.stream().anyMatch(slot -> slot.containsKey(email))) {
        for (Map<String, Set<Integer>> slot : slots) {
          if (slot.containsKey(email)) {
            Set<Integer> existingNumbers = slot.get(email);
            if (existingNumbers.stream().anyMatch(numbers::contains)) {
              return new DrawingRegistrationResponse(400, "Number(s) already registered");
            }
          }
        }
      }
    }
    //
    // End of validation
    //

    drawingSlots.computeIfAbsent(dateTime, k -> new ArrayList<>());
    awardAmounts.putIfAbsent(dateTime, 0.0);

    slots = drawingSlots.get(dateTime);
    if (slots.stream().noneMatch(slot -> slot.containsKey(email))) {
      Map<String, Set<Integer>> participant = new HashMap<>();
      participant.put(email, numbers);
      slots.add(participant);
    } else {
      slots.stream()
          .filter(slot -> slot.containsKey(email))
          .forEach(slot -> slot.get(email).addAll(numbers));
    }

    awardAmounts.put(dateTime, awardAmounts.get(dateTime) + PARTICIPATION_FEE);
    System.out.println(drawingSlots); // TODO: Remove
    System.out.println(awardAmounts); // TODO: Remove

    return new DrawingRegistrationResponse(200, "Registration successful");
  }

  @Override
  public void retrieveHistoricalData() {}
}
