# Tutorial 01 — Setup

What Spring Boot is, generating the project, what every file is for,
running it, and dev reload.

The same notes, shorter, live in the header comment of
`src/main/java/com/example/bookshop/BookshopApplication.java`.

---

## 1. Why Spring Boot exists

You already know plain Java and JDBC. Imagine building a web backend
with only that. Before you write one line of bookshop logic you must:

- open a network socket and accept connections
- read raw HTTP text ("GET /books HTTP/1.1...") and parse it
- decide which of your methods handles which URL
- turn your Java objects into JSON text by hand
- open, reuse, and close database connections without leaking them
- do all of it on many threads at once

That is months of plumbing, and every company would write the same
plumbing. A **framework** is that plumbing, written once, shared by
everyone.

**Analogy.** Your Java programs so far were like cooking at home: you
do everything, in the order you choose. A framework is a restaurant
kitchen that hires you as a chef. The kitchen already runs — ovens hot,
orders coming in. You only write the recipes (your classes), hand them
over, and the kitchen calls YOU when an order arrives.

That inversion is the one big mental shift:

```
+---------------------------+---------------------------------------+
| A library                 | A framework                           |
+---------------------------+---------------------------------------+
| You call it.              | It calls you.                         |
| You own main() and the    | It owns the loop. You hand it classes |
| flow of the program.      | and it runs them at the right moment. |
| Example: java.util.List   | Example: Spring                       |
+---------------------------+---------------------------------------+
```

**Spring vs Spring Boot.** Spring is the framework. It is old, huge,
and endlessly configurable — and setting it up by hand is famously
painful. Spring Boot is Spring with the decisions already made:

```
+--------------------------+--------------------------------------+
| Spring alone             | Spring Boot                          |
+--------------------------+--------------------------------------+
| You pick 30 library      | One "starter" pulls a tested set of  |
| versions that must agree | versions that are known to agree     |
| You install and          | A web server (Tomcat) is embedded    |
| configure a web server   | inside your app and starts itself    |
| Configuration first,     | Sensible defaults; you only          |
| lots of it               | configure what you want to change    |
+--------------------------+--------------------------------------+
```

One term you will meet constantly, defined now:

> A **web server** is a program that listens on a network port,
> speaks the HTTP protocol, and hands each request to your code.
> Ours is called Tomcat, and it runs *inside* our app.

> A **port** is a number (0–65535) that lets one machine run many
> network programs at once. Ours listens on port 8080.

---

## 2. Generating the project

Nobody writes a Spring Boot project from an empty folder. The official
generator at https://start.spring.io produces the skeleton. You fill a
form, it gives you a zip.

What I chose, and why:

```
+------------------+--------------------+--------------------------------+
| Setting          | Value              | Why                            |
+------------------+--------------------+--------------------------------+
| Project          | Maven              | The build tool we agreed on    |
| Language         | Java               |                                |
| Spring Boot      | 4.1.1 (default)    | Latest stable                  |
| Group            | com.example        | Reversed-domain naming, like   |
|                  |                    | package names                  |
| Artifact         | bookshop           | The project's short name       |
| Package name     | com.example.bookshop | Root package of all our code |
| Packaging        | Jar                | One runnable file, server      |
|                  |                    | included                       |
| Java             | 25                 | What you have installed        |
+------------------+--------------------+--------------------------------+
| Dependencies     | Spring Web         | HTTP endpoints + embedded      |
|                  |                    | Tomcat                         |
|                  | Spring Boot        | Auto-restart while developing  |
|                  | DevTools           | (section 6)                    |
+------------------+--------------------+--------------------------------+
```

> A **dependency** is a library your project needs. Maven downloads it
> for you instead of you hunting for jar files.

The same thing from the terminal (this is the exact command that
generated this project):

```bash
curl -s https://start.spring.io/starter.zip \
  -d type=maven-project -d language=java \
  -d groupId=com.example -d artifactId=bookshop -d name=bookshop \
  -d packageName=com.example.bookshop -d javaVersion=25 \
  -d dependencies=web,devtools -o starter.zip
unzip starter.zip && rm starter.zip
chmod +x mvnw          # make the wrapper script runnable
```

