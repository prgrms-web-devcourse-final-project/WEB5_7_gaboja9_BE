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
public class FiveMinuteTaskInitializer {

    @Value("${spring.influx.org}")
    private String influxOrg;

    @Value("${spring.influx.bucket.minute}")
    private String minuteBucket;

    private static final String TASK_NAME = "5분봉 실시간 생성 (장중 전용)";

    private final InfluxDBClient minuteInfluxDBClient;

    public FiveMinuteTaskInitializer(
            @Qualifier("minuteInfluxDBClient") InfluxDBClient minuteInfluxDBClient) {
        this.minuteInfluxDBClient = minuteInfluxDBClient;
    }

    @PostConstruct
    public void initializeFiveMinuteTask() {
        TasksApi tasksApi = minuteInfluxDBClient.getTasksApi();

        try {
            Optional<Task> existingTask = findTaskByName(tasksApi, TASK_NAME);

            if (existingTask.isPresent()) {
                Task task = existingTask.get();
                log.info(
                        "5-minute task '{}' already exists (ID: {}, Status: {})",
                        TASK_NAME,
                        task.getId(),
                        task.getStatus());

                if (task.getStatus() == TaskStatusType.INACTIVE) {
                    task.setStatus(TaskStatusType.ACTIVE);
                    tasksApi.updateTask(task);
                    log.info("Activated 5-minute task '{}'", TASK_NAME);
                }
            } else {
                createNewTask(tasksApi);
            }
        } catch (Exception e) {
            log.error("Failed to initialize 5-minute task '{}'", TASK_NAME, e);
        }
    }

    private void createNewTask(TasksApi tasksApi) {
        log.info("Creating new 5-minute task: {}", TASK_NAME);

        Task newTask = new Task();
        newTask.setName(TASK_NAME);
        newTask.setOrg(influxOrg);
        newTask.setStatus(TaskStatusType.ACTIVE);
        newTask.setDescription("5분마다 1분봉 데이터를 집계하여 5분봉 생성 후 동일 버킷에 저장");
        newTask.setFlux(generateFluxScript());

        Task createdTask = tasksApi.createTask(newTask);
        log.info(
                "Successfully created 5-minute task '{}' with ID: {}",
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
                option task = { name: "%s", every: 5m, offset: 1m }
                // 1. 월봉/주봉 스크립트와 동일한 구조를 위해 시간 범위를 계산합니다.
                range_stop = date.truncate(t: now(), unit: 5m)   // 예: 12:10:00
                range_start = date.sub(d: 5m, from: range_stop)    // 예: 12:05:00

                base = from(bucket: "%s")
                  |> range(start: range_start, stop: range_stop)
                  |> filter(fn: (r) => r._measurement == "stock_minute")
                  |> filter(fn: (r) => {
                      kst = date.add(d: 9h, to: r._time)
                      h = date.hour(t: kst)
                      m = date.minute(t: kst)
                      return (h >= 9 and h < 15) or (h == 15 and m <= 30)
                  })

                // 3. 각 필드별로 5분봉을 집계합니다.
                // timeSrc를 생략하면 윈도우의 종료 시점(range_stop)이 타임스탬프가 됩니다.
                open = base |> filter(fn: (r) => r._field == "openPrice") |> aggregateWindow(every: 5m, fn: first)
                high = base |> filter(fn: (r) => r._field == "maxPrice") |> aggregateWindow(every: 5m, fn: max)
                low = base |> filter(fn: (r) => r._field == "minPrice") |> aggregateWindow(every: 5m, fn: min)
                close = base |> filter(fn: (r) => r._field == "closePrice") |> aggregateWindow(every: 5m, fn: last)
                volume = base |> filter(fn: (r) => r._field == "accumTrans") |> aggregateWindow(every: 5m, fn: sum)

                // 4. 집계된 데이터들을 하나로 합치고 "stock_5minute"으로 이름을 바꿔 저장합니다.
                union(tables: [open, high, low, close, volume])
                  |> map(fn: (r) => ({ r with _measurement: "stock_5minute" }))
                  |> to(bucket: "%s", org: "%s")
                """,
                TASK_NAME, minuteBucket, minuteBucket, influxOrg);
    }
}
