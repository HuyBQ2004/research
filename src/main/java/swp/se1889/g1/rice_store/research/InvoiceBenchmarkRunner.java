package swp.se1889.g1.rice_store.research;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import swp.se1889.g1.rice_store.repository.InvoicesRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Random;

@Component
@Profile("benchmark-invoice") // Dùng profile này để chạy riêng biệt, tránh xung đột với DebtRunner
public class InvoiceBenchmarkRunner implements CommandLineRunner {

    @Autowired
    private InvoicesRepository invoiceRepo;
    @Autowired
    private EntityManagerFactory entityManagerFactory;


    // CẤU HÌNH THÍ NGHIỆM (Đồng bộ với FakeData)
    // -----------------------------------------------------------
    private static final int WARMUP_CYCLES = 200;      // Warm-up kỹ hơn chút vì bảng Invoices rất lớn
    private static final int MEASURE_CYCLES = 1000;    // Số mẫu đo
    private static final String CSV_FILE = "research_data_invoices.csv";

    // Giả định dữ liệu đã generate từ FakeData
    private static final long MAX_INVOICE_ID = 1000000L; // Max ID của Invoice (check lại DB của bạn)
    private static final long MAX_STORE_ID = 50L;       // Số lượng Store
    private static final int TARGET_YEAR = 2025;        // Năm dữ liệu
    // -----------------------------------------------------------

    private final Random random = new Random();

