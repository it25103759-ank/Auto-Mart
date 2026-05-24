package com.automart;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.concurrent.Executors;


import static com.automart.AutoMartApplication.*;

final class FileHelper {
    static List<Vehicle> readVehicles() throws IOException {
        List<Vehicle> out = new ArrayList<>();
        if (!Files.exists(SUBMISSIONS_FILE)) return out;
        for (String line : Files.readAllLines(SUBMISSIONS_FILE, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            Vehicle v = Vehicle.fromTsv(line);
            if (v != null) out.add(v);
        }
        return out;
    }
    static void writeVehicles(List<Vehicle> vehicles) throws IOException {
        StringBuilder out = new StringBuilder();
        for (Vehicle v : vehicles) out.append(v.toTsv()).append(System.lineSeparator());
        Files.writeString(SUBMISSIONS_FILE, out.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
