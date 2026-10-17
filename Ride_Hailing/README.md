# Ride-Hailing Facade — Assignment 4 (Java)

A complete Java console application that books a prepaid ride through three independent, durable subsystems. `RideBookingFacade` coordinates A → B → C; `RideAdminFacade` provides inspection, manual compensation and recovery. The project uses Java 17 language features, Maven, SQLite, JUnit 5 and JaCoCo. All source comments, user messages and documentation are in English.

## Open and run in IntelliJ IDEA

1. Extract the code archive. In IntelliJ IDEA choose **File → Open**, select this folder or its `pom.xml`, and open it as a Maven project.
2. Set **Project SDK** to a **JDK 17 or newer** (JDK 21 is also suitable). Allow Maven to import the dependencies. The initial import/build needs access to Maven Central. No database server is needed.
3. Open `src/main/java/com/ridehail/app/Main.java` and click the green Run icon beside `main`. With no arguments, an interactive menu appears in the Run console.
4. Alternatively select the included **Ride-Hailing Menu** or **Ride-Hailing Live Demo** run configuration. The working directory is the project root. If the IDE asks for a module, select the imported `ride-hailing-facade` module.
5. Run tests from the `src/test/java` folder or run Maven's `verify` lifecycle. The Maven command is the authoritative coverage check.

The project layout and Maven build are prepared for IntelliJ import; validation here used the command line, not the IntelliJ desktop UI. The `dist/ride-hailing.jar` included in the delivery is a prebuilt executable with its runtime dependencies, so it can run without a Maven download when Java 17+ is installed.

## One-command launch

From the project root, run the supplied executable:

```sh
java -jar dist/ride-hailing.jar
```

Build and launch directly from source with one command:

```sh
./mvnw -q compile exec:java
```

On Windows use `mvnw.cmd -q compile exec:java`. Maven Wrapper pins Maven 3.9.11 and verifies its distribution checksum. To build a new executable, use `./mvnw verify`; the resulting file is `target/ride-hailing.jar`. Replace `dist/ride-hailing.jar` with that path in subsequent commands. The standard test/coverage command is also `./mvnw verify` (`mvnw.cmd verify` on Windows).

## Domain and boundaries

This is the assigned **Ride-Hailing variant (option 3)**. The independent subsystems have exactly five top-level classes each, fifteen in total:

| Subsystem | Stateful entity | Pure business rules | Own persistence gateway | Public service |
|---|---|---|---|---|
| A: reservation | DriverReservation | DriverMatcher, GpsRouter | ReservationRepository | ReservationService |
| B: processing | FareReceipt | PricingEngine, ReceiptGenerator | FareRepository | FareService |
| C: finalization | PaymentRecord | PaymentProcessor, RatingService | PaymentRepository | PaymentService |

Each subsystem is in its own package and imports neither another subsystem nor the facades/journal. Each gateway directly owns the SQL for its own SQLite file. `SqliteStore` only provides connection mechanics and error translation. The facades exchange simple values such as distance and amount. `AppFactory` constructs and injects the dependencies; it contains no business decisions.

The offline router supports `campus=(0,0)`, `station=(3,4)`, `riverside=(0,6)` and `hospital=(5,1)`. Routes use Manhattan distance, in whole kilometres. Driver IDs are D1, D2 and D3. Fares are integer KZT: `500 + 180 × distanceKm`. Campus → station is 7 km and costs 1760 KZT.

The application confirms a booking, not the physical completion of a ride. `COMPLETED` means the A → B → C booking chain finished. The driver remains reserved by that booking. C records a deferred `AFTER_RIDE:<id>` rating invitation, rather than requesting a rating before the ride. Real maps, real banking, ride completion and refunds are outside this bounded model. `--funds` initializes a **new** passenger wallet only; changing it on later bookings does not top up an existing balance.

## Two facades and the client

`RideBookingFacade.book(request)` performs real sequencing and decisions: reserve A, commit progress, issue B, commit progress, pay C, complete. `resume(id)` reads persisted results before advancing. `cancelPending(id)` protects settled payments from accidental cancellation.

