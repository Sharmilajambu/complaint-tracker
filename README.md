# Campus Complaint & Escalation Tracker

Core Java backend (built-in `HttpServer`, no frameworks) + HTML/CSS/JS frontend + MySQL database.
Complaints are auto-scored for priority and auto-escalated up the authority chain
(Department -> Warden/HOD -> Principal) if left unresolved past their deadline.

## Tech Stack
- **Backend:** Core Java (`com.sun.net.httpserver.HttpServer`), JDBC
- **Database:** MySQL
- **Frontend:** HTML, CSS, vanilla JavaScript (fetch API)
- No Spring, no Maven/Gradle required - just the JDK + MySQL Connector/J jar

## Project Structure
```
complaint-tracker/
├── schema.sql                     # run this in MySQL first
├── src/main/java/
│   ├── Main.java                  # starts the server + escalation engine
│   ├── db/
│   │   ├── DBConnection.java      # <-- EDIT your MySQL username/password here
│   │   └── ComplaintDAO.java      # all SQL queries
│   ├── model/Complaint.java
│   ├── service/
│   │   ├── PriorityScorer.java    # the scoring algorithm
│   │   └── EscalationService.java # background auto-escalation job
│   ├── handler/
│   │   ├── ComplaintHandler.java  # REST API (/api/complaints)
│   │   └── StaticFileHandler.java # serves the frontend
│   └── util/JsonUtil.java         # tiny hand-written JSON helper
└── public/
    ├── index.html
    ├── style.css
    └── script.js
```

## Quick Start (Windows)

1. **MySQL jar** — already in `lib/` (mysql-connector-j). If missing, copy from your Maven cache or download from https://dev.mysql.com/downloads/connector/j/

2. **Database** — double-click `setup-db.bat` (or run `schema.sql` in MySQL Workbench)

3. **Config** — copy `config.properties.example` to `config.properties` and set your MySQL password:
   ```
   db.password=your_mysql_password
   server.port=8081
   ```
   Port 8081 is used by default because 8080 is often taken by other apps.

4. **Run** — double-click `run.bat` (compiles + starts the server)

5. **Open** — http://localhost:8081 in your browser

## Setup (VS Code)

Install **Extension Pack for Java**. The `.vscode/settings.json` already points at `lib/` for the MySQL driver.

Compile/run manually:
```bash
compile.bat
run.bat
```

Or:
```bash
javac -cp "lib/*" -d out src/main/java/Main.java src/main/java/**/*.java   # simplified
java -cp "out;lib/*" Main        # Windows
java -cp "out:lib/*" Main        # Mac/Linux
```

## How the scoring works
See `PriorityScorer.java`:
- Urgent keywords (fire, shock, harassment, leak...) → +30
- Mild keywords (dirty, noisy, broken...) → +10
- High-risk category (electrical, water, harassment...) → +20 (else +5)
- 3+ similar complaints filed in the last 7 days → +15

Score ≥50 = HIGH (24h deadline), ≥25 = MEDIUM (3 days), else LOW (7 days).

## Authentication & roles

- **Users table:** username, password hash + salt, full name, role (`STUDENT` / `ADMIN` / `AUTHORITY`), plus designation, category, and level for authority accounts
- **Students** self-register (full name, username, password, optional email/phone for authority contact) — role is always `STUDENT`
- **Admin** is auto-seeded on first server startup; credentials are printed in the console (username `admin`, random password). Admin accounts are never self-registered.
- **Authority** accounts are seeded on first startup (one login per Level 0 / Level 1 position per category, plus a single Principal at Level 2). Usernames like `auth_mess_l0`, `auth_principal_l2`, etc. — passwords printed once in the console.
- **Sessions:** in-memory token map (`Authorization: Bearer <token>`); no external auth library
- **Students:** login → submit complaints (name from session, not the form) → see only their complaints
- **Authority:** separate dashboard (`/api/authority/*`) — only complaints assigned to their category + level; Mark Resolved and Call/Contact student
- **Admin:** login → all complaints + escalated panel → only admin can resolve (oversight across all categories)

Category → authority mapping: see **`AUTHORITY_MAPPING.md`** and `AuthorityMapper.java`.

## How escalation works
`EscalationService` runs on a background timer (every 1 minute in this demo -
change the interval in `Main.java`). It checks for OPEN complaints past their
deadline and bumps them to the next authority level, logging the reason in
`escalation_log`.

## Interview talking points
- Built the core Java backend using `HttpServer` (no framework) with REST-style
  JSON endpoints
- Designed a rule-based priority scoring algorithm (keyword + category + repeat-complaint weighting)
- Built a background auto-escalation engine using `java.util.Timer`
- Designed the MySQL schema with a foreign-key escalation audit log
- Frontend (HTML/CSS/JS) calls the API via `fetch()` and renders live data
