package se.hh.simplelotterysystem.job;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static se.hh.simplelotterysystem.enums.LoggingType.INFO;
import static se.hh.simplelotterysystem.util.Logger.log;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

public class DrawingJob implements Job {

  @Override
  public void execute(JobExecutionContext context) {
    System.out.println("=========================================================================");
    log(INFO, "Drawing job for time slot " + now().format(ofPattern("yyyy-MM-dd'T'HH:mm")));

    LocalDateTime timestamp = now().truncatedTo(ChronoUnit.MINUTES);

    List<Map<String, Set<Integer>>> currentDrawingSlots =
        obtainCurrentDrawingSlots(context, timestamp);

    if (currentDrawingSlots == null) {
      context.setResult(new DrawingJobResult(timestamp, emptyList()));
      return;
    }

    int random = generateRandomNumber();
    List<String> winners = obtainWinners(currentDrawingSlots, random);

    context.setResult(new DrawingJobResult(timestamp, winners));
    if (winners.isEmpty()) {
      context.setResult(new DrawingJobResult(timestamp, emptyList()));
    } else {
      context.setResult(new DrawingJobResult(timestamp, (winners)));
    }
  }

  @SuppressWarnings({"unchecked"})
  private List<Map<String, Set<Integer>>> obtainCurrentDrawingSlots(
      JobExecutionContext context, LocalDateTime timestamp) {
    JobDataMap dataMap = context.getJobDetail().getJobDataMap();
    Map<LocalDateTime, List<Map<String, Set<Integer>>>> drawingSlots =
        (Map<LocalDateTime, List<Map<String, Set<Integer>>>>) dataMap.get("drawingSlots");
    return drawingSlots.get(timestamp);
  }

  private int generateRandomNumber() {
    // TODO: return (int) (Math.random() * 256);
    return 5;
  }

  private List<String> obtainWinners(
      List<Map<String, Set<Integer>>> currentDrawingSlots, int random) {
    return currentDrawingSlots.stream()
        .flatMap(
            slot ->
                slot.entrySet().stream()
                    .filter(entry -> entry.getValue().contains(random))
                    .map(Map.Entry::getKey))
        .toList();
  }
}
