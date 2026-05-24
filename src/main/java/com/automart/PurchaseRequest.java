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

final class PurchaseRequest {
    private static int counter = 1000;
    final String id;
    final String vehicleId;
    final String vehicleTitle;
    final String buyerUsername;
    final String note;
    String status;
    final String createdAt;
    PurchaseRequest(String id, String vehicleId, String vehicleTitle, String buyerUsername, String note, String status, String createdAt) {
        this.id=id; this.vehicleId=vehicleId; this.vehicleTitle=vehicleTitle; this.buyerUsername=buyerUsername; this.note=note; this.status=status; this.createdAt=createdAt;
    }
    static PurchaseRequest create(String vehicleId, String vehicleTitle, String buyerUsername, String note) {
        return new PurchaseRequest("REQ-" + (counter++), vehicleId, vehicleTitle, buyerUsername, note, "Pending", LocalDateTime.now().format(STORED));
    }
    String toRecord() { return String.join("\t", id, vehicleId, Vehicle.clean(vehicleTitle), Vehicle.clean(buyerUsername), Vehicle.clean(note), status, createdAt); }
    static PurchaseRequest fromRecord(String line) { String[] p=line.split("\t",-1); return p.length<7?null:new PurchaseRequest(p[0],p[1],p[2],p[3],p[4],p[5],p[6]); }
}
