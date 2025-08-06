package io.gaboja9.mockstock.global.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.TasksApi;
import com.influxdb.client.domain.Task;
import com.influxdb.client.domain.TaskStatusType;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DailyTaskInitializer {

  @Value("${spring.influx.org}")
  private String influxOrg;

  @Value("${spring.influx.bucket.minute}")
  private String minuteBucket;

  @Value("${spring.influx.bucket.daily}")
  private String dailyBucket;

  private static final String TASK_NAME = "1분봉 to 일봉 변환 작업";

  private final InfluxDBClient minuteInfluxDBClient;

  public DailyTaskInitializer(
      @Qualifier("minuteInfluxDBClient") InfluxDBClient minuteInfluxDBClient) {
    this.minuteInfluxDBClient = minuteInfluxDBClient;
  }

  @PostConstruct
  public void initializeDailyFromMinuteTask() {
    TasksApi tasksApi = minuteInfluxDBClient.getTasksApi();

    try {
      Optional<Task> existingTask = findTaskByName(tasksApi, TASK_NAME);

      if (existingTask.isPresent()) {
        Task task = existingTask.get();
        log.info(
            "Daily-from-minute task '{}' already exists (ID: {}, Status: {})",
            TASK_NAME,
            task.getId(),
            task.getStatus());

        if (task.getStatus() == TaskStatusType.INACTIVE) {
          task.setStatus(TaskStatusType.ACTIVE);
          tasksApi.updateTask(task);
          log.info("Activated daily-from-minute task '{}'", TASK_NAME);
        }
      } else {
        createNewTask(tasksApi);
      }

    } catch (Exception e) {
      log.error("Failed to initialize daily-from-minute task '{}'", TASK_NAME, e);
    }
  }

  private void createNewTask(TasksApi tasksApi) {
    log.info("Creating new daily-from-minute task: {}", TASK_NAME);

    Task newTask = new Task();
    newTask.setName(TASK_NAME);
    newTask.setOrg(influxOrg);
    newTask.setStatus(TaskStatusType.ACTIVE);
    newTask.setDescription("매일 오후 6시에 1분봉 데이터를 일봉으로 변환하여 daily 버킷에 저장");
    newTask.setFlux(generateFluxScript());

    Task createdTask = tasksApi.createTask(newTask);
    log.info(
        "Successfully created daily-from-minute task '{}' with ID: {}",
        TASK_NAME,
        createdTask.getId());
  }

  private Optional<Task> findTaskByName(TasksApi tasksApi, String name) {
    List<Task> tasks = tasksApi.findTasks();
    return tasks.stream().filter(task -> name.equals(task.getName())).findFirst();
  }


  private String generateFluxScript() {
    return String.format(
        """
        import "date"
  
        option task = {
          name: "%s",
          every: 1d,
          // 수정: 18:00 KST는 09:00 UTC이므로 offset을 9h로 설정
          offset: 9h
        }
        // 오늘 00:00 UTC 부터 Task 실행 시점(18:00 KST)까지 조회
        range_start = date.truncate(t: now(), unit: 1d)
        range_stop = now()
        
        // minute 버킷에서 한국 장 시간(09:00 ~ 15:30) 데이터만 필터링
        base = from(bucket: "%s")
          |> range(start: range_start, stop: range_stop)
          |> filter(fn: (r) => r._measurement == "stock_minute")
          |> filter(fn: (r) => {
              kst = date.add(d: 9h, to: r._time)
              h = date.hour(t: kst)
              m = date.minute(t: kst)
              return (h >= 9 and h < 15) or (h == 15 and m <= 30)
          })
        // 타임스탬프를 해당일의 15:00 UTC로 설정하는 함수
        setTimestampTo15UTC = (r) => {
          // Task 실행 시간을 기준으로 날짜의 시작(00:00 UTC)을 구함
          day_start_utc = date.truncate(t: now(), unit: 1d)
 
          return {
            r with
            _measurement: "stock_daily",
            _time: date.add(d: 15h, to: day_start_utc)
          }
        }
  
        open = base
          |> filter(fn: (r) => r._field == "openPrice")
          |> aggregateWindow(every: 1d, fn: first)
          |> map(fn: (r) => ({ r with _field: "openPrice" }))
          |> map(fn: setTimestampTo15UTC)
  
        high = base
          |> filter(fn: (r) => r._field == "maxPrice")
          |> aggregateWindow(every: 1d, fn: max)
          |> map(fn: (r) => ({ r with _field: "maxPrice" }))
          |> map(fn: setTimestampTo15UTC)
  
        low = base
          |> filter(fn: (r) => r._field == "minPrice")
          |> aggregateWindow(every: 1d, fn: min)
          |> map(fn: (r) => ({ r with _field: "minPrice" }))
          |> map(fn: setTimestampTo15UTC)
  
        close = base
          |> filter(fn: (r) => r._field == "closePrice")
          |> aggregateWindow(every: 1d, fn: last)
          |> map(fn: (r) => ({ r with _field: "closePrice" }))
          |> map(fn: setTimestampTo15UTC)
  
        volume = base
          |> filter(fn: (r) => r._field == "accumTrans")
          |> aggregateWindow(every: 1d, fn: last)
          |> map(fn: (r) => ({ r with _field: "accumTrans" }))
          |> map(fn: setTimestampTo15UTC)
  
        union(tables: [open, high, low, close, volume])
          |> to(bucket: "%s", org: "%s")
        """,
        TASK_NAME, minuteBucket, dailyBucket, influxOrg);
  }

}