---

## 3. What every generated file is for

```
.
├── mvnw                  <- run Maven WITHOUT installing Maven (mac/linux)
├── mvnw.cmd              <- same, for Windows
├── .mvn/wrapper/         <- config telling mvnw which Maven version to fetch
├── pom.xml               <- THE build file. Section 4.
├── HELP.md               <- generated link list. Safe to delete.
├── .gitignore            <- files git should never track (target/, IDE files)
├── .gitattributes        <- line-ending rules for git. Ignore for now.
└── src
    ├── main
    │   ├── java/com/example/bookshop/
    │   │   └── BookshopApplication.java   <- the entry point (main method)
    │   └── resources
    │       ├── application.properties     <- configuration (tutorial 4)
    │       ├── static/                    <- files served as-is (css, images)
    │       └── templates/                 <- server-rendered HTML (unused;
    │                                         we build a JSON API)
    └── test
        └── java/com/example/bookshop/
            └── BookshopApplicationTests.java  <- first test (tutorial 12)
```

After the first build a `target/` folder appears. It holds compiled
`.class` files and the packaged jar. It is generated output — never
edit it, never commit it (`.gitignore` already excludes it).

**Why `mvnw` matters.** You do not have Maven installed. That is fine:

```
+---------------------------+----------------------------------------+
| mvn (installed Maven)     | ./mvnw (the Maven Wrapper)             |
+---------------------------+----------------------------------------+
| Everyone must install it, | Ships inside the repo. First run       |
| possibly different        | downloads the one pinned Maven version |
| versions                  | to ~/.m2/wrapper and reuses it         |
| "Works on my machine"     | Same build for every machine and CI    |
+---------------------------+----------------------------------------+
```

Rule of thumb: if a project has `mvnw`, always use `./mvnw`, never
`mvn`.

---

## 4. pom.xml in plain words

> **Maven** is a build tool: it downloads dependencies, compiles,
> runs tests, and packages the app — driven by one file, `pom.xml`.

The four parts of our pom that matter:

**1. The parent**

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.1</version>
</parent>
```

Our project inherits from Spring Boot's parent. Its main job: it holds
a giant list of library versions that are tested to work together.
That is why the dependencies below have no `<version>` tag — the
parent already decided.

**2. The dependencies**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

> A **starter** is a bundle-dependency: one entry that pulls in a
> whole set of libraries for a purpose (here: web server + HTTP
> handling + JSON conversion).

Note for reading other tutorials: this starter was called
`spring-boot-starter-web` for years and was renamed to
`spring-boot-starter-webmvc` in Spring Boot 4. Same thing. Most
material online still shows the old name.

**3. devtools** — dev-only helper, section 6. Marked `optional` so it
never leaks into a real deployment.

**4. The plugin** — `spring-boot-maven-plugin` teaches Maven two
tricks: `./mvnw spring-boot:run` (run the app) and packaging the
runnable jar (tutorial 17).

---

## 5. Run it

From the project root:

```bash
./mvnw spring-boot:run
```

The first run downloads every dependency (mine made 184 downloads and
took under a minute; a slow network takes a few minutes). After that,
starts are instant because everything is cached in `~/.m2/repository`.

What actually appeared (trimmed to the lines that matter):

```
 :: Spring Boot ::                (v4.1.1)

INFO ... : Starting BookshopApplication using Java 25.0.4.1 with PID 6127
INFO ... : No active profile set, falling back to 1 default profile: "default"
INFO ... : Tomcat initialized with port 8080 (http)
INFO ... : Starting Servlet engine: [Apache Tomcat/11.0.24]
INFO ... : Tomcat started on port 8080 (http) with context path '/'
INFO ... : Started BookshopApplication in 0.427 seconds (process running for 0.54)
```

Reading it line by line:

- `Starting ... with PID 6127` — your app is a normal Java process.
- `No active profile set` — profiles are named config sets (tutorial 4).
- `Tomcat started on port 8080` — the embedded web server is live.
- `Started BookshopApplication in 0.427 seconds` — ready. Note the
  program does NOT exit. Tomcat's threads keep it alive, waiting for
  requests. This is the difference from every `main` you wrote before.

**Test it.** In a second terminal (or a browser at
http://localhost:8080/):

```bash
curl -i http://localhost:8080/
```

Real response:

```
HTTP/1.1 404
Content-Type: application/json

