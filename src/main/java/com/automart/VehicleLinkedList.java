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

final class VehicleLinkedList implements ISortable {
    private VehicleNode head;
    public void insert(Vehicle vehicle) {
        VehicleNode node = new VehicleNode(vehicle);
        if (head == null) { head = node; return; }
        VehicleNode current = head;
        while (current.next != null) current = current.next;
        current.next = node;
    }
    public boolean delete(String id) {
        if (head == null) return false;
        if (head.data.id.equals(id)) { head = head.next; return true; }
        VehicleNode current = head;
        while (current.next != null) {
            if (current.next.data.id.equals(id)) { current.next = current.next.next; return true; }
            current = current.next;
        }
        return false;
    }
    public List<Vehicle> toList() {
        List<Vehicle> vehicles = new ArrayList<>();
        VehicleNode current = head;
        while (current != null) { vehicles.add(current.data); current = current.next; }
        return vehicles;
    }
    public List<Vehicle> mergeSortByPrice(boolean descending) {
        head = mergeSort(head, descending);
        List<Vehicle> sorted = new ArrayList<>();
        VehicleNode current = head;
        while (current != null) { sorted.add(current.data); current = current.next; }
        return sorted;
    }
    private VehicleNode mergeSort(VehicleNode node, boolean descending) {
        if (node == null || node.next == null) return node;
        VehicleNode middle = split(node);
        VehicleNode left = mergeSort(node, descending);
        VehicleNode right = mergeSort(middle, descending);
        return merge(left, right, descending);
    }
    private VehicleNode split(VehicleNode head) {
        VehicleNode slow = head, fast = head.next;
        while (fast != null && fast.next != null) { slow = slow.next; fast = fast.next.next; }
        VehicleNode second = slow.next;
        slow.next = null;
        return second;
    }
    private VehicleNode merge(VehicleNode a, VehicleNode b, boolean descending) {
        if (a == null) return b;
        if (b == null) return a;
        boolean takeA = descending ? parsePriceValue(a.data.price) >= parsePriceValue(b.data.price) : parsePriceValue(a.data.price) <= parsePriceValue(b.data.price);
        if (takeA) { a.next = merge(a.next, b, descending); return a; }
        b.next = merge(a, b.next, descending); return b;
    }
}
