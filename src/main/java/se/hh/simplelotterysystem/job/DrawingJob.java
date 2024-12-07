package se.hh.simplelotterysystem.job;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
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
import org.quartz.JobExecutionException;

public class DrawingJob implements Job {

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    System.out.println("=========================================================================");
    log(INFO, "🎰💯 Drawing job for time slot " + now().format(ofPattern("yyyy-MM-dd'T'HH:mm")));
    List<Map<String, Set<Integer>>> currentDrawingSlots = obtainCurrentDrawingSlots(context);
    if (currentDrawingSlots == null) {
      log(INFO, "🎰 No people registered for this time slot.");
      System.out.println("=========================================================================");
      return;
    }

    int random = generateRandomNumber();
    List<String> winners = obtainWinners(currentDrawingSlots, random);

    if (winners.isEmpty()) {
      log(INFO, "🎰 No winners this time! The random number was: " + random);
    } else {
      log(INFO, "🏆🎖️ The winners are: " + winners + " with number " + random);
    }
    System.out.println("=========================================================================");

    // TODO: Send email to winners!
  }

  @SuppressWarnings({"unchecked"})
  private List<Map<String, Set<Integer>>> obtainCurrentDrawingSlots(JobExecutionContext context) {
    JobDataMap dataMap = context.getJobDetail().getJobDataMap();
    Map<LocalDateTime, List<Map<String, Set<Integer>>>> drawingSlots =
        (Map<LocalDateTime, List<Map<String, Set<Integer>>>>) dataMap.get("drawingSlots");
    LocalDateTime truncatedTimestamp = now().truncatedTo(ChronoUnit.MINUTES);
    return drawingSlots.get(truncatedTimestamp);
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
