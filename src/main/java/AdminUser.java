import static com.automart.AutoMartApplication.*;

final class AdminUser extends AppUser {
    AdminUser(String username, String email, String phone, String password) { super(username, email, phone, password, "admin"); }
    @Override String roleMessage() { return "Admin profile: monitor users, listings, requests, and reviews from the dashboard."; }
}
