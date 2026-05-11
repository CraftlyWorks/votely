# votely-api

The API module contains the shared types used by both the standalone server and Minecraft plugins. Add this as a
dependency in your plugin — it has no heavy runtime deps.

## Dependency

**Gradle (Kotlin DSL)**

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.craftlyworks.votely:votely-api:1.0-RELEASE")
}
```

**Maven**

```xml

<dependency>
    <groupId>com.craftlyworks.votely</groupId>
    <artifactId>votely-api</artifactId>
    <version>1.0-RELEASE</version>
</dependency>
```

## Usage

Subscribe to `VotelyChannel.VOTES` on your Redis connection and deserialize the message into a `Vote`:

```java
redis.subscribe(VotelyChannel.VOTES, message ->{
Vote vote = Vote.deserialize(message);

rewardPlayer(vote.username());
    });
```

## Vote fields

| Field         | Type     | Description                                      |
|---------------|----------|--------------------------------------------------|
| `id`          | `String` | Unique UUID assigned by Votely on receipt        |
| `serviceName` | `String` | Voting site name (e.g. `"MinecraftServers.org"`) |
| `username`    | `String` | Minecraft username of the player who voted       |
| `address`     | `String` | IP address of the voting site                    |
| `timestamp`   | `String` | Timestamp provided by the voting site            |

## Message format

Messages on `VotelyChannel.VOTES` are pipe-separated strings:

```
id|serviceName|username|address|timestamp
```

`Vote.deserialize(String)` and `Vote.serialize()` handle this format — you never need to parse it manually.
