package dev.nexoplus.resourcepack;

import com.sun.net.httpserver.HttpServer;
import dev.nexoplus.core.NexoPlus;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;

/**
 * Built-in HTTP server for hosting the resource pack.
 * Eliminates need for external hosting (Dropbox, MC-packs, etc.)
 */
public class PackHostServer {
    private final NexoPlus plugin;
    private final int port;
    private HttpServer server;

    public PackHostServer(NexoPlus plugin, int port) {
        this.plugin = plugin;
        this.port = port;
    }

    public void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> {
            File packZip = plugin.getResourcePackManager().getPackZip();
            if (packZip == null || !packZip.exists()) {
                byte[] msg = "Pack not generated yet.".getBytes();
                exchange.sendResponseHeaders(404, msg.length);
                exchange.getResponseBody().write(msg);
                exchange.close();
                return;
            }
            byte[] data = Files.readAllBytes(packZip.toPath());
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.getResponseHeaders().set("Content-Disposition",
                    "attachment; filename=\"NexoPlus_ResourcePack.zip\"");
            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);
            exchange.close();
        });
        server.setExecutor(null);
        server.start();
    }

    public void stop() {
        if (server != null) server.stop(0);
    }
}
