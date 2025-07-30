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
public class WeeklyTaskInitializer {

    private final InfluxDBClient influxDBClient;

    @Value("${spring.influx.org}")
    private String Influxorg;

    @Value("${spring.influx.bucket.daily}")
    private String bucket;

    private static final String TASK_NAME = "일봉 to 주봉 변환 작업";

    // 수동 생성자 주입
    public WeeklyTaskInitializer(@Qualifier("dailyInfluxDBClient") InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
    }

    @PostConstruct
    public void initializeWeeklyTask() {
        TasksApi tasksApi = influxDBClient.getTasksApi();

        try {
            Optional<Task> existingTask = findTaskByName(tasksApi, TASK_NAME);

            if (existingTask.isPresent()) {
                Task task = existingTask.get();
                log.info(
                        "Weekly task '{}' already exists (ID: {}, Status: {})",
                        TASK_NAME,
                        task.getId(),
                        task.getStatus());

                if (task.getStatus() == TaskStatusType.INACTIVE) {
                    task.setStatus(TaskStatusType.ACTIVE);
                    tasksApi.updateTask(task);
                    log.info("Activated weekly task '{}'", TASK_NAME);
                }
            } else {
                createNewTask(tasksApi);
            }

        } catch (Exception e) {
            log.error("Failed to initialize weekly task '{}'", TASK_NAME, e);
        }
    }

    private void createNewTask(TasksApi tasksApi) {
        log.info("Creating new weekly task: {}", TASK_NAME);

        Task newTask = new Task();
        newTask.setName(TASK_NAME);
        newTask.setOrg(Influxorg);
        newTask.setStatus(TaskStatusType.ACTIVE);
        newTask.setDescription("매주 토요일 오전 6시에 일봉 데이터를 주봉으로 변환");
        newTask.setFlux(generateFluxScript());

        Task createdTask = tasksApi.createTask(newTask);
        log.info(
                "Successfully created weekly task '{}' with ID: {}",
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
                  every: 1w,
                  offset: 45h
                }

                // 1. 공통 데이터 조회 (데일리 버킷에서)
                base = from(bucket: "%s")
                  |> range(start: -8d)
                  |> filter(fn: (r) => r._measurement == "stock_daily")
                  |> filter(fn: (r) => {
                      weekday = date.weekDay(t: r._time)
                      return weekday >= 1 and weekday <= 5
                  })

                // 2. 각 필드별로 주봉 데이터 집계하고, measurement를 'stock_weekly'로 설정
                open = base
                  |> filter(fn: (r) => r._field == "openPrice")
                  |> aggregateWindow(every: 1w, offset: -3d, fn: first)
                  |> map(fn: (r) => ({ r with _field: "openPrice", _measurement: "stock_weekly" }))

                high = base
                  |> filter(fn: (r) => r._field == "maxPrice")
                  |> aggregateWindow(every: 1w, offset: -3d, fn: max)
                  |> map(fn: (r) => ({ r with _field: "maxPrice", _measurement: "stock_weekly" }))

                low = base
                  |> filter(fn: (r) => r._field == "minPrice")
                  |> aggregateWindow(every: 1w, offset: -3d, fn: min)
                  |> map(fn: (r) => ({ r with _field: "minPrice", _measurement: "stock_weekly" }))

                close = base
                  |> filter(fn: (r) => r._field == "closePrice")
                  |> aggregateWindow(every: 1w, offset: -3d, fn: last)
                  |> map(fn: (r) => ({ r with _field: "closePrice", _measurement: "stock_weekly" }))

                volume = base
                  |> filter(fn: (r) => r._field == "accumTrans")
                  |> aggregateWindow(every: 1w, offset: -3d, fn: sum)
                  |> map(fn: (r) => ({ r with _field: "accumTrans", _measurement: "stock_weekly" }))

                // 3. 집계된 데이터를 하나로 합쳐서 최종 저장 (다시 데일리 버킷에)
                union(tables: [open, high, low, close, volume])
                  |> to(bucket: "%s", org: "%s")

                """,
                TASK_NAME, bucket, bucket, Influxorg);
    }
}
