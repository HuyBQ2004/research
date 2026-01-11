package swp.se1889.g1.rice_store.research;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import swp.se1889.g1.rice_store.repository.DebtRecordRepository;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Optional;

@Component
@Profile("benchmark")
public class DebtBenchmarkRunner implements CommandLineRunner {

    @Autowired
    private DebtRecordRepository debtRepo;
    @Autowired
    private EntityManagerFactory entityManagerFactory;


    // -----------------------------------------------------------
    private static final int WARMUP_CYCLES = 100;
    private static final int MEASURE_CYCLES = 1000;
    private static final String CSV_FILE = "research_data_debt_full.csv";
    private static final int MAX_CUSTOMER_ID = 1000;
    private static final long MAX_DEBT_ID = 500000L;
    // -----------------------------------------------------------

    private final Random random = new Random();

    @Override
    public void run(String... args) {
        System.out.println(">>> STARTING RESEARCH BENCHMARK PROTOCOL (Q1 STANDARD) <<<");
        System.out.println("Data will be written to: " + CSV_FILE);

        try (FileWriter fw = new FileWriter(CSV_FILE);
             PrintWriter pw = new PrintWriter(fw)) {

            // HEADER CSV
            pw.println("iteration,scenario,type,duration_ns,duration_ms,sql_queries,prepared_statements");

            // =========================================================================
            // PHASE 1: WARM-UP

            // =========================================================================
            System.out.println("[Phase Warmup] Warming up system (Ignoring results)...");
            for (int i = 0; i < WARMUP_CYCLES; i++) {
                Long randomCustomerId = getRandomCustomerId();
                Long randomDebtId = getRandomDebtId();
                debtRepo.getMonthlyDebtNative(randomCustomerId);
                debtRepo.getMonthlyDebtJPQL(randomCustomerId);
                debtRepo.getHistoryNative(randomCustomerId);
                debtRepo.getHistoryJPQL(randomCustomerId, PageRequest.of(0, 5000));
                debtRepo.findById(randomDebtId);
                debtRepo.findByIdNative(randomDebtId);
                debtRepo.getHistoryNativeDTO(randomCustomerId);
                debtRepo.getHistoryJPQLDTO(randomCustomerId, PageRequest.of(0, 5000));
            }

            // =========================================================================
            // PHASE S1: SIMPLE READ (JPQL vs NATIVE)
            // ========================================================================
            System.out.println("[Phase S1/5] Measuring Scenario 1 (Simple Read) - NATIVE vs JPQL...");
            cleanMemory();

            // S1 - NATIVE
            for (int i = 0; i < MEASURE_CYCLES; i++) {
                Long randomId = getRandomDebtId();
                Statistics statistics = hibernateStatistics();
                if (statistics != null) statistics.clear();

                long start = System.nanoTime();
                debtRepo.findByIdNative(randomId);
                long duration = System.nanoTime() - start;

                long queryCount = statistics != null ? statistics.getQueryExecutionCount() : -1;
                long prepCount = statistics != null ? statistics.getPrepareStatementCount() : -1;

                logData(pw, i, "S1_SimpleRead", "Native", duration, queryCount, prepCount);
            }

            // S1 - JPQL (JPA findById)
            cleanMemory();
            for (int i = 0; i < MEASURE_CYCLES; i++) {
                Long randomId = getRandomDebtId();
                Statistics statistics = hibernateStatistics();
                if (statistics != null) statistics.clear();

                long start = System.nanoTime();
                // findById is a JPA method, repository still exposes it
                Optional<?> ignored = debtRepo.findById(randomId);
                long duration = System.nanoTime() - start;

                long queryCount = statistics != null ? statistics.getQueryExecutionCount() : -1;
                long prepCount = statistics != null ? statistics.getPrepareStatementCount() : -1;

                logData(pw, i, "S1_SimpleRead", "JPQL", duration, queryCount, prepCount);
            }

            // =========================================================================
            // PHASE 2: SCENARIO 2 - COMPLEX AGGREGATION (NATIVE)
            // =========================================================================
            System.out.println("[Phase 2/5] Measuring Scenario 2 (Aggregation) - NATIVE...");
            cleanMemory(); // Reset RAM để công bằng

            for (int i = 0; i < MEASURE_CYCLES; i++) {
                Long randomId = getRandomCustomerId();

                Statistics statistics = hibernateStatistics();
                if (statistics != null) statistics.clear();

                long start = System.nanoTime();
                debtRepo.getMonthlyDebtNative(randomId);
                long duration = System.nanoTime() - start;

                long queryCount = statistics != null ? statistics.getQueryExecutionCount() : -1;
                long prepCount = statistics != null ? statistics.getPrepareStatementCount() : -1;

                logData(pw, i, "S2_Aggregation", "Native", duration, queryCount, prepCount);
            }

            // =========================================================================
            // PHASE 3: SCENARIO 2 - COMPLEX AGGREGATION (JPQL)
            // =========================================================================
            System.out.println("[Phase 3/5] Measuring Scenario 2 (Aggregation) - JPQL...");
            cleanMemory();

            for (int i = 0; i < MEASURE_CYCLES; i++) {
                Long randomId = getRandomCustomerId();

                Statistics statistics = hibernateStatistics();
                if (statistics != null) statistics.clear();

                long start = System.nanoTime();
                debtRepo.getMonthlyDebtJPQL(randomId);
                long duration = System.nanoTime() - start;

                long queryCount = statistics != null ? statistics.getQueryExecutionCount() : -1;
                long prepCount = statistics != null ? statistics.getPrepareStatementCount() : -1;

                logData(pw, i, "S2_Aggregation", "JPQL", duration, queryCount, prepCount);
            }

            // =========================================================================
            // PHASE 4: SCENARIO 3 - LARGE DATA FETCH (NATIVE)
            // =========================================================================
            System.out.println("[Phase 4/5] Measuring Scenario 3 (Large Fetch 5000 rows) - NATIVE...");
            cleanMemory();

            for (int i = 0; i < MEASURE_CYCLES; i++) {
                Long randomId = getRandomCustomerId();

                Statistics statistics = hibernateStatistics();
                if (statistics != null) statistics.clear();

                long start = System.nanoTime();
                debtRepo.getHistoryNative(randomId);
                long duration = System.nanoTime() - start;

                long queryCount = statistics != null ? statistics.getQueryExecutionCount() : -1;
                long prepCount = statistics != null ? statistics.getPrepareStatementCount() : -1;

                logData(pw, i, "S3_LargeFetch", "Native", duration, queryCount, prepCount);
            }

            // =========================================================================
            // PHASE 5: SCENARIO 3 - LARGE DATA FETCH (JPQL)
            // =========================================================================
            System.out.println("[Phase 5/5] Measuring Scenario 3 (Large Fetch 5000 rows) - JPQL...");
            cleanMemory();

            for (int i = 0; i < MEASURE_CYCLES; i++) {
                Long randomId = getRandomCustomerId();

                Statistics statistics = hibernateStatistics();
                if (statistics != null) statistics.clear();

                long start = System.nanoTime();
                debtRepo.getHistoryJPQL(randomId, PageRequest.of(0, 5000));
                long duration = System.nanoTime() - start;

                long queryCount = statistics != null ? statistics.getQueryExecutionCount() : -1;
                long prepCount = statistics != null ? statistics.getPrepareStatementCount() : -1;

                logData(pw, i, "S3_LargeFetch", "JPQL", duration, queryCount, prepCount);
            }
            System.out.println("[Phase S4] Measuring Scenario 4 (Large Fetch - DTO)...");
            cleanMemory();
            for (int i = 0; i < MEASURE_CYCLES; i++) {
                Long randomId = getRandomCustomerId();

                Statistics statistics = hibernateStatistics();
                if (statistics != null) statistics.clear();

                long start = System.nanoTime();
                debtRepo.getHistoryJPQLDTO(randomId, PageRequest.of(0, 5000));
                long duration = System.nanoTime() - start;

                long queryCount = statistics != null ? statistics.getQueryExecutionCount() : -1;
                long prepCount = statistics != null ? statistics.getPrepareStatementCount() : -1;

                logData(pw, i, "S4_DTO", "JPQL", duration, queryCount, prepCount);
            }
            System.out.println("[S4] Measuring Scenario 4 (DTO 5000 rows) - NATIVE...");
            cleanMemory();

            for (int i = 0; i < MEASURE_CYCLES; i++) {
                Long randomId = getRandomCustomerId();

                Statistics statistics = hibernateStatistics();
                if (statistics != null) statistics.clear();

                long start = System.nanoTime();
                debtRepo.getHistoryNativeDTO(randomId);
                long duration = System.nanoTime() - start;

                long queryCount = statistics != null ? statistics.getQueryExecutionCount() : -1;
                long prepCount = statistics != null ? statistics.getPrepareStatementCount() : -1;

                logData(pw, i, "S4_DTO", "Native", duration, queryCount, prepCount);
            }

            System.out.println(">>> BENCHMARK COMPLETE. SUCCESS! <<<");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Helper Methods ---

    private Long getRandomCustomerId() {
        // Random from 1 to MAX_CUSTOMER_ID
        return 1L + random.nextInt(MAX_CUSTOMER_ID);
    }

    private Long getRandomDebtId() {
        //Random from 1 to MAX_DEBT_ID
        return 1L + (long) (random.nextDouble() * MAX_DEBT_ID);
    }

    private void logData(PrintWriter pw, int iteration, String scenario, String type, long durationNs, long queryCount, long prepCount) {

        pw.printf("%d,%s,%s,%d,%.4f,%d,%d%n",
                iteration,
                scenario,
                type,
                durationNs,
                durationNs / 1_000_000.0,
                queryCount,
                prepCount);
        pw.flush();
    }

    private void cleanMemory() {

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Statistics hibernateStatistics() {
        try {
            SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
            Statistics statistics = sessionFactory.getStatistics();

            if (!statistics.isStatisticsEnabled()) statistics.setStatisticsEnabled(true);
            return statistics;
        } catch (Exception e) {

            return null;
        }
    }
}
