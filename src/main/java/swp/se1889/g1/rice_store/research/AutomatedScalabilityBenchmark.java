package swp.se1889.g1.rice_store.research;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import swp.se1889.g1.rice_store.repository.InvoicesRepository;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AutomatedScalabilityBenchmark implements CommandLineRunner {

    private final InvoicesRepository invoicesRepository;

    // ===== JOURNAL CONFIGURATION =====
    // Tăng lên mức 8000 để tìm điểm gãy (Breaking Point)
    private static final int[] CONCURRENT_USERS_LIST = {100, 500, 1000, 2000, 4000, 8000};
    private static final int TOTAL_ITERATIONS = 10; // 10 lần là đủ cho thống kê
    private static final int WARMUP_ROUNDS = 3;
    private static final Long TEST_STORE_ID = 1L;

    // Giả lập độ trễ thực tế của DB/Network (Quan trọng cho Virtual Thread)
    private static final int SIMULATED_LATENCY_MS = 50;

    private final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    private final String FILE_RAW = "research_result_FINAL_" + timeStamp + ".csv";

    public AutomatedScalabilityBenchmark(InvoicesRepository invoicesRepository) {
        this.invoicesRepository = invoicesRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== IST JOURNAL BENCHMARK STARTED ===");

        try (PrintWriter rawWriter = new PrintWriter(new FileWriter(FILE_RAW, true))) {
            // Header chuẩn cho phân tích R/Python
            rawWriter.println("Scenario,Type,Concurrency,Iteration,Throughput_RPS,P99_ms,Success_Rate");

            // Kịch bản đối chứng (Control & Variable)
            List<TestScenario> scenarios = List.of(
                    // 1. CPU BOUND: Kiểm chứng sự công bằng (PT sẽ thắng hoặc hòa)
                    new TestScenario("PT_CPU_Heavy", false, TaskType.CPU_BOUND),
                    new TestScenario("VT_CPU_Heavy", true, TaskType.CPU_BOUND),

                    // 2. IO BOUND: Kịch bản chính (VT sẽ thắng áp đảo)
                    new TestScenario("PT_IO_Wait", false, TaskType.IO_BOUND),
                    new TestScenario("VT_IO_Wait", true, TaskType.IO_BOUND)
            );

            for (int concurrency : CONCURRENT_USERS_LIST) {
                // Mỗi user request 10 lần
                int totalRequests = concurrency * 10;

                for (TestScenario scenario : scenarios) {
                    System.out.printf("Running: %s | Users: %d... ", scenario.name, concurrency);

                    ExecutorService executor = scenario.useVirtualThreads
                            ? Executors.newVirtualThreadPerTaskExecutor()
                            : Executors.newFixedThreadPool(concurrency);

                    for (int i = 1; i <= TOTAL_ITERATIONS; i++) {
                        // Clean up trước mỗi lần chạy
                        System.gc();
                        Thread.sleep(500);

                        RunMetrics metrics = runExperiment(executor, scenario, concurrency, totalRequests);

                        // Ghi log RAW để vẽ biểu đồ Boxplot
                        if (i > WARMUP_ROUNDS) {
                            rawWriter.printf("%s,%s,%d,%d,%.2f,%.2f,%.2f%n",
                                    scenario.name, scenario.type, concurrency, i,
                                    metrics.throughput, metrics.p99, metrics.successRate);
                            rawWriter.flush();
                        }
                    }

                    executor.shutdownNow();
                    System.out.println("DONE");
                }
            }
        }
        System.out.println("=== BENCHMARK COMPLETED. DATA SAVED TO " + FILE_RAW + " ===");
    }

    private RunMetrics runExperiment(ExecutorService executor, TestScenario scenario, int concurrency, int totalRequests) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(totalRequests);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger(0);

        long start = System.nanoTime();

        for (int k = 0; k < totalRequests; k++) {
            executor.submit(() -> {
                long t0 = System.nanoTime();
                try {
                    if (scenario.type == TaskType.CPU_BOUND) {
                        performCpuTask(); // Kéo CPU lên 100%
                    } else {
                        performIoTask();  // Query nhẹ + Sleep
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Lỗi do quá tải (Connection timeout, v.v.)
                } finally {
                    latencies.add(System.nanoTime() - t0);
                    latch.countDown();
                }
            });
        }

        // Timeout an toàn để tránh treo máy khi PT bị đơ
        boolean finished = latch.await(60, TimeUnit.SECONDS);

        long durationNs = System.nanoTime() - start;
        double durationSec = durationNs / 1_000_000_000.0;

        // Tính toán Metrics
        double throughput = successCount.get() / durationSec;
        double successRate = (double) successCount.get() / totalRequests * 100;

        Collections.sort(latencies);
        double p99 = latencies.isEmpty() ? 0 : latencies.get((int) (latencies.size() * 0.99)) / 1_000_000.0;

        return new RunMetrics(throughput, p99, successRate);
    }

    // === TASK 1: IO BOUND (Sở trường VT) ===
    private void performIoTask() throws InterruptedException {
        // 1. Query siêu nhẹ (lấy DTO) để tránh nghẽn DB
        Pageable limit = PageRequest.of(0, 20);
        invoicesRepository.findTop5000JPQLDTO(TEST_STORE_ID, limit);

        // 2. Giả lập Latency (Chìa khóa của bài báo)
        // Mô phỏng gọi API thanh toán hoặc query phức tạp mất 50ms
        Thread.sleep(SIMULATED_LATENCY_MS);
    }

    // === TASK 2: CPU BOUND (Sở trường PT) ===
    private void performCpuTask() {
        // Tính toán vô nghĩa để đốt CPU
        double result = 0;
        for (int i = 0; i < 1000; i++) {
            result += Math.sin(i) * Math.cos(i) * Math.tan(i);
        }
    }

    // Records
    record TestScenario(String name, boolean useVirtualThreads, TaskType type) {
    }

    record RunMetrics(double throughput, double p99, double successRate) {
    }

    enum TaskType {CPU_BOUND, IO_BOUND}
}