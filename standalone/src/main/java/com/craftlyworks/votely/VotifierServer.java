package com.craftlyworks.votely;

import lombok.Getter;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class VotifierServer {
    private static final Logger LOG = Logger.getLogger(VotifierServer.class.getName());
    private static final String BANNER = "VOTIFIER 1.0\n";
    private static final int BLOCK_SIZE = 256;

    private final String host;
    private final int port;
    private final PrivateKey privateKey;
    private final Consumer<Vote> voteConsumer;

    private ServerSocket serverSocket;
    private Thread serverThread;
    @Getter
    private volatile boolean running;

    public VotifierServer(String host, int port, PrivateKey privateKey, Consumer<Vote> voteConsumer) {
        this.host = host;
        this.port = port;
        this.privateKey = privateKey;
        this.voteConsumer = voteConsumer;
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

            out.write(BANNER.getBytes(StandardCharsets.US_ASCII));
            out.flush();

            byte[] block = new byte[BLOCK_SIZE];
            int totalRead = 0;
            while (totalRead < BLOCK_SIZE) {
                int n = in.read(block, totalRead, BLOCK_SIZE - totalRead);
                if (n == -1) break;
                totalRead += n;
            }

            if (totalRead < BLOCK_SIZE) {
                LOG.warning("[VotifierServer] Short read (" + totalRead + " bytes) from " + socket.getInetAddress());
                return;
            }

            byte[] decrypted = Crypto.decrypt(block, privateKey);
            String payload = new String(decrypted, StandardCharsets.UTF_8);

            // Payload format: "VOTE\nserviceName\nusername\naddress\ntimestamp\n" (null-padded to 256 bytes)
            String[] parts = payload.split("\n");
            if (parts.length < 5 || !"VOTE".equals(parts[0])) {
                LOG.warning("[VotifierServer] Invalid vote payload from " + socket.getInetAddress());
                return;
            }

            Vote vote = new Vote("", parts[1], parts[2], parts[3], parts[4]);
            LOG.info("[VotifierServer] Vote received: " + vote.username() + " via " + vote.serviceName());
            voteConsumer.accept(vote);
        } catch (Exception e) {
            LOG.warning("[VotifierServer] Failed to handle vote connection: " + e.getMessage());
        }
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