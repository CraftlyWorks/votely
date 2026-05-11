package com.craftlyworks.votely;

import com.craftlyworks.configra.config.IConfigSource;
import com.craftlyworks.configra.redis.Redis;
import com.craftlyworks.configra.redis.RedisConfig;
import com.craftlyworks.configra.util.YamlUtil;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;
import java.util.logging.Logger;

public class VotelyApplication {
    private static final Logger LOG = Logger.getLogger("Votely");

    public static void main(String[] args) throws Exception {
        LoggingHandler.init();
        File configFile = new File("config.yml");
        writeDefaultConfig(configFile);
        IConfigSource config = YamlUtil.load(configFile);
        RedisConfig.load(config);
        String channel = VotelyConfig.CONFIG.get(config, VotelyConfig.VOTE_CHANNEL);
        int port = VotelyConfig.CONFIG.get(config, VotelyConfig.VOTIFIER_PORT);
        String keyDir = VotelyConfig.CONFIG.get(config, VotelyConfig.KEY_DIR);
        String configuredToken = VotelyConfig.CONFIG.get(config, VotelyConfig.VOTIFIER_TOKEN);
        File rsaDir = new File(keyDir != null ? keyDir : VotelyConfig.DEFAULT_KEY_DIR);
        KeyPair keyPair = loadOrGenerateKeys(rsaDir);
        String token = loadOrGenerateToken(rsaDir, configuredToken != null ? configuredToken : "");
        VotifierServer server = new VotifierServer("0.0.0.0", port, keyPair.getPrivate(), token, vote -> {
            Vote identified = new Vote(UUID.randomUUID().toString(), vote.serviceName(), vote.username(), vote.address(), vote.timestamp());
            Redis.INSTANCE.publishGlobal(channel, identified.serialize());
            LOG.info("Vote received from " + identified.username()
                + " via " + identified.serviceName()
                + " (address=" + identified.address()
                + ", timestamp=" + identified.timestamp()
                + ", id=" + identified.id() + ")");
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            Redis.INSTANCE.unload();
            LOG.info("Votely stopped.");
        }, "Votely-Shutdown"));
        server.start();
        LOG.info("Votely started on port " + port + " - publishing to channel '" + channel + "'.");
        LOG.info("v2 token: " + token);
    }

    private static KeyPair loadOrGenerateKeys(File rsaDir) throws Exception {
        File publicKeyFile = new File(rsaDir, "public.key");
        File privateKeyFile = new File(rsaDir, "private.key");
        if (!rsaDir.exists() && !rsaDir.mkdirs()) {
            throw new Exception("Could not create RSA directory: " + rsaDir.getAbsolutePath());
        }
        if (!publicKeyFile.exists() || !privateKeyFile.exists()) {
            KeyPair keyPair = Crypto.generateKeyPair();
            Files.writeString(publicKeyFile.toPath(), Crypto.keyToString(keyPair.getPublic()));
            Files.writeString(privateKeyFile.toPath(), Crypto.keyToString(keyPair.getPrivate()));
            LOG.info("Generated new RSA key pair in " + rsaDir.getAbsolutePath());
            return keyPair;
        }
        PublicKey publicKey = Crypto.loadPublicKey(Files.readString(publicKeyFile.toPath()).trim());
        PrivateKey privateKey = Crypto.loadPrivateKey(Files.readString(privateKeyFile.toPath()).trim());
        LOG.info("Loaded RSA key pair from " + rsaDir.getAbsolutePath());
        return new KeyPair(publicKey, privateKey);
    }

    private static String loadOrGenerateToken(File rsaDir, String configuredToken) throws Exception {
        if (!configuredToken.isEmpty()) return configuredToken;
        File tokenFile = new File(rsaDir, "token.txt");
        if (tokenFile.exists()) {
            return Files.readString(tokenFile.toPath()).trim();
        }
        String token = Crypto.generateToken();
        Files.writeString(tokenFile.toPath(), token);
        LOG.info("Generated new v2 token in " + tokenFile.getAbsolutePath());
        return token;
    }

    private static void writeDefaultConfig(File file) throws Exception {
        if (file.exists()) return;
        try (InputStream in = VotelyApplication.class.getResourceAsStream("/config.yml")) {
            if (in == null) throw new IllegalStateException("Default config.yml not found in jar");
            Files.copy(in, file.toPath());
        }
        LOG.info("Created default config.yml - please review before starting.");
    }
}