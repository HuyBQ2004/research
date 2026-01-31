# Experimental Artifacts: Scalability under Resource Constraints
## An Empirical Study of Java Virtual Threads

This repository contains the source code, benchmark scripts, and raw data used in the manuscript **"Scalability under Resource Constraints: An Empirical Study of Java Virtual Threads"**, submitted to *Performance Evaluation*.

**Authors:**
* Bui Quang Huy (FPT University)
* Bui Minh Nhat (East Asia University of Technology)

---

## 1. Overview
This study presents a controlled empirical performance evaluation of Java Virtual Threads compared to Platform Threads in a Spring Boot microservice environment. The experiments focus on two key scenarios:
* **Scenario A (CPU-bound):** Trigonometric operations to validate carrier thread pinning behavior.
* **Scenario B (I/O-bound):** Database queries with simulated network latency to demonstrate thread unmounting.

The goal is to quantify trade-offs in **Throughput**, **Tail Latency (P99)**, **OS Thread Usage**, and **Heap Memory** under strict resource constraints (fixed database connection pool).

## 2. Experimental Environment
To reproduce the results reported in the paper (Section 3.1), the following environment is recommended. While exact numerical reproduction depends on hardware, relative performance trends should remain consistent.

### Hardware (Reference Setup)
* **CPU:** Intel Core i7-8550U (4 cores / 8 threads) or equivalent.
* **RAM:** 16 GB DDR4.
* **OS:** Windows 11 Pro / Linux equivalent.

### Software Prerequisites
* **Java:** OpenJDK 21 (LTS) - *Required for Virtual Threads support.*
* **Build Tool:** Maven 3.8+.
* **Database:** Microsoft SQL Server 2022 (running via Docker).
* **Containerization:** Docker Desktop/Engine.

## 3. Configuration & Setup

### 3.1. Database Setup
The experiments require a SQL Server instance with specific resource limits to emulate the constrained environment described in the paper.


# Run SQL Server in Docker with limited resources (8 CPUs, 8GB RAM as per paper)
docker run -e "ACCEPT_EULA=Y" -e "SA_PASSWORD=YourStrong!Passw0rd" \
    --cpus="8.0" --memory="8g" \
    -p 1434:1434 --name sql_server_benchmark \
    -d [mcr.microsoft.com/mssql/server:2022-latest](https://mcr.microsoft.com/mssql/server:2022-latest)

# Experimental Artifacts: Scalability under Resource Constraints
## An Empirical Study of Java Virtual Threads

This repository contains the source code, benchmark scripts, and raw data used in the manuscript **"Scalability under Resource Constraints: An Empirical Study of Java Virtual Threads"**, submitted to *Performance Evaluation*.

**Authors:**
* Bui Quang Huy (FPT University)
* Bui Minh Nhat (East Asia University of Technology)

---

## 1. Overview
This study presents a controlled empirical performance evaluation of Java Virtual Threads compared to Platform Threads in a Spring Boot microservice environment. The experiments focus on two key scenarios:
* **Scenario A (CPU-bound):** Trigonometric operations to validate carrier thread pinning behavior.
* **Scenario B (I/O-bound):** Database queries with simulated network latency to demonstrate thread unmounting.

The goal is to quantify trade-offs in **Throughput**, **Tail Latency (P99)**, **OS Thread Usage**, and **Heap Memory** under strict resource constraints (fixed database connection pool).

## 2. Experimental Environment
To reproduce the results reported in the paper (Section 3.1), the following environment is recommended. While exact numerical reproduction depends on hardware, relative performance trends should remain consistent.

### Hardware (Reference Setup)
* **CPU:** Intel Core i7-8550U (4 cores / 8 threads) or equivalent.
* **RAM:** 16 GB DDR4.
* **OS:** Windows 11 Pro / Linux equivalent.

### Software Prerequisites
* **Java:** OpenJDK 21 (LTS) - *Required for Virtual Threads support.*
* **Build Tool:** Maven 3.8+.
* **Database:** Microsoft SQL Server 2022 (running via Docker).
* **Containerization:** Docker Desktop/Engine.
* **IDE IntelliJ**

## 3. Configuration & Setup

### 3.1. Database Setup
The experiments require a SQL Server instance with specific resource limits to emulate the constrained environment described in the paper.

```bash
# Run SQL Server in Docker with limited resources (8 CPUs, 8GB RAM as per paper)
docker run -e "ACCEPT_EULA=Y" -e "SA_PASSWORD=YourStrong!Passw0rd" \
    --cpus="8.0" --memory="8g" \
    -p 1433:1433 --name sql_server_benchmark \
    -d [mcr.microsoft.com/mssql/server:2022-latest](https://mcr.microsoft.com/mssql/server:2022-latest)
3.2. Application Configuration
Key parameters in application.properties (or passed as JVM arguments) that match the experimental design:

server.tomcat.threads.max: 10,000 (To allow Platform Threads to scale to OS limits).

spring.datasource.hikari.maximum-pool-size: 50 (Strict bottleneck).

spring.datasource.hikari.connection-timeout: 120000 (120s to observe queueing).
4. How to Run the Experiments
First, build the project:


mvn clean package -DskipTests
4.1. Scenario Selection (CPU vs. I/O)
The application exposes endpoints corresponding to the paper's test scenarios:

CPU-Heavy:(Scenario A)

I/O-Wait: (Scenario B - JPA Query + 50ms Sleep)

4.2. Switching Threading Models
You can switch between Platform Threads (PT) and Virtual Threads (VT) using the provided configuration flag:

Run with Platform Threads (Default):


java -jar target/benchmark-app.jar --spring.threads.virtual.enabled=false
Run with Virtual Threads (Project Loom):


java -jar target/benchmark-app.jar --spring.threads.virtual.enabled=true
5. Workload Generation
We used a custom java.util.concurrent benchmarking tool (included in /benchmarker) to generate synthetic load.

To run a benchmark cycle (e.g., 8,000 concurrent users):



run with IDE
Measurements should be averaged over 10 repetitions after a warm-up phase.

6. Repository Structure
data                        # Raw CSV logs used to generate Figures 1-5
/src
  /main/java/com/...       # Spring Boot Application Logic
/research               # Load generation tool source code, Automation scripts for sensitivity analysis.
README.md                  # This file
7. Citation
If you use this artifact or dataset in your research, please cite our paper:

B, Q.Huy(2026). Scalability under Resource Constraints: An Empirical Study of Java Virtual Threads. Submitted to Performance Evaluation.

License
This project is licensed under the MIT License - see the LICENSE file for details.
