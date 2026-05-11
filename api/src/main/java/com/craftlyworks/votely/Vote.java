package com.craftlyworks.votely;

/**
 * Represents a player vote received from a voting site.
 *
 * <p>Votely publishes votes over Redis as a pipe-separated string:
 * <pre>{@code id|serviceName|username|address|timestamp}</pre>
 * Use {@link #deserialize(String)} to parse the raw message and {@link #serialize()} to recreate it.
 *
 * <p>Example — listening for votes in a Minecraft plugin:
 * <pre>{@code
 * redis.subscribe(VotelyChannel.VOTES, message -> {
 *     Vote vote = Vote.deserialize(message);
 *     rewardPlayer(vote.getUsername());
 * });
 * }</pre>
 *
 * @param id          Unique ID assigned by Votely when the vote is received.
 * @param serviceName The voting site that sent this vote (e.g. {@code "MinecraftServers.org"}).
 * @param username    The Minecraft username of the player who voted.
 * @param address     The IP address of the voting site server.
 * @param timestamp   The timestamp provided by the voting site.
 */
public record Vote(String id, String serviceName, String username, String address, String timestamp) {
    /**
     * Deserializes a vote from the raw Redis message published by Votely.
     *
     * @param message pipe-separated string in the format {@code id|serviceName|username|address|timestamp}
     * @return the parsed {@link Vote}
     */
    public static Vote deserialize(String message) {
        String[] parts = message.split("\\|", 5);
        return new Vote(parts[0], parts[1], parts[2], parts[3], parts[4]);
    }

    /**
     * Serializes this vote to a pipe-separated string suitable for Redis pub/sub.
     *
     * @return {@code id|serviceName|username|address|timestamp}
     */
    public String serialize() {
        return String.join("|", id, serviceName, username, address, timestamp);
    }
}