`RideAdminFacade` exposes `inspect`, `inspectAll`, `walletBalance`, `compensate` and `recoverAll`. It aggregates all three persisted subsystem states into plain snapshots and selects unfinished chains for recovery. Manual compensation delegates to the booking facade's shared B → A mechanism. This keeps the administrative interface focused without duplicating critical undo logic.

The application `Client` has only two immediate service dependencies: the booking and administrative facades. `ConsoleMenu` and the command-line entry point use this same client. The subsystems remain publicly usable on their own.

## State transitions

| Entity | Legal lifecycle | Independent enforcement |
|---|---|---|
| DriverReservation | NEW → RESERVED → RELEASED | `reserve()` / `release()` |
| FareReceipt | NEW → ISSUED → VOIDED | `issue()` / `voidReceipt()` |
| PaymentRecord | NEW → SETTLED or NEW → DECLINED | `settle()` / `decline()` |

Status fields are private. Each entity checks its own legal transitions; a facade is not required for enforcement. Repeating the current state is allowed for idempotent retries. Reversing RELEASED, VOIDED or DECLINED to an active/successful state is forbidden. NEW is the initial in-memory entity state; the first durable domain commit contains the business outcome.

The chain journal persists `PENDING_A → PENDING_B → PENDING_C → COMPLETED`; a later rejection follows `COMPENSATING → COMPENSATED`; an initial-stage rejection produces `FAILED`. The compensation intent is committed before undo begins. Events are ordered and unique per `(bookingId, action)`.

## Durable state and recovery

The data directory contains `reservations.sqlite`, `fares.sqlite`, `payments.sqlite`, `chains.sqlite` and `workflow_lock.sqlite`. A, B and C really commit separately; there is no transaction spanning their databases. Within C, wallet debit, payment record and deferred invitation commit atomically. Each connection is closed by try-with-resources. SQLite uses `synchronous=FULL`.

A separate SQLite transaction serializes facade operations against one store. A killed JVM releases that process's lock. No persistent state is represented by a static in-memory list or map. The static location and fleet data are immutable reference inputs, not persisted booking state.

`--stop-after A|B|C` exits after a subsystem commit but **before** its journal update. Thus a genuine restart can find PENDING_A + RESERVED, PENDING_B + ISSUED, or PENDING_C + SETTLED. `resume` rereads the domain stores and repeats idempotent operations. If C already settled, it completes the journal without charging twice. If the journal says COMPENSATING, it continues undo instead of restarting the forward chain.

This is durable compensation, not SQL rollback of an already committed transaction. A becomes RELEASED and B becomes VOIDED; the records remain inspectable. A declined C attempt remains DECLINED, without a wallet debit. Storage failures are reported as infrastructure failures, with unfinished progress available for retry. The program does not claim success while the disk is unavailable or silently repair corrupt files. External direct mutation of subsystem records is outside the facade's coordination protocol; standalone demonstrations should use their own stores.

## Required compensation depths

| Failure | Real input/business cause | Committed before failure | Required undo | Persisted outcome |
|---|---|---|---|---|
| B rejects | 1760 KZT fare exceeds maximum 1000 | A RESERVED | A only | A RELEASED; B/C absent |
| C rejects | New wallet has only 100 KZT | A RESERVED, B ISSUED | B first, then A | B VOIDED; A RELEASED; C DECLINED |

These are reachable business decisions, not artificial failure switches. Additional failures include an occupied/unknown driver, an invalid route, a malformed ID, invalid monetary values, reuse of an ID for a different request, and unavailable storage. Custom `RideException` subclasses represent domain failures; `StorageException` remains separate so uncertain storage results are not misreported as a completed business rollback.

## Manual defense demonstration

Close other processes using the store. Reset preserves the previous directory as a uniquely named backup:

```sh
java -jar dist/ride-hailing.jar reset
java -jar dist/ride-hailing.jar book b-failure --max-fare 1000
java -jar dist/ride-hailing.jar inspect b-failure
java -jar dist/ride-hailing.jar book c-failure --passenger poor --funds 100
java -jar dist/ride-hailing.jar inspect c-failure
java -jar dist/ride-hailing.jar balance poor
```

