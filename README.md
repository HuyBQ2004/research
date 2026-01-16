# Empirical Study of Java Virtual Threads under Resource Constraints

This repository contains the experimental artifacts used in the study
"Scalability under Resource Constraints: An Empirical Study of Java Virtual Threads",
submitted to the Journal of Systems and Software.

## Overview
The repository provides a reference implementation of the benchmark
application and supporting scripts used to evaluate Java Virtual Threads
and Platform Threads under constrained infrastructure conditions.

The experiments are designed to expose contention and scalability limits
in a microservice setting with a fixed-size downstream bottleneck,
allowing the behavior of different concurrency models to be observed
under sustained overload.

## Code Organization
The main benchmark application is implemented as a Spring Boot service.
Request handling, database access, and measurement logic are grouped by
functionality within the application package. Supporting scripts and
configuration files are provided separately.

## Experimental Setup
The benchmark application connects to a relational database with a
fixed-size connection pool to simulate hard downstream constraints.
Workloads generate high levels of concurrent requests to evaluate
resource usage, throughput, and tail latency under different threading
models.

## Reproducibility Notes
The artifacts are provided to support transparency and reproducibility
of the reported results. Exact numerical reproduction is not guaranteed
and may vary depending on hardware, operating system, JVM version, and
runtime configuration.

## Requirements
- Java 21
- Maven
- A relational database (e.g., SQL Server or compatible)

## License
This repository is provided for academic and research purposes.
