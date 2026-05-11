package com.craftlyworks.votely;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class VotifierServer {
    private static final Logger LOG = Logger.getLogger(VotifierServer.class.getName());
    private static final int V1_BLOCK_SIZE = 256;
    private static final int V2_MAGIC_0 = 0x73;
    private static final int V2_MAGIC_1 = 0x3A;

    private final String host;
    private final int port;
    private final PrivateKey privateKey;
    private final String token;
    private final Consumer<Vote> voteConsumer;

    private ServerSocket serverSocket;
    private Thread serverThread;
    @Getter
    private volatile boolean running;

    public VotifierServer(String host, int port, PrivateKey privateKey, String token, Consumer<Vote> voteConsumer) {
        this.host = host;
        this.port = port;
        this.privateKey = privateKey;
        this.token = token;
        this.voteConsumer = voteConsumer;
    }

    private static boolean readFully(InputStream in, byte[] buf, int offset, int length) throws Exception {
        int total = 0;
        while (total < length) {
            int n = in.read(buf, offset + total, length - total);
            if (n == -1) return false;
            total += n;
        }
        return true;
    }

    private static String generateChallenge() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder(32);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

    public void start() throws Exception {
        serverSocket = new ServerSocket(port, 5, InetAddress.getByName(host));
        running = true;
        serverThread = new Thread(this::acceptLoop, "Votifier-Acceptor");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(5000);
                Thread handler = new Thread(() -> handleConnection(socket), "Votifier-Handler");
                handler.setDaemon(true);
                handler.start();
            } catch (Exception e) {
                if (running) {
                    LOG.warning("[VotifierServer] Error accepting connection: " + e.getMessage());
                }
            }
        }
    }

    private void handleConnection(Socket socket) {
        try (socket) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Send banner with per-connection challenge for v2; v1 clients ignore the challenge portion
            String challenge = generateChallenge();
            out.write(("VOTIFIER 2.0 " + challenge + "\n").getBytes(StandardCharsets.US_ASCII));
            out.flush();

            // First 2 bytes determine the protocol:
            //   v2 clients send magic 0x733A first
            //   v1 clients send the start of their 256-byte RSA block
            byte[] header = new byte[2];
            if (!readFully(in, header, 0, 2)) {
                LOG.warning("[VotifierServer] No data received from " + socket.getInetAddress());
                return;
            }

            if ((header[0] & 0xFF) == V2_MAGIC_0 && (header[1] & 0xFF) == V2_MAGIC_1) {
                handleV2(in, challenge, socket.getInetAddress().toString());
            } else {
                handleV1(in, header, socket.getInetAddress().toString());
            }
        } catch (Exception e) {
            LOG.warning("[VotifierServer] Failed to handle vote connection: " + e.getMessage());
        }
    }

    private void handleV1(InputStream in, byte[] headerBytes, String remoteAddr) throws Exception {
        byte[] block = new byte[V1_BLOCK_SIZE];
        block[0] = headerBytes[0];
        block[1] = headerBytes[1];
        if (!readFully(in, block, 2, V1_BLOCK_SIZE - 2)) {
            LOG.warning("[VotifierServer] Short read (v1) from " + remoteAddr);
            return;
        }

        byte[] decrypted = Crypto.decrypt(block, privateKey);
        String payload = new String(decrypted, StandardCharsets.UTF_8);

        String[] parts = payload.split("\n");
        if (parts.length < 5 || !"VOTE".equals(parts[0])) {
            LOG.warning("[VotifierServer] Invalid v1 vote payload from " + remoteAddr);
            return;
        }

        Vote vote = new Vote("", parts[1], parts[2], parts[3], parts[4]);
        voteConsumer.accept(vote);
    }

    private void handleV2(InputStream in, String challenge, String remoteAddr) throws Exception {
        // Read 2-byte big-endian message length
        byte[] lenBytes = new byte[2];
        if (!readFully(in, lenBytes, 0, 2)) {
            LOG.warning("[VotifierServer] Short read (v2 length) from " + remoteAddr);
            return;
        }
        int msgLen = ((lenBytes[0] & 0xFF) << 8) | (lenBytes[1] & 0xFF);
        if (msgLen <= 0) {
            LOG.warning("[VotifierServer] Invalid v2 message length from " + remoteAddr);
            return;
        }

        byte[] msgBytes = new byte[msgLen];
        if (!readFully(in, msgBytes, 0, msgLen)) {
            LOG.warning("[VotifierServer] Short read (v2 message) from " + remoteAddr);
            return;
        }

        // Outer JSON: {"payload": "<json-string>", "signature": "<base64-hmac>"}
        JsonObject outer = JsonParser.parseString(new String(msgBytes, StandardCharsets.UTF_8)).getAsJsonObject();
        String payloadStr = outer.get("payload").getAsString();
        String signature = outer.get("signature").getAsString();

        if (!Crypto.verifyHmacSha256(token, payloadStr, signature)) {
            LOG.warning("[VotifierServer] Invalid v2 HMAC signature from " + remoteAddr);
            return;
        }

        // Inner JSON: {"serviceName":..., "username":..., "address":..., "timestamp":..., "challenge":...}
        JsonObject payload = JsonParser.parseString(payloadStr).getAsJsonObject();
        String payloadChallenge = payload.has("challenge") ? payload.get("challenge").getAsString() : "";
        if (!challenge.equals(payloadChallenge)) {
            LOG.warning("[VotifierServer] Challenge mismatch in v2 vote from " + remoteAddr);
            return;
        }

        String serviceName = payload.get("serviceName").getAsString();
        String username = payload.get("username").getAsString();
        String address = payload.get("address").getAsString();
        String timestamp = String.valueOf(payload.get("timestamp").getAsLong());

        Vote vote = new Vote("", serviceName, username, address, timestamp);
        voteConsumer.accept(vote);
    }

    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }
}