Show A_RELEASED with no B or C row for B failure. For C failure show `A_RESERVED, B_ISSUED, B_VOIDED, A_RELEASED` in that order, plus an unchanged 100 KZT wallet. `book` returns exit code 2 for the intentional business rejections; execute the next command independently rather than treating that expected code as a build failure.

```sh
java -jar dist/ride-hailing.jar book restart --stop-after B
java -jar dist/ride-hailing.jar inspect restart
java -jar dist/ride-hailing.jar resume restart
java -jar dist/ride-hailing.jar balance passenger1
java -jar dist/ride-hailing.jar book busy-driver
java -jar dist/ride-hailing.jar book manual --driver D2 --stop-after B
java -jar dist/ride-hailing.jar compensate manual
java -jar dist/ride-hailing.jar recover
java -jar dist/ride-hailing.jar inspect
```

Every command starts a new JVM. The interrupted booking reloads PENDING_B and its issued receipt, then completes with exactly one payment and a balance of 8240 KZT. The next D1 booking fails before any subsystem commit because D1 is occupied. The D2 pending booking can be manually compensated. Administrative compensation refuses a settled payment; refunds require a separate workflow.

Use `--data DIRECTORY` before the command to select a separate store. Run `java -jar dist/ride-hailing.jar --data demo-data demo` for an automatic nine-command demo with assertions and a fresh child store on every run. `help` lists all commands. `book` and `resume` return 2 if the workflow ends FAILED/COMPENSATED; successful administrative compensation returns 0.

## Direct use without a facade

These tests instantiate only the named subsystem and its own file store:

```sh
./mvnw -Dtest=ReservationSubsystemTest test
./mvnw -Dtest=PricingSubsystemTest test
./mvnw -Dtest=PaymentSubsystemTest test
```

Each subsystem's test reopens its store and checks results through its public service. This demonstrates that Facade simplifies access without making the underlying APIs inaccessible.

## Principle of Least Knowledge: before and after

A client without the facade reaches through its reservation service and coordinates all three services:

```java
var occupied = reservations.repository().occupied();
int km = reservations.router().distance("campus", "station");
String driver = reservations.matcher().choose("D1", occupied);
var hold = reservations.reserve("r1", driver, "campus", "station");
var receipt = fares.issue("r1", hold.distanceKm(), 5000);
payments.pay("r1", "aliya", receipt.amount(), 10000);
// The client would also need durable progress and compensation logic.
```

The first three lines concretely reach through a friend into its repository, router and matcher. The client also owns the chain's ordering and error-handling burden. Multiple immediate collaborators alone are not automatically a Law of Demeter violation; these deep calls demonstrate the actual issue.

With the facades, the client talks to its two injected immediate friends:

```java
var request = new BookingRequest("r1", "aliya", "D1",
        "campus", "station", 5000, 10000);
booking.book(request);
var snapshot = admin.inspect("r1");
```

The real `Client` follows this structure and renders plain snapshot data. It never retrieves a repository or business-rule object from a facade. Route, pricing or SQL implementation changes do not require the client to reproduce the workflow.

## Facade versus Adapter — reflection

ReservationService, FareService and PaymentService already expose usable Java APIs. The client's difficulty is knowing their order, recording progress and compensating the correct committed subset after failure. RideBookingFacade provides the coherent operation `book`, while RideAdminFacade provides focused inspection and recovery. Their intent is to simplify use and reduce the knowledge a client needs about the system.

An Adapter would address an incompatible interface. For example, a future external bank might accept tiyn rather than KZT or return a different result format. An adapter inside C could translate that contract. It would not, by that translation alone, coordinate driver reservation, receipt issuance or B → A compensation. This project therefore uses Facade. Durable compensation resembles a local orchestration saga and supports the assignment's consistency requirements; it is not the definition of Facade. The distinction is about intent, not class count.

## Tests, F.I.R.S.T. and JaCoCo

`./mvnw verify` runs JUnit 5, creates `target/site/jacoco/index.html`, XML and CSV, builds the executable JAR, and enforces an 80% line-coverage threshold overall, for each A/B/C/facade package and for both facade classes. `./mvnw test` also creates the report, while `verify` additionally enforces its gate. No production package is omitted from measurement.

