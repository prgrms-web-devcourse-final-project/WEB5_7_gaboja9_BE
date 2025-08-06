package io.gaboja9.mockstock.global.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.TasksApi;
import com.influxdb.client.domain.Task;
import com.influxdb.client.domain.TaskStatusType;

import jakarta.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class MonthlyTaskInitializer {

  private final InfluxDBClient influxDBClient;

  @Value("${spring.influx.org}")
  private String influxOrg;

  @Value("${spring.influx.bucket.daily}")
  private String bucket;

  private static final String TASK_NAME = "일봉 to 월봉 변환 작업";

  // 수동 생성자 주입
  public MonthlyTaskInitializer(@Qualifier("dailyInfluxDBClient") InfluxDBClient influxDBClient) {
    this.influxDBClient = influxDBClient;
  }

  @PostConstruct
  public void initializeMonthlyTask() {
    TasksApi tasksApi = influxDBClient.getTasksApi();

    try {
      Optional<Task> existingTask = findTaskByName(tasksApi, TASK_NAME);

      if (existingTask.isPresent()) {
        Task task = existingTask.get();
        log.info(
            "Monthly task '{}' already exists (ID: {}, Status: {})",
            TASK_NAME,
            task.getId(),
            task.getStatus());

        if (task.getStatus() == TaskStatusType.INACTIVE) {
          task.setStatus(TaskStatusType.ACTIVE);
          tasksApi.updateTask(task);
          log.info("Activated monthly task '{}'", TASK_NAME);
        }
      } else {
        createNewTask(tasksApi);
      }

    } catch (Exception e) {
      log.error("Failed to initialize monthly task '{}'", TASK_NAME, e);
    }
  }

  private void createNewTask(TasksApi tasksApi) {
    log.info("Creating new monthly task: {}", TASK_NAME);

    Task newTask = new Task();
    newTask.setName(TASK_NAME);
    newTask.setOrg(influxOrg);
    newTask.setStatus(TaskStatusType.ACTIVE);
    newTask.setDescription("매월 1일 오전 6시에 일봉 데이터를 월봉으로 변환");
    newTask.setFlux(generateFluxScript());

    Task createdTask = tasksApi.createTask(newTask);
    log.info(
        "Successfully created monthly task '{}' with ID: {}",
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
          every: 1mo,
          offset: 6h
        }

        // 이전 달 데이터만 조회해서 이전 달 월봉 생성
        range_stop = date.truncate(t: now(), unit: 1mo)           // 이번 달 1일 00:00:00
        range_start = date.sub(d: 1mo, from: range_stop)          // 지난 달 1일 00:00:00
        
        base = from(bucket: "%s")
          |> range(start: range_start, stop: range_stop)
          |> filter(fn: (r) => r._measurement == "stock_daily")
          |> filter(fn: (r) => {
              weekday = date.weekDay(t: r._time)
              return weekday >= 1 and weekday <= 5
          })

        // 각 필드별로 월봉 데이터 집계하고, 타임스탬프를 월 마지막 날 한국시간 00:00 (UTC 15:00 전날)로 조정
        open = base
          |> filter(fn: (r) => r._field == "openPrice")
          |> aggregateWindow(every: 1mo, fn: first)
          |> map(fn: (r) => ({ 
              r with 
              _field: "openPrice", 
              _measurement: "stock_monthly",
              _time: date.sub(d: 33h, from: date.add(d: 1mo, to: date.truncate(t: r._time, unit: 1mo)))
          }))

        high = base
          |> filter(fn: (r) => r._field == "maxPrice")
          |> aggregateWindow(every: 1mo, fn: max)
          |> map(fn: (r) => ({ 
              r with 
              _field: "maxPrice", 
              _measurement: "stock_monthly",
              _time: date.sub(d: 33h, from: date.add(d: 1mo, to: date.truncate(t: r._time, unit: 1mo)))
          }))

        low = base
          |> filter(fn: (r) => r._field == "minPrice")
          |> aggregateWindow(every: 1mo, fn: min)
          |> map(fn: (r) => ({ 
              r with 
              _field: "minPrice", 
              _measurement: "stock_monthly",
              _time: date.sub(d: 33h, from: date.add(d: 1mo, to: date.truncate(t: r._time, unit: 1mo)))
          }))

        close = base
          |> filter(fn: (r) => r._field == "closePrice")
          |> aggregateWindow(every: 1mo, fn: last)
          |> map(fn: (r) => ({ 
              r with 
              _field: "closePrice", 
              _measurement: "stock_monthly",
              _time: date.sub(d: 33h, from: date.add(d: 1mo, to: date.truncate(t: r._time, unit: 1mo)))
          }))

        volume = base
          |> filter(fn: (r) => r._field == "accumTrans")
          |> aggregateWindow(every: 1mo, fn: sum)
          |> map(fn: (r) => ({ 
              r with 
              _field: "accumTrans", 
              _measurement: "stock_monthly",
              _time: date.sub(d: 33h, from: date.add(d: 1mo, to: date.truncate(t: r._time, unit: 1mo)))
          }))

        // 집계된 데이터를 하나로 합쳐서 최종 저장
        union(tables: [open, high, low, close, volume])
          |> to(bucket: "%s", org: "%s")

        """,
        TASK_NAME, bucket, bucket, influxOrg);
  }
}