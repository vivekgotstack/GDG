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
