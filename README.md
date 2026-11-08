# Exchange Rates Provider

A Spring Boot service that returns historical exchange rates for a given day. Rates come from
[Fixer](https://fixer.io), are stored in PostgreSQL on first request, and are served from the database afterwards,
converted to USD as the base currency.

## API

### `GET /api/v1/rates/{day}`

Returns the exchange rates of every currency Fixer provides for `day`, with **USD** as the base currency: each rate is
how many units of that currency one US dollar buys. Currencies are sorted alphabetically.

**`day` format:** ISO-8601 calendar date `yyyy-MM-dd`, e.g. `2026-11-06`.

- It must be a real date, so `2023-02-29` and `2022-13-01` are rejected.
- It must not be later than today, judged in **UTC**.
- Anything else (`20220620`, `2022-6-20`, `06-11-2026`, `abc`) is rejected.

**Rounding:** rates are rounded half-up to 2 decimal places and always have at least 2. Rates below 0.1 would lose all their digits that way, so they keep their first 2 non-zero decimal
digits instead: `0.0000131964` becomes `0.000013`. This is mainly because of high-value currencies such as Bitcoin.

### Responses

| Status | When | Body |
|---|---|---|
| `200 OK` | Rates found or fetched | `{ "date", "base", "rates" }` |
| `400 Bad Request` | `day` is malformed, impossible or in the future | [`ProblemDetail`](https://www.rfc-editor.org/rfc/rfc9457) with the reason in `detail` |
| `404 Not Found` | Unknown path | Spring's default error |
| `503 Service Unavailable` | Rates are not stored yet and could not be fetched from Fixer | `ProblemDetail`, e.g. `Exchange rate snapshot for 2026-11-06 is unavailable` |

## How it works

```
GET /api/v1/rates/2026-11-06
        │
        ▼
 1. Validate day ─────────────── invalid ──▶ 400
        │
        ▼
 2. Snapshot for the day in DB? ── yes ─────────────────────────┐
        │ no                                                    │
        ▼                                                       │
 3. Fetch from Fixer (base EUR)                                 │
      HTTP 429 / code 106 → retry after 1 s, 2 s, 4 s           │
      still failing ──────────────────────────────▶ 503         │
        │                                                       │
        ▼                                                       │
 4. Store snapshot in EUR ──────────────────────────────────────┤
                                                                ▼
                                          5. Convert to USD and round
                                                                │
                                                                ▼
                                                               200
```

1. **Validation.** Bean Validation annotations on the controller check the format and that the day is not in the
   future. The manager checks the future-date rule again through `ValidatingEngine`, so callers that skip the
   controller are protected too.
2. **Database first.** A snapshot is stored once per day (unique constraint on `date`), so each day costs at most one
   successful Fixer call.
3. **Fetch.** Fixer's free plan only offers EUR as the base currency. If Fixer reports its rate limit (HTTP 429 or error
   code `106`), the request is retried up to 3 times, waiting 1 s, 2 s and 4 s. Other errors (invalid key, bad date,
   network failure) are not retried.
4. **Store.** Snapshots are stored exactly as Fixer returned them, in EUR, so they can later be converted to any base
   without fetching again.
5. **Convert and round.** `rate in USD = rate in EUR / EUR→USD rate`, computed with 16 significant digits, then rounded
   once at the end.

Two requests for the same new day at the same time may both fetch from Fixer. The second insert hits the unique
constraint, and that request returns the snapshot the first one stored. The manager runs outside any transaction, so the
HTTP call never holds a database connection and the failed insert can be recovered from.

## Architecture

The code is layered along the lines of [IDesign](https://www.idesign.net/) (Clients → Managers → Engines /
ResourceAccess). Each layer depends only on the layers below it and talks to them through interfaces.

```
┌──────────────────────────────────────────────────────────────┐
│ client        ExchangeRatesController, ExchangeRatesResponse │  HTTP, validation, error mapping
└──────────────────────────────┬───────────────────────────────┘
                               │ RatesSnapshotData
┌──────────────────────────────▼───────────────────────────────┐
│ manager       RatesManager                                   │  orchestrates one use case
└───────┬───────────────────────┬──────────────────────┬───────┘
        │                       │                      │
┌───────▼────────┐  ┌───────────▼──────────┐  ┌────────▼──────────────────────┐
│ engine         │  │ engine               │  │ resourceaccess                │
│ Validating-    │  │ ComputingEngine      │  │ FixerResourceAccess (HTTP)    │
│ Engine         │  │ (conversion,         │  │ RatesSnapshotResourceAccess   │
│ (day rules)    │  │  rounding)           │  │   (JPA, PostgreSQL)           │
└────────────────┘  └──────────────────────┘  └───────────────────────────────┘
```

| Package | Responsibility |
|---|---|
| `client` | REST controller and response DTO. Validates input with annotations and maps exceptions to HTTP status codes. |
| `manager` | `RatesManager`: validate, read from DB or fetch and store, convert. Contains no business rules of its own. |
| `engine` | Business rules without I/O. `ValidatingEngine` decides which days are valid; `ComputingEngine` converts between base currencies and rounds. `BaseCurrency` lists the supported target currencies (only `USD` for now). |
| `resourceaccess` | External resources. `FixerResourceAccess` calls the Fixer API, including the retry; `RatesSnapshotResourceAccess` is a Spring Data repository exposing only `save` and `findByDate`. JPA entities live in `resourceaccess.entity`. |
| `model` | `RatesSnapshotData`, the plain record (date, base currency, rates) passed between layers. |
| `config` | `TimeConfig`: the shared UTC `Clock`, also used by Bean Validation's `@PastOrPresent`. |

### Design decisions

- **Entities stay inside the manager.** The JPA entities never leave the manager. Converted rates are plain
  `RatesSnapshotData` records, so they can never be saved by accident.
- **One clock, in UTC.** Fixer dates its rates in UTC. Both the controller validation and `ValidatingEngine` read "today"
  from the same `Clock` bean, so they cannot disagree around midnight, and tests can replace it with a fixed clock.
- **Rounding only at the end.** Stored rates are never rounded by the app, and conversion uses full precision, so
  rounding errors do not add up.
- **Precision of 6 decimal places in the database** (`NUMERIC(19, 6)`), chosen because of very small rates such as BTC.
  See [Future improvements](#future-improvements) for why this is not quite enough.

### Data model

Managed by Flyway (`src/main/resources/db/migration`); Hibernate only validates the schema.

```
rates_snapshot                              rate
──────────────────────────────              ─────────────────────────────────────
id             BIGINT PK                ┌── rates_snapshot_id  BIGINT FK
date           DATE     UNIQUE          │   id                 BIGINT PK
base_currency  VARCHAR(3)               │   currency           VARCHAR(3)
fetched_at     TIMESTAMPTZ              │   rate               NUMERIC(19, 6)
                     1 ─────────────── N┘   UNIQUE (rates_snapshot_id, currency)
```

## Running the app

### Prerequisites

- A Fixer access key. The free plan is enough, but it limits both the number of calls per month and how quickly you can
  make them.
- Docker with Docker Compose, **or** Java 17 for running outside Docker. Maven is not needed; use the included `./mvnw`.

### 1. Provide the Fixer access key

Create a `.env` file in the project root. It is git-ignored and excluded from the Docker image.

```properties
FIXER_ACCESS_KEY=your-access-key
```

### 2a. Run everything in Docker

```sh
docker compose up -d --build
```

This builds the app image from the current working tree (uncommitted changes included) and starts PostgreSQL and the
app. The app waits for the database to report healthy, and Flyway creates the schema on first start.

```sh
docker compose ps                 # both services "Up", database "(healthy)"
docker compose logs -f app        # wait for "Started TestingdayExchangeRatesApplication"
```

With the app running:

- API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI document: <http://localhost:8080/v3/api-docs>

Stored rates live in the named Docker volume `db-data`.

The image build skips tests because some of them need a database. Run `./mvnw test` first if you want the build checked.

### 2b. Run locally (for development)

Start only the database in Docker, then run the app with Maven:

```sh
docker compose up -d database
./mvnw spring-boot:run
```

The app reads `FIXER_ACCESS_KEY` from `.env` in the working directory, or from an environment variable of the same name,
which takes precedence.

### 3. Try it

```sh
curl http://localhost:8080/api/v1/rates/2026-11-06
```

## Tests

```sh
./mvnw test
```

| Test | Type | Needs PostgreSQL |
|---|---|---|
| `ExchangeRatesControllerTest` | `@WebMvcTest` slice with a mocked manager and a fixed clock. Covers `day` validation: today and past days succeed; empty, impossible, malformed, non-date and future values fail. | No |
| `RatesManagerImplTest` | Mockito unit test using a sample Fixer response (`src/test/resources/fixer/`). Covers database hit, fetch and store, validation and fetch errors, concurrent insert, conversion to USD and rounding. | No |
| `TestingdayExchangeRatesApplicationTests` | Starts the full application context. | **Yes** |

Tests never call Fixer: the test configuration uses a dummy access key, and the Fixer client is mocked.

## Future improvements

- **Store rates with more precision.** `NUMERIC(19, 6)` was chosen with BTC in mind, but it still cuts very small rates
  short: Fixer's `BTC = 0.000014714976` is stored as `0.000015`, about 2 % off, and gold (`XAU`) loses precision as
  well. A migration to something like `NUMERIC(30, 15)` would fix this. Already stored snapshots would have to be
  fetched again to regain their precision.
- **Today's rates are frozen.** Fixer updates the current day's rates during the day, but the first request for today
  stores them permanently. One option is to cache today with TTL=1h as the free plan offers hourly updates.
- **Don't block threads while retrying.** A retried request holds its server thread for up to about 7 seconds, which is
  fine for low traffic but not under load.
- **Make the retry configurable and testable.** Delays and timeouts are constants, and the retry has no automated test.
- **Tell failures apart.** Every unavailable snapshot returns `503`. A non-retryable failure such as an invalid access
  key arguably deserves a `502` or `500`, and is currently only distinguishable in the logs.
- **Use HTTPS.** The access key travels in the query string over plain HTTP and the Fixer free plan doesn't support HTTPS.
- **Use Testcontainers** for tests that need PostgreSQL, so `./mvnw test` works without a manually started database
  and can run inside the Docker build. Testcontainers can be later used more during integration testing.
- **Add tests for** `FixerResourceAccessImpl` (against a local stub server, including the retry),
  `ComputingEngineImpl` and `ValidatingEngineImpl`, and the repository queries.
- **Move credentials out of the code.** Database credentials are hard-coded in `application.yml` and
  `docker-compose.yml`, which is fine for development but should come from the environment or a secret store elsewhere.
- **Add Spring Boot Actuator** for health checks and metrics, and use its health endpoint as the app container's
  healthcheck.
- **Introduce dev/test/prod profiles** Some features such as Swagger should be disabled in production and with more tests, specific configuration would be a nice-to-have.
- **Logging**