- **Fast:** deterministic local rules and embedded stores; unit tests use no network, waits or external services. Separate-JVM integration tests are identified separately.
- **Independent:** JUnit `@TempDir` gives every test a clean persistent store; test order is irrelevant.
- **Repeatable:** no time/randomness in booking rules; fixed routes, integer amounts and fresh files.
- **Self-validating:** assertions check persisted states, balances, undo order, idempotency and process exit codes.
- **Timely:** the supplied development Git history contains subsystem code and its JUnit tests in the same actual commits, followed by orchestration/recovery and client commits with their tests. See `GIT_HISTORY.txt` and `development-history.bundle` in the delivery. This is a record of this Java implementation; no earlier history or strict TDD chronology is fabricated.

The custom test language includes `passengerRequestsRide(5000, 100)` and `persistedBookingShouldBe("COMPENSATED", "RELEASED", "VOIDED", "DECLINED")`. Its assertions recreate gateways and read the files. Tests include both compensation depths, actual B-before-A persisted undo order, reload after A/B/C, interruption during compensation, hard JVM halt after B, request conflicts, bad inputs, storage errors, independent subsystem use and the real CLI.

Verified on Java 21 / Linux, compiled for Java 17: **35 JUnit tests passed, 0 failures, 0 errors, 0 skipped.**

| Scope | Covered / executable lines | Line coverage |
|---|---|---|
| reservation | 116/126 | 92.06% |
| pricing | 89/95 | 93.68% |
| payment | 122/131 | 93.13% |
| facade | 83/83 | 100.00% |
| All production code | 739/794 | 93.07% |

Both facade classes individually have 100% line coverage. No production packages are omitted. Line coverage is execution evidence, not proof of all possible inputs or 100% branch coverage.

## UML and report

The class diagram is available as [SVG](docs/uml.svg), [PNG](docs/uml.png) and [editable PlantUML](docs/uml.puml) and [Graphviz layout](docs/uml.dot). It shows the actual Client, both facades, the journal/request/state records and all fifteen subsystem classes with their associations.

The separate English report archive contains PDF and Word reports, a defense guide, UML, fresh JaCoCo HTML/XML/CSV, JUnit summaries, the Maven verification log and the live-JVM transcript. The application source is Java; no Python runtime is needed for the project or the report's demonstration commands.

## Git history and submission

The code archive contains `development-history.bundle`, which preserves the real local commits without copying session-specific Git configuration. Inspect it in a separate folder with `git clone development-history.bundle ../ride-hailing-history`, then use `git log --stat`. `GIT_HISTORY.txt` provides a readable list of the implementation commits and their files.

The configured repository destination is `https://github.com/Orinbassar-Nuradil/Codex-`. Publishing the completed source there has not been performed by creating these archives. Upload/push the source while preserving its history, submit the actual repository link through Moodle before the instructor's deadline, and perform the oral defense/live coding yourself. The package supplies code and preparation, not evidence that those external actions already occurred.

The example was built for the Ride-Hailing domain specified by the student. It does not implement the lecture's Home Theater, Hotel/HotelKeeper, Video Converter, phone-shop operator, XML-page-testing or environment-control examples. Prior Assignments 1–3 and other students' submissions were not supplied; the student must confirm the domain was not used previously and satisfies the instructor's individual-variant requirement.

## References

1. Almas Ospanov, *Lecture 6 — The Facade Design Pattern*, supplied PDF: pp. 4–5, 10–11, 16 and 23–26 for intent, multiple facades and Least Knowledge; pp. 34–35 and 39–42 for clean tests, testing DSL and F.I.R.S.T. Only the principles are used here, not the lecture's worked examples.
2. *Assignment 4 — Facade Over a Three-Stage Chain, With Persistence and a Live Demo*, supplied document, requirements 3.1–3.10 and variant 3.
3. [JUnit 5 User Guide](https://docs.junit.org/5.13.4/user-guide/).
4. [JaCoCo Maven Plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html).
5. [SQLite Atomic Commit](https://www.sqlite.org/atomiccommit.html).
