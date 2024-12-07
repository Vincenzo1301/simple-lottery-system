package se.hh.simplelotterysystem.service.impl;

import static java.time.LocalDateTime.now;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import se.hh.simplelotterysystem.data.DrawingRegistrationRequest;
import se.hh.simplelotterysystem.data.DrawingRegistrationResponse;
import se.hh.simplelotterysystem.job.DrawingJob;
import se.hh.simplelotterysystem.service.LotteryService;

public class LotteryServiceImpl implements LotteryService {

  private final Scheduler scheduler;
  private final Map<LocalDateTime, List<Map<String, Set<Integer>>>> drawingSlots = new HashMap<>();

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
          TriggerBuilder.newTrigger()
              .withIdentity("drawingTrigger", "lotteryGroup")
              .withSchedule(CronScheduleBuilder.cronSchedule("0 * * * * ?")) // every second
              .forJob(job)
              .build();

      scheduler.scheduleJob(job, trigger);
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

    System.out.println(drawingSlots);

    return new DrawingRegistrationResponse(200, "Registration successful");
  }

  @Override
  public void retrieveHistoricalData() {}
}
