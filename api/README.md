# votely-api

The API module contains the shared types used by both the standalone server and Minecraft plugins. Add this as a
dependency in your plugin — it has no heavy runtime deps.

## Dependency

```kotlin
implementation("com.craftlyworks.votely:api:1.0-RELEASE")
```

## Usage

Subscribe to `VotelyChannel.VOTES` on your Redis connection and deserialize the message into a `Vote`:

```java
redis.subscribe(VotelyChannel.VOTES, message ->{
Vote vote = Vote.deserialize(message);
    System.out.

println(vote.getUsername() +" voted via "+vote.

getServiceName());
    });
```

## Message format

Messages on `VotelyChannel.VOTES` are pipe-separated strings:

```
id|serviceName|username|address|timestamp
```

`Vote.deserialize(String)` and `Vote.serialize()` handle this format — you never need to parse it manually.