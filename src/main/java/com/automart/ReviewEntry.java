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

class ReviewEntry {
    final String id;
    final String vehicleId;
    final String vehicleTitle;
    final String authorUsername;
    String comment;
    final String rating;
    final String createdAt;
    final String type;
    ReviewEntry(String id, String vehicleId, String vehicleTitle, String authorUsername, String comment, String rating, String createdAt, String type) {
        this.id=id; this.vehicleId=vehicleId; this.vehicleTitle=vehicleTitle; this.authorUsername=authorUsername; this.comment=comment; this.rating=rating; this.createdAt=createdAt; this.type=type;
    }
    static ReviewEntry create(String type, String id, String vehicleId, String vehicleTitle, String authorUsername, String comment, String rating, String createdAt) {
        return "VerifiedReview".equalsIgnoreCase(type) ? new VerifiedReview(id, vehicleId, vehicleTitle, authorUsername, comment, rating, createdAt) : new PublicReview(id, vehicleId, vehicleTitle, authorUsername, comment, rating, createdAt);
    }
    String toRecord() { return String.join("	", type, id, vehicleId, Vehicle.clean(vehicleTitle), Vehicle.clean(authorUsername), Vehicle.clean(comment), rating, createdAt); }
    static ReviewEntry fromRecord(String line) { String[] p=line.split("\t",-1); return p.length<8?null:create(p[0],p[1],p[2],p[3],p[4],p[5],p[6],p[7]); }
    @Override public String toString() {
        return renderReviewCard("Standard review", "Community feedback", "review-card-standard");
    }

    String renderReviewCard(String badge, String caption, String extraClass) {
        return "<article class='panel review-card-pro " + extraClass + "'><div class='review-card-top'><div><span class='review-type-pill'>" + esc(badge) + "</span><h3>" + esc(vehicleTitle) + "</h3><p class='review-meta'><strong>" + esc(authorUsername) + "</strong><span>•</span><span>" + esc(caption) + "</span><span>•</span><span>" + esc(formatTime(createdAt)) + "</span></p></div><div class='review-rating-badge'>" + starIcons(parseIntSafe(rating, 0)) + "<strong>" + esc(rating) + ".0</strong></div></div><p class='review-comment-full'>" + esc(comment) + "</p><div class='review-detail-chips'><span>Vehicle ID: " + esc(vehicleId) + "</span><span>Review ID: " + esc(id) + "</span><span>Type: " + esc(type) + "</span></div><form method='post' action='/reviews/action' class='review-inline-form'>" + hiddenField("id", id) + "<input name='comment' value='" + esc(comment) + "' placeholder='Update your review comment'><button class='btn btn-secondary' type='submit' name='action' value='update'>Update</button><button class='btn btn-danger' type='submit' name='action' value='delete' data-delete-button data-delete-message=\"Are you sure you want to delete this feedback permanently?\">Delete</button></form></article>";
    }
}
