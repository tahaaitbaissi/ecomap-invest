# Running tests

- **Default** (`mvn test`): unit and slice tests only. Integration tests tagged with `integration` are **skipped** (no Docker required).
- **All tests** including full `SpringBootTest` with PostGIS + Redis Testcontainers:
  ```bash
  mvn -pl backend -am test -Dtest.excluded.groups=
  ```
  Requires **Docker** running locally.

CI should use `-Dtest.excluded.groups=` so integration tests run in the pipeline.
