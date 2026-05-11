# Votely

Votely is a standalone Votifier server that receives player votes from voting sites and forwards them to your Minecraft
servers over Redis pub/sub.

## Modules

| Module                      | Description                                                  |
|-----------------------------|--------------------------------------------------------------|
| [`api`](api/)               | Shared types for plugin developers — `Vote`, `VotelyChannel` |
| [`standalone`](standalone/) | The runnable Votifier server                                 |

## How it works

```
Voting site  →  Votifier protocol  →  standalone  →  Redis  →  your Minecraft plugin
```

1. A player votes on a site like MinecraftServers.org.
2. The site sends an encrypted Votifier packet to the standalone server.
3. The standalone decrypts it, assigns a unique ID, and publishes it to Redis.
4. Your plugin subscribes to `VotelyChannel.VOTES` and handles the vote.