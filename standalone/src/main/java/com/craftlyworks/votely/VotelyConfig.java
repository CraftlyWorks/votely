package com.craftlyworks.votely;

import com.craftlyworks.configra.config.ConfigKey;
import com.craftlyworks.configra.config.ConfigRegistry;

public class VotelyConfig {
    public static final int DEFAULT_PORT = 8192;
    public static final String DEFAULT_KEY_DIR = "./rsa";

    public static final ConfigRegistry CONFIG = new ConfigRegistry();
    public static final ConfigKey<Integer> VOTIFIER_PORT = CONFIG.add("votifier.port", DEFAULT_PORT, Integer.class);
    public static final ConfigKey<String> KEY_DIR = CONFIG.add("votifier.key-dir", DEFAULT_KEY_DIR, String.class);
    public static final ConfigKey<String> VOTE_CHANNEL = CONFIG.add("votes.channel", VotelyChannel.VOTES, String.class);
}