    @Override
    public void run(String... args) {
        System.out.println(">>> STARTING INVOICE BENCHMARK PROTOCOL <<<");

        try (FileWriter fw = new FileWriter(CSV_FILE);
             PrintWriter pw = new PrintWriter(fw)) {

            pw.println("iteration,scenario,type,duration_ns,duration_ms,query_count,prepare_count");


            // =========================================================================
            // PHASE 1: WARM-UP
            // =========================================================================
            System.out.println("[Phase 1] Warming up JVM & Cache...");
            for (int i = 0; i < WARMUP_CYCLES; i++) {
                Long rInvId = getRandomInvoiceId();
                Long rStoreId = getRandomStoreId();

                invoiceRepo.findByIdNative(rInvId);
                invoiceRepo.findByIdJPQL(rInvId);

                invoiceRepo.getRevenueByMonthNative(rStoreId, TARGET_YEAR);
                invoiceRepo.getRevenueByMonthJPQL(rStoreId, TARGET_YEAR);

                // Top 5000 (chạy ít hơn lúc warmup vì nặng)
                // ... bên trong vòng lặp warm-up
                // Top 5000 (chạy ít hơn lúc warmup vì nặng)
                if (i % 10 == 0) {
                    invoiceRepo.findTop5000ByStoreNative(rStoreId);
                    invoiceRepo.findTop5000ByStoreJPQL(rStoreId, PageRequest.of(0, 5000));
                    invoiceRepo.findTop5000ByStoreJPQLOptimized(rStoreId, PageRequest.of(0, 5000)); // <--- THÊM DÒNG NÀY
                }
            }

            // =========================================================================
            // SCENARIO 1: SIMPLE READ (FIND BY ID)
            // =========================================================================
            System.out.println("[Phase 2] Measuring S1: Simple Read (Native)...");
            cleanMemory();
            for (int i = 0; i < MEASURE_CYCLES; i++) {
                Long id = getRandomInvoiceId();
                Statistics stats = getStatistics();
                if (stats != null) stats.clear();

                long start = System.nanoTime();
                invoiceRepo.findByIdNative(id);
                long duration = System.nanoTime() - start;

                long queryCount = stats != null ? stats.getQueryExecutionCount() : -1;
                long prepareCount = stats != null ? stats.getPrepareStatementCount() : -1;

                logData(pw, i, "S1_SimpleRead", "Native", duration, queryCount, prepareCount);
            }

            System.out.println("[Phase 3] Measuring S1: Simple Read (JPQL)...");
            cleanMemory();
            for (int i = 0; i < MEASURE_CYCLES; i++) {
                Long id = getRandomInvoiceId();
                Statistics stats = getStatistics();
                if (stats != null) stats.clear();

                long start = System.nanoTime();
                invoiceRepo.findByIdJPQL(id);
                long duration = System.nanoTime() - start;

                long queryCount = stats != null ? stats.getQueryExecutionCount() : -1;
                long prepareCount = stats != null ? stats.getPrepareStatementCount() : -1;

                logData(pw, i, "S1_SimpleRead", "JPQL", duration, queryCount, prepareCount);
            }

            // =========================================================================
            // SCENARIO 2: AGGREGATION (REVENUE REPORT)
            // =========================================================================
            System.out.println("[Phase 4] Measuring S2: Aggregation (Native)...");
            cleanMemory();
            for (int i = 0; i < MEASURE_CYCLES; i++) {
                Long storeId = getRandomStoreId();
                Statistics stats = getStatistics();
                if (stats != null) stats.clear();

                long start = System.nanoTime();
                invoiceRepo.getRevenueByMonthNative(storeId, TARGET_YEAR);
                long duration = System.nanoTime() - start;

                long queryCount = stats != null ? stats.getQueryExecutionCount() : -1;
                long prepareCount = stats != null ? stats.getPrepareStatementCount() : -1;

                logData(pw, i, "S2_Aggregation", "Native", duration, queryCount, prepareCount);
            }

            System.out.println("[Phase 5] Measuring S2: Aggregation (JPQL)...");
            cleanMemory();
            for (int i = 0; i < MEASURE_CYCLES; i++) {
                Long storeId = getRandomStoreId();
                Statistics stats = getStatistics();
                if (stats != null) stats.clear();

                long start = System.nanoTime();
                invoiceRepo.getRevenueByMonthJPQL(storeId, TARGET_YEAR);
                long duration = System.nanoTime() - start;

                long queryCount = stats != null ? stats.getQueryExecutionCount() : -1;
                long prepareCount = stats != null ? stats.getPrepareStatementCount() : -1;

                logData(pw, i, "S2_Aggregation", "JPQL", duration, queryCount, prepareCount);
            }

            // =========================================================================
            // SCENARIO 3: LARGE FETCH (TOP 5000)
            // =========================================================================
            System.out.println("[Phase 6] Measuring S3: Large Fetch (Native)...");
            cleanMemory();
            for (int i = 0; i < MEASURE_CYCLES; i++) {
                Long storeId = getRandomStoreId();
                Statistics stats = getStatistics();
                if (stats != null) stats.clear();

                long start = System.nanoTime();
                invoiceRepo.findTop5000ByStoreNative(storeId);
                long duration = System.nanoTime() - start;

                long queryCount = stats != null ? stats.getQueryExecutionCount() : -1;
                long prepareCount = stats != null ? stats.getPrepareStatementCount() : -1;

                logData(pw, i, "S3_LargeFetch", "Native", duration, queryCount, prepareCount);
            }

            System.out.println("[Phase 7] Measuring S3: Large Fetch (JPQL)...");
            cleanMemory();
            for (int i = 0; i < MEASURE_CYCLES; i++) {
                Long storeId = getRandomStoreId();
                Statistics stats = getStatistics();
                if (stats != null) stats.clear();

                long start = System.nanoTime();
                invoiceRepo.findTop5000ByStoreJPQL(storeId, PageRequest.of(0, 5000));
                long duration = System.nanoTime() - start;

                long queryCount = stats != null ? stats.getQueryExecutionCount() : -1;
                long prepareCount = stats != null ? stats.getPrepareStatementCount() : -1;

                logData(pw, i, "S3_LargeFetch", "JPQL", duration, queryCount, prepareCount);
            }
            // ... (Sau khi kết thúc Phase 7 JPQL)

            System.out.println("[Phase 8] Measuring S3: Large Fetch (JPQL Optimized)...");
            cleanMemory();
            for (int i = 0; i < MEASURE_CYCLES; i++) {
                Long storeId = getRandomStoreId();
                Statistics stats = getStatistics();
                if (stats != null) stats.clear();

                long start = System.nanoTime();

                // Gọi hàm tối ưu
                invoiceRepo.findTop5000ByStoreJPQLOptimized(storeId, PageRequest.of(0, 5000));

                long duration = System.nanoTime() - start;

                long queryCount = stats != null ? stats.getQueryExecutionCount() : -1;
                long prepareCount = stats != null ? stats.getPrepareStatementCount() : -1;

                // Lưu log với type là "JPQL_Opt" hoặc "JPQL_Fetch"
                logData(pw, i, "S3_LargeFetch", "JPQL_Optimized", duration, queryCount, prepareCount);
            }
            System.out.println("[Phase 9] Measuring S4: DTO (JPQL DTO)...");
            cleanMemory();
            for (int i = 0; i < MEASURE_CYCLES; i++) {
                Long storeId = getRandomStoreId();
                Statistics stats = getStatistics();
                if (stats != null) stats.clear();

                long start = System.nanoTime();

                // Gọi hàm tối ưu
                invoiceRepo.findTop5000JPQLDTO(storeId, PageRequest.of(0, 5000));

                long duration = System.nanoTime() - start;

                long queryCount = stats != null ? stats.getQueryExecutionCount() : -1;
                long prepareCount = stats != null ? stats.getPrepareStatementCount() : -1;

                // Lưu log với type là "JPQL_Opt" hoặc "JPQL_Fetch"
                logData(pw, i, "S4_DTO", "JPQL_DTO", duration, queryCount, prepareCount);
            }
            System.out.println("[Phase 10] Measuring S4: DTO (Native)...");
            cleanMemory();
            for (int i = 0; i < MEASURE_CYCLES; i++) {
                Long storeId = getRandomStoreId();
                Statistics stats = getStatistics();
                if (stats != null) stats.clear();

                long start = System.nanoTime();
                invoiceRepo.findTop5000NativeDTO(storeId);
                long duration = System.nanoTime() - start;

                long queryCount = stats != null ? stats.getQueryExecutionCount() : -1;
                long prepareCount = stats != null ? stats.getPrepareStatementCount() : -1;

                logData(pw, i, "S4_DTO", "Native", duration, queryCount, prepareCount);
            }

            System.out.println(">>> INVOICE BENCHMARK COMPLETE <<<");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Long getRandomInvoiceId() {
        return 1L + random.nextInt((int) MAX_INVOICE_ID);
    }

    private Long getRandomStoreId() {
        return 1L + random.nextInt((int) MAX_STORE_ID);
    }

    private void logData(PrintWriter pw, int iteration, String scenario, String type, long durationNs, long queryCount, long prepareCount) {
        pw.printf("%d,%s,%s,%d,%.4f,%d,%d%n", iteration, scenario, type, durationNs, durationNs / 1_000_000.0, queryCount, prepareCount);
        pw.flush();
    }

    private void cleanMemory() {

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }

    private Statistics getStatistics() {
        try {
            SessionFactory sessionFactory =
                    entityManagerFactory.unwrap(SessionFactory.class);
            Statistics stats = sessionFactory.getStatistics();
            if (!stats.isStatisticsEnabled()) {
                stats.setStatisticsEnabled(true);
            }
            return stats;
        } catch (Exception e) {
            return null; // an toàn nếu không unwrap được
        }
    }

}
