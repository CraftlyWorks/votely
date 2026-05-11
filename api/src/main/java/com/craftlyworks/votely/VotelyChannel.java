package com.craftlyworks.votely;

/**
 * Redis channel names used by Votely.
 *
 * <p>Subscribe to these channels to receive events published by the standalone server.
 */
public final class VotelyChannel {

    /**
     * Channel on which incoming votes are published.
     *
     * <p>Each message is a pipe-separated string:
     * <pre>{@code voteId|serviceName|username|address|timestamp}</pre>
     */
    public static final String VOTES = "votely:votes:incoming";

    private VotelyChannel() {
    }
}