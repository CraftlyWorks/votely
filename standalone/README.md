# votely-standalone

The standalone Votifier server. Run this on your network alongside Redis — it listens for incoming votes, decrypts them,
and publishes them to Redis for your plugins to consume.

Configuration and Redis connectivity are powered by [Configra](https://github.com/CraftlyWorks/configra).

## Running

Build the fat jar and run it:

```bash
./gradlew :standalone:shadowJar
java -jar standalone/build/libs/standalone-1.0-RELEASE-all.jar
```

On first run a `config.yml` and an RSA key pair are generated automatically.

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
3. Set the IP and port to match your `votifier.port`.

Votes will start arriving as soon as the site verifies the connection.