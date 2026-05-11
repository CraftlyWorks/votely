# votely-standalone

The standalone Votifier server. Run this on your network alongside Redis — it listens for incoming votes, decrypts them,
and publishes them to Redis for your plugins to consume.

Configuration and Redis connectivity are powered by [Configra](https://github.com/CraftlyWorks/configra).

## Installation

**Prerequisites**

- Java 21 or newer
- A running Redis server

**1. Build the jar**

```bash
./gradlew :standalone:shadowJar
```

The output is at `standalone/build/libs/standalone-1.0-RELEASE-all.jar`. Copy it wherever you want to run it from.

**2. Run it**

```bash
java -jar standalone-1.0-RELEASE-all.jar
```

On first run, Votely creates two things automatically:

- `config.yml` — edit this before running again in production
- `rsa/public.key` + `rsa/private.key` — your Votifier RSA key pair

**3. Configure it**

Open `config.yml`, point it at your Redis server, and set the port voting sites should connect to.
See [Configuration](#configuration) below.

**4. Connect a voting site**

Open `rsa/public.key`, copy its contents, and paste it into the "public key" field on your voting site.
See [Setting up a voting site](#setting-up-a-voting-site) below.

That's it — votes will start flowing to Redis as soon as the site verifies the connection.

## Configuration

```yaml
redis:
  host: localhost
  port: 6379
  prefix: ""

votifier:
  port: 8192          # port voting sites connect to
  key-dir: "./rsa"    # where the RSA key pair is stored

votes:
  channel: "votely:votes:incoming"  # Redis channel to publish votes on
```

## Setting up a voting site

1. Copy the contents of `rsa/public.key`.
2. Paste it into the "Server public key" field on your voting site.
3. Set the host and port to match your server's IP and `votifier.port`.

Votes will start arriving as soon as the site verifies the connection.

## Logs

Logs are written to the `logs/` directory next to the jar.

| File                       | Description                        |
|----------------------------|------------------------------------|
| `logs/latest.log`          | The current session's log          |
| `logs/2025-01-15-1.log.gz` | Previous sessions, gzip-compressed |

On each startup the previous `latest.log` is compressed and archived automatically, so you never lose a session's
output. Archived logs follow the `yyyy-MM-dd-N.log.gz` naming pattern — if multiple sessions ran on the same day, N
increments.