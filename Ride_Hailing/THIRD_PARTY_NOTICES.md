# Third-party components

The application uses the following independently distributed components. Their original notices remain in the dependency JAR metadata where supplied.

| Component | Version | Purpose | License / project |
|---|---|---|---|
| SQLite JDBC (Xerial) | 3.50.3.0 | Embedded durable database | Apache-2.0; native SQLite is public domain. https://github.com/xerial/sqlite-jdbc |
| SLF4J API and NOP binding | 2.0.17 | JDBC logging API with a quiet runtime binding | MIT. https://www.slf4j.org/ |
| JUnit Jupiter | 5.13.4 | Tests only | EPL-2.0. https://junit.org/ |
| JaCoCo | 0.8.13 | Coverage instrumentation/reporting only | EPL-2.0. https://www.jacoco.org/ |
| Apache Maven Wrapper | 3.3.4 | Reproducible Maven bootstrap | Apache-2.0. https://maven.apache.org/wrapper/ |
| Apache Maven | 3.9.11 | Build tool, downloaded by the wrapper | Apache-2.0. https://maven.apache.org/ |

The shaded executable includes runtime dependencies, not the JUnit or JaCoCo build tools. Maven's shade plugin removes dependency signature metadata that no longer describes the transformed combined archive; the original dependency checksums and TLS verification remain enabled during resolution. This does not change the dependencies' licenses.
