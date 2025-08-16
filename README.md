# espaoil-server

A Kotlin app that updates gas station data and exposes it via a lightweight HTTP API.

API (pure kotlin)
- Server: JDK HttpServer started inside the app process.
- Port: PORT env var (default 8080).
- JSON: Gson.

Endpoints
- GET /health → { "status": "ok" }
- GET /gas-stations/near?lat={double}&lon={double}&distance={int?}&gasType={string?}

Run locally
- Requirements: Java 17 and MongoDB reachable by DATABASE_URL.
- Env vars:
  - DATABASE_URL (e.g. mongodb://user:password@localhost:27017)

- Commands:
```
./gradlew run
# or build fat JAR
./gradlew shadowJar
java -jar app/build/libs/app-all.jar
```

Quick smoke tests
```
curl -s http://localhost:8080/health
curl -s "http://localhost:8080/gas-stations/near?lat=40.4168&lon=-3.7038&distance=5000&gasType=GASOLINA_95_E5" | jq '. | length'
# short key works too:
curl -s "http://localhost:8080/gas-stations/near?lat=40.4168&lon=-3.7038&distance=5000&gasType=95_E5" | jq '. | length'
```

Docker dev
- docker/docker-compose.dev.yml provides Mongo and the app. Ensure .env exists with DATABASE_URL.
- Makefile helpers:
```
make up
make logs
make down
```

Tests
- Some tests use Testcontainers + Docker.
```
./gradlew test
```
