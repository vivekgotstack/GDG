# MeetGrid

MeetGrid helps student teams find a shared time and a room in one search.

People availability + resource availability + capacity constraints + duration constraints = feasible meeting options.

The Next.js App Router frontend presents a pastel, student-friendly planner with editable weekly availability, a visual overlap timeline, explainable room results, a room directory, and a conflict demo. The Spring Boot API owns deterministic interval intersection and room matching. PostgreSQL is supported through the `postgres` profile; the default local profile uses an in-memory H2 database so the demo starts without setup.

## Run locally

Start the API:

```powershell
cd backend
mvn spring-boot:run
```

In another terminal start the frontend:

```powershell
cd frontend
npm install
npm run dev
```

Open http://localhost:3000. The API exposes `GET /api/members`, `GET /api/rooms`, `POST /api/meeting-options/search`, `GET /api/bookings`, `POST /api/bookings`, and `PUT /api/members/{id}/availability`.

For PostgreSQL, start `docker compose -f compose.yml up -d` and run the API with the `postgres` profile and `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` configured.

## Demo flow

1. The seeded workspace loads four students and five rooms with two valid one-hour options.
2. Expand a result to see available rooms and exact rejection reasons for the others.
3. Open “Try a room conflict”, add Lab 2 on Tuesday from 2:00–3:00 PM, and watch the Tuesday option disappear.
4. Reset the demo to restore the original data.

Interval calculations use half-open ranges (`start < existingEnd && end > existingStart`), so touching availability and adjacent bookings remain valid. Candidate starts align to 30-minute boundaries and rooms must satisfy capacity, opening hours, and booking checks.

## Verification

The backend suite covers interval intersection, duration, capacity, opening boundaries, booking conflicts, adjacent bookings, and the conflict demo. The frontend production build passes with Next.js 16 and TypeScript.

## Render Docker deployment (backend)

Create a Render Web Service from this repository, select **Docker**, branch `main`, leave **Root Directory empty**, and use **Dockerfile Path: ./Dockerfile** with **Docker Build Context: .**. Leave the Docker Command empty; the image starts the API automatically. Set the health check path to `/api/rooms`.

The image builds with Maven and Java 21, runs as a non-root user, binds to `0.0.0.0`, and reads Render's `PORT` environment variable (default `10000`).

For an immediate demo, no database variables are needed: H2 is seeded on startup and data resets on restart. For persistent PostgreSQL, set `SPRING_PROFILES_ACTIVE=postgres`, `DB_URL=jdbc:postgresql://HOST:5432/DATABASE`, `DB_USERNAME`, and `DB_PASSWORD` in Render. Use the JDBC format for `DB_URL`, not Render's `postgresql://user:password@host/database` URL.

Deploy the Next.js frontend separately and set `API_BASE_URL=https://YOUR-BACKEND.onrender.com` **before building** it, then redeploy the frontend so its API proxy points to Render.

Local Docker check:

```sh
docker build -t meetgrid-api .
docker run --rm -p 10000:10000 meetgrid-api
# Open http://localhost:10000/api/rooms
```