{"timestamp":"2026-09-03T08:38:27.896Z","status":404,"error":"Not Found",
 "trace":"org.springframework.web.servlet.resource.NoResourceFoundException:
          No static resource  for request '/'. ...(long stack trace)...",
 "message":"No static resource .","path":"/"}
```

A 404 is the CORRECT result today, and it proves three things:

1. The server is up and answered over HTTP.
2. We registered nothing at `/`, so Spring's built-in error handler
   responded — and it responded in JSON, because Spring Web is set up
   for JSON out of the box.
3. The `trace` field (a full stack trace) only appears because
   devtools marks this as a development machine. In production that
   would be hidden.

Tutorial 2 replaces this 404 with our first real endpoint.

**Stop it** with `Ctrl+C` in the terminal running it.

**VS Code alternative:** open `BookshopApplication.java` and click
"Run" above the main method. Same thing; the terminal way works
everywhere.

---

## 6. Dev reload (devtools)

Without devtools the loop is: stop the app, rebuild, start, wait for
the JVM. Devtools shortens it: it watches `target/classes`, and when a
compiled class changes it restarts just your app inside the already
warm JVM.

Real demo. With the app running, I changed a source file and compiled
it from a second terminal (`./mvnw compile`) at 13:39:00. The running
app noticed on its own:

```
13:39:01 INFO ... [ File Watcher] : Restarting due to 1 class path change
                                    (0 additions, 0 deletions, 1 modification)
13:39:02 INFO ... : Started BookshopApplication in 0.065 seconds
```

First start: 0.427 s plus JVM startup. Reload: 0.065 s. That is the
whole point.

Two things to know:

- Devtools reacts to changed **compiled** classes, not saved `.java`
  files. Something must compile them. In VS Code the Java extension
  compiles automatically when you save, so in practice: save file →
  app restarts. Watch the app terminal to see it happen.
- Devtools disables itself automatically when you run a packaged jar
  in production. You never ship it by accident.

---

## 7. Common beginner mistakes (all reproduced for real)

**Mistake 1 — running Maven from the wrong folder.** I ran
`../mvnw spring-boot:run` from inside `src/`. The message does not say
"wrong folder"; it says this:

```
[ERROR] No plugin found for prefix 'spring-boot' in the current project
        and in the plugin groups ...
```

Maven only knows about the spring-boot plugin from `pom.xml`. No
`pom.xml` in the current folder → no plugin → this message. Fix: run
`./mvnw` from the folder that contains `pom.xml`.

**Mistake 2 — starting the app twice.** Two programs cannot listen on
the same port. Starting a second copy while one runs:

```
***************************
APPLICATION FAILED TO START
***************************

Description:

Web server failed to start. Port 8080 was already in use.

Action:

Identify and stop the process that's listening on port 8080 or
configure this application to listen on another port.
```

This one is friendly. It usually means an older copy is still running
in a forgotten terminal (or crashed without freeing the port). Find
and stop it:

```bash
lsof -i :8080        # shows the PID listening on 8080
kill <PID>
```

**Mistake 3 — `zsh: permission denied: ./mvnw`.** The wrapper script
lost its "executable" file permission (can happen when unzipping).
Fix: `chmod +x mvnw`.

---

## 8. Recap

- Spring Boot = the Spring framework + pre-made decisions + an
  embedded web server. It calls your code; you stop owning `main`.
- `./mvnw` builds and runs everything; you never install Maven.
- `pom.xml` declares WHAT you need; the parent picks versions;
  starters bundle libraries.
- `./mvnw spring-boot:run` starts the app; port 8080; Ctrl+C stops it.
- Devtools restarts the app in ~0.07 s whenever classes recompile.

Next: [**Tutorial 02 — First endpoint.**](tutorial-02%20%28First%20endpoint%29.md) We make `GET /books` return
real JSON and follow a Java object on its way out the door.
