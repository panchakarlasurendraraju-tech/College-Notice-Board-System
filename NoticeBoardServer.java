
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

// ============================================
// RCE COLLEGE NOTICE BOARD WEBSITE
// Complete Java Web Application
// ============================================

public class NoticeBoardServer {

    static final int PORT = 8080;

    static final String ADMIN_USER = "admin";
    static final String ADMIN_PASS = "admin123";

    static final String STUDENT_ROLL = "23A91A0501";
    static final String STUDENT_PASS = "student123";

    static final String NOTICE_FILE = "notices.dat";
    static final String DOUBT_FILE = "doubts.dat";

    static ArrayList<Notice> notices = new ArrayList<>();
    static ArrayList<Doubt> doubts = new ArrayList<>();

    static HashMap<String, String> sessions = new HashMap<>();

    static final DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // ============================================
    // MODULE 1: DATA MODELS
    // ============================================

    static class Notice implements Serializable {
        private static final long serialVersionUID = 1L;

        int id;
        String title, category, description, date, author;
        boolean pinned, published;

        Notice(int id, String title, String category,
               String description, String author) {

            this.id = id;
            this.title = title;
            this.category = category;
            this.description = description;
            this.author = author;
            this.date = LocalDateTime.now().format(formatter);
            this.pinned = false;
            this.published = true;
        }
    }

    static class Doubt implements Serializable {
        private static final long serialVersionUID = 1L;

        int id;
        String rollNumber, question, date;
        boolean answered;

        Doubt(int id, String rollNumber, String question) {
            this.id = id;
            this.rollNumber = rollNumber;
            this.question = question;
            this.date = LocalDateTime.now().format(formatter);
            this.answered = false;
        }
    }

    // ============================================
    // MODULE 2: FILE STORAGE
    // ============================================

    static void saveData(String file, Object data) {
        try (ObjectOutputStream out =
                 new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(data);
        } catch (IOException e) {
            System.out.println("Error saving " + file);
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    static void loadData() {

        File nf = new File(NOTICE_FILE);
        File df = new File(DOUBT_FILE);

        if (nf.exists()) {
            try (ObjectInputStream in =
                     new ObjectInputStream(new FileInputStream(nf))) {
                notices = (ArrayList<Notice>) in.readObject();
            } catch (Exception e) {
                System.out.println("Could not load notices.");
            }
        }

        if (df.exists()) {
            try (ObjectInputStream in =
                     new ObjectInputStream(new FileInputStream(df))) {
                doubts = (ArrayList<Doubt>) in.readObject();
            } catch (Exception e) {
                System.out.println("Could not load doubts.");
            }
        }
    }

    static int getNextNoticeId() {
        return notices.stream().mapToInt(n -> n.id).max().orElse(0) + 1;
    }

    static int getNextDoubtId() {
        return doubts.stream().mapToInt(d -> d.id).max().orElse(0) + 1;
    }

    static Notice findNotice(int id) {
        for (Notice n : notices) {
            if (n.id == id) return n;
        }
        return null;
    }

    // ============================================
    // MODULE 3: HTML DESIGN
    // ============================================

    static String page(String title, String content) {

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>{{TITLE}} | RCE Notice Board</title>

        <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: Arial, sans-serif;
            background: #080d1b;
            color: #f8fafc;
            min-height: 100vh;
        }

        header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 20px 7%;
            background: #111827;
            border-bottom: 1px solid #263449;
        }

        .logo {
            font-size: 22px;
            font-weight: bold;
            color: #38bdf8;
        }

        nav a {
            color: #cbd5e1;
            text-decoration: none;
            margin-left: 20px;
            font-size: 14px;
        }

        nav a:hover {
            color: #38bdf8;
        }

        .container {
            width: 90%;
            max-width: 1100px;
            margin: 45px auto;
        }

        .hero {
            padding: 65px 35px;
            text-align: center;
            background: linear-gradient(135deg,
                #172554, #312e81, #0f172a);
            border: 1px solid #334155;
            border-radius: 22px;
            box-shadow: 0 15px 50px rgba(0,0,0,.4);
        }

        .hero h1 {
            font-size: 42px;
            margin-bottom: 18px;
        }

        .hero p {
            color: #cbd5e1;
            line-height: 1.7;
            margin-bottom: 25px;
        }

        .blue {
            color: #38bdf8;
        }

        .btn {
            display: inline-block;
            padding: 13px 24px;
            border-radius: 10px;
            text-decoration: none;
            font-weight: bold;
            margin: 7px;
            background: #2563eb;
            color: white;
            border: none;
            cursor: pointer;
        }

        .btn:hover {
            background: #0284c7;
            transform: translateY(-2px);
        }

        .btn-green {
            background: #059669;
        }

        .btn-red {
            background: #dc2626;
        }

        .section-title {
            margin: 35px 0 20px;
            font-size: 25px;
        }

        .grid {
            display: grid;
            grid-template-columns:
                repeat(auto-fit, minmax(220px, 1fr));
            gap: 20px;
        }

        .card {
            background: #111827;
            padding: 25px;
            border-radius: 16px;
            border: 1px solid #263449;
            transition: .3s;
        }

        .card:hover {
            transform: translateY(-5px);
            border-color: #38bdf8;
        }

        .card h3 {
            color: #38bdf8;
            margin-bottom: 12px;
        }

        .card p {
            color: #94a3b8;
            line-height: 1.6;
        }

        .form-box {
            max-width: 480px;
            margin: 40px auto;
            background: #111827;
            border: 1px solid #334155;
            border-radius: 18px;
            padding: 30px;
        }

        input, textarea, select {
            width: 100%;
            padding: 13px;
            margin: 10px 0 18px;
            border: 1px solid #334155;
            border-radius: 9px;
            background: #080d1b;
            color: white;
            font-size: 15px;
        }

        label {
            color: #cbd5e1;
        }

        textarea {
            min-height: 110px;
            resize: vertical;
        }

        .notice {
            background: #111827;
            border: 1px solid #334155;
            border-left: 4px solid #38bdf8;
            border-radius: 12px;
            padding: 20px;
            margin: 15px 0;
            overflow-wrap: anywhere;
        }

        .notice h3 {
            color: #38bdf8;
            margin-bottom: 10px;
        }

        .muted {
            color: #94a3b8;
            font-size: 13px;
            margin-bottom: 10px;
        }

        .message {
            padding: 12px;
            border-radius: 8px;
            margin-bottom: 15px;
            background: #172554;
            color: #bfdbfe;
        }

        footer {
            text-align: center;
            padding: 25px;
            margin-top: 60px;
            color: #64748b;
            border-top: 1px solid #263449;
        }

        @media(max-width:600px) {
            header {
                flex-direction: column;
                gap: 15px;
            }

            nav a {
                margin: 0 7px;
            }

            .hero {
                padding: 40px 18px;
            }

            .hero h1 {
                font-size: 29px;
            }

            .container {
                width: 94%;
            }
        }
        </style>
        </head>

        <body>
        <header>
            <div class="logo">RCE NOTICE BOARD</div>
            <nav>
                <a href="/">Home</a>
                <a href="/student-login">Student</a>
                <a href="/admin-login">Admin</a>
            </nav>
        </header>

        <main class="container">
            {{CONTENT}}
        </main>

        <footer>
            RCE College Notice Board |
            Smart Campus Communication System
        </footer>
        </body>
        </html>
        """.replace("{{TITLE}}", escape(title))
           .replace("{{CONTENT}}", content);
    }

    static String escape(String value) {
        if (value == null) return "";

        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }

    static String homeContent() {
        return """
        <section class="hero">
            <h1>Welcome to <span class="blue">RCE Notice Board</span></h1>
            <p>
                One intelligent platform for college announcements,
                academic updates, events and student communication.
            </p>
            <a href="/student-login" class="btn">Student Login</a>
            <a href="/admin-login" class="btn btn-green">Admin Login</a>
        </section>

        <h2 class="section-title">Platform Features</h2>

        <section class="grid">
            <div class="card">
                <h3>College Notices</h3>
                <p>Access important college announcements.</p>
            </div>
            <div class="card">
                <h3>Student Portal</h3>
                <p>Students can log in and view published notices.</p>
            </div>
            <div class="card">
                <h3>Admin Dashboard</h3>
                <p>Admin can create and manage announcements.</p>
            </div>
            <div class="card">
                <h3>Doubt Support</h3>
                <p>Students can submit questions to the admin.</p>
            </div>
        </section>
        """;
    }

    // ============================================
    // MODULE 4: RESPONSE AND FORM UTILITIES
    // ============================================

    static void sendResponse(HttpExchange ex, int status, String html)
            throws IOException {

        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);

        ex.getResponseHeaders().set(
            "Content-Type", "text/html; charset=UTF-8"
        );

        ex.getResponseHeaders().set(
            "X-Content-Type-Options", "nosniff"
        );

        ex.sendResponseHeaders(status, bytes.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    static void redirect(HttpExchange ex, String location)
            throws IOException {

        ex.getResponseHeaders().set("Location", location);
        ex.sendResponseHeaders(303, -1);
        ex.close();
    }

    static Map<String, String> parseForm(HttpExchange ex)
            throws IOException {

        String body = new String(
            ex.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        );

        Map<String, String> data = new HashMap<>();

        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);

            String key = URLDecoder.decode(
                parts[0], StandardCharsets.UTF_8
            );

            String value = parts.length > 1
                ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                : "";

            data.put(key, value);
        }

        return data;
    }

    static String getQueryValue(HttpExchange ex, String parameter) {
        String query = ex.getRequestURI().getRawQuery();

        if (query == null) return "";

        for (String part : query.split("&")) {
            String[] values = part.split("=", 2);

            if (values.length == 2 && values[0].equals(parameter)) {
                return URLDecoder.decode(
                    values[1], StandardCharsets.UTF_8
                );
            }
        }

        return "";
    }

    static int getId(HttpExchange ex) {
        try {
            return Integer.parseInt(getQueryValue(ex, "id"));
        } catch (Exception e) {
            return -1;
        }
    }

    static int formId(Map<String, String> form) {
        try {
            return Integer.parseInt(form.getOrDefault("id", "-1"));
        } catch (Exception e) {
            return -1;
        }
    }

    // ============================================
    // MODULE 5: LOGIN SYSTEM
    // ============================================

    static String loginContent(String role, String error) {

        String heading = role.equals("admin")
            ? "Administrator Login" : "Student Login";

        String label = role.equals("admin")
            ? "Username" : "Roll Number";

        String placeholder = role.equals("admin")
            ? "Enter admin username" : "Enter student roll number";

        StringBuilder html = new StringBuilder();

        html.append("<div class='form-box'>");
        html.append("<h2 style='text-align:center;color:#38bdf8;margin-bottom:20px;'>")
            .append(heading).append("</h2>");

        if (error != null && !error.isEmpty()) {
            html.append("<div class='message'>")
                .append(escape(error)).append("</div>");
        }

        html.append("<form action='/login' method='POST'>");

        html.append("<input type='hidden' name='role' value='")
            .append(escape(role)).append("'>");

        html.append("<label>").append(label).append("</label>");

        html.append("<input type='text' name='username' placeholder='")
            .append(escape(placeholder)).append("' required>");

        html.append("<label>Password</label>");

        html.append("<input type='password' name='password' ")
            .append("placeholder='Enter your password' required>");

        html.append("<button type='submit' class='btn' style='width:100%'>")
            .append("Login</button></form>");

        html.append("<p style='text-align:center;margin-top:20px'>")
            .append("<a href='/' style='color:#38bdf8'>Back to Home</a>")
            .append("</p></div>");

        return html.toString();
    }

    static String getSessionRole(HttpExchange ex) {

        List<String> cookies = ex.getRequestHeaders().get("Cookie");

        if (cookies == null) return null;

        for (String header : cookies) {
            for (String cookie : header.split(";")) {

                String[] parts = cookie.trim().split("=", 2);

                if (parts.length == 2 &&
                    parts[0].equals("RCESESSION")) {
                    return sessions.get(parts[1]);
                }
            }
        }

        return null;
    }

    static void handleLogin(HttpExchange ex) throws IOException {

        Map<String, String> form = parseForm(ex);

        String role = form.getOrDefault("role", "");
        String username = form.getOrDefault("username", "").trim();
        String password = form.getOrDefault("password", "");

        boolean valid = false;

        if (role.equals("admin")) {
            valid = username.equals(ADMIN_USER)
                 && password.equals(ADMIN_PASS);
        } else if (role.equals("student")) {
            valid = username.equals(STUDENT_ROLL)
                 && password.equals(STUDENT_PASS);
        }

        if (!valid) {
            sendResponse(ex, 401, page("Login Failed",
                loginContent(role.equals("admin") ? "admin" : "student",
                "Invalid username, roll number or password!")));
            return;
        }

        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, role);

        ex.getResponseHeaders().add(
            "Set-Cookie",
            "RCESESSION=" + sessionId +
            "; Path=/; HttpOnly; SameSite=Lax"
        );

        redirect(ex, role.equals("admin") ? "/admin" : "/student");
    }

    static void handleLogout(HttpExchange ex) throws IOException {

        List<String> cookies = ex.getRequestHeaders().get("Cookie");

        if (cookies != null) {
            for (String header : cookies) {
                for (String cookie : header.split(";")) {
                    String[] parts = cookie.trim().split("=", 2);

                    if (parts.length == 2 &&
                        parts[0].equals("RCESESSION")) {
                        sessions.remove(parts[1]);
                    }
                }
            }
        }

        ex.getResponseHeaders().add(
            "Set-Cookie",
            "RCESESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax"
        );

        redirect(ex, "/");
    }

    // ============================================
    // MODULE 6: ADMIN DASHBOARD
    // ============================================

    static void handleAdmin(HttpExchange ex) throws IOException {

        if (!"admin".equals(getSessionRole(ex))) {
            redirect(ex, "/admin-login");
            return;
        }

        int published = 0;

        for (Notice n : notices) {
            if (n.published) published++;
        }

        StringBuilder html = new StringBuilder();

        html.append("<h1 class='blue'>Admin Dashboard</h1>");
        html.append("<p class='muted'>Welcome, Administrator!</p>");

        html.append("<div class='grid'>");

        html.append("<div class='card'><h3>Total Notices</h3><h1>")
            .append(notices.size()).append("</h1></div>");

        html.append("<div class='card'><h3>Published</h3><h1>")
            .append(published).append("</h1></div>");

        html.append("<div class='card'><h3>Unpublished</h3><h1>")
            .append(notices.size() - published).append("</h1></div>");

        html.append("</div>");

        html.append("<h2 class='section-title'>Admin Controls</h2>");
        html.append("<div class='grid'>");

        html.append("<div class='card'><h3>Add Notice</h3>")
            .append("<p>Create a college announcement.</p>")
            .append("<a href='/add' class='btn'>Add Notice</a></div>");

        html.append("<div class='card'><h3>Student Doubts</h3>")
            .append("<p>Manage student questions.</p>")
            .append("<a href='/doubts' class='btn btn-green'>View Doubts</a></div>");

        html.append("<div class='card'><h3>Student Portal</h3>")
            .append("<p>View the student portal.</p>")
            .append("<a href='/student' class='btn'>View Portal</a></div>");

        html.append("</div>");
        html.append("<h2 class='section-title'>Manage Notices</h2>");

        if (notices.isEmpty()) {
            html.append("<div class='card'><h3>No Notices Available</h3>")
                .append("<p>Create your first notice.</p></div>");
        }

        for (Notice n : notices) {

            html.append("<div class='notice'>");

            html.append("<h3>").append(escape(n.title)).append("</h3>");

            html.append("<p class='muted'>")
                .append(escape(n.category)).append(" | ")
                .append(escape(n.date)).append("</p>");

            html.append("<p>").append(escape(n.description)).append("</p>");

            html.append("<p class='muted'>Posted by: ")
                .append(escape(n.author)).append("</p>");

            html.append("<p>Status: ")
                .append(n.published ? "Published" : "Unpublished")
                .append(" | ").append(n.pinned ? "Pinned" : "Not Pinned")
                .append("</p>");

            html.append("<a class='btn' href='/edit?id=")
                .append(n.id).append("'>Edit</a>");

            html.append("<form action='/delete' method='POST' style='display:inline'>")
                .append("<input type='hidden' name='id' value='")
                .append(n.id).append("'>")
                .append("<button class='btn btn-red' type='submit'>Delete</button>")
                .append("</form>");

            html.append("<form action='/toggle' method='POST' style='display:inline'>")
                .append("<input type='hidden' name='id' value='")
                .append(n.id).append("'>")
                .append("<button class='btn btn-green' type='submit'>")
                .append(n.published ? "Unpublish" : "Publish")
                .append("</button></form>");

            html.append("</div>");
        }

        html.append("<a href='/logout' class='btn btn-red'>Logout</a>");

        sendResponse(ex, 200, page("Admin Dashboard", html.toString()));
    }

    // ============================================
    // MODULE 7: ADD NOTICE
    // ============================================

    static String addNoticeForm(String error) {

        StringBuilder html = new StringBuilder();

        html.append("<div class='form-box'>");
        html.append("<h2 class='blue'>Create New Notice</h2>");

        if (error != null && !error.isEmpty()) {
            html.append("<div class='message'>")
                .append(escape(error)).append("</div>");
        }

        html.append("""
        <form action="/add" method="POST">
            <label>Notice Title</label>
            <input type="text" name="title" placeholder="Enter title" required>

            <label>Category</label>
            <select name="category" required>
                <option value="">Select Category</option>
                <option>Examination</option>
                <option>Academic</option>
                <option>Events</option>
                <option>Sports</option>
                <option>Placement</option>
                <option>General</option>
            </select>

            <label>Description</label>
            <textarea name="description" placeholder="Write notice details..." required></textarea>

            <label>Publication Status</label>
            <select name="published">
                <option value="yes">Publish Now</option>
                <option value="no">Save as Draft</option>
            </select>

            <label>Pin Notice?</label>
            <select name="pinned">
                <option value="no">No</option>
                <option value="yes">Yes - Important Notice</option>
            </select>

            <button type="submit" class="btn" style="width:100%">Save Notice</button>
        </form>
        <a href="/admin" class="btn">Back to Dashboard</a>
        </div>
        """);

        return html.toString();
    }

    static void handleAdd(HttpExchange ex) throws IOException {

        if (!"admin".equals(getSessionRole(ex))) {
            redirect(ex, "/admin-login");
            return;
        }

        if (ex.getRequestMethod().equalsIgnoreCase("POST")) {

            Map<String, String> form = parseForm(ex);

            String title = form.getOrDefault("title", "").trim();
            String category = form.getOrDefault("category", "").trim();
            String description = form.getOrDefault("description", "").trim();

            if (title.isEmpty() || category.isEmpty() ||
                description.isEmpty()) {
                sendResponse(ex, 400, page("Add Notice",
                    addNoticeForm("Please fill in all fields.")));
                return;
            }

            Notice n = new Notice(getNextNoticeId(), title,
                category, description, "Administrator");

            n.published = form.getOrDefault("published", "yes").equals("yes");
            n.pinned = form.getOrDefault("pinned", "no").equals("yes");

            notices.add(n);
            saveData(NOTICE_FILE, notices);

            redirect(ex, "/admin");
            return;
        }

        sendResponse(ex, 200, page("Add Notice", addNoticeForm("")));
    }

    // ============================================
    // MODULE 8: EDIT NOTICE
    // ============================================

    static void handleEdit(HttpExchange ex) throws IOException {

        if (!"admin".equals(getSessionRole(ex))) {
            redirect(ex, "/admin-login");
            return;
        }

        Notice n = findNotice(getId(ex));

        if (n == null) {
            sendResponse(ex, 404, page("Not Found",
                "<div class='card'><h2>Notice not found.</h2>" +
                "<a href='/admin' class='btn'>Back</a></div>"));
            return;
        }

        StringBuilder html = new StringBuilder();

        html.append("<div class='form-box'><h2 class='blue'>Edit Notice</h2>");
        html.append("<form action='/update' method='POST'>");

        html.append("<input type='hidden' name='id' value='")
            .append(n.id).append("'>");

        html.append("<label>Title</label><input name='title' value='")
            .append(escape(n.title)).append("' required>");

        html.append("<label>Category</label><input name='category' value='")
            .append(escape(n.category)).append("' required>");

        html.append("<label>Description</label><textarea name='description' required>")
            .append(escape(n.description)).append("</textarea>");

        html.append("<label>Publication Status</label><select name='published'>");

        html.append("<option value='yes'")
            .append(n.published ? " selected" : "")
            .append(">Published</option>");

        html.append("<option value='no'")
            .append(!n.published ? " selected" : "")
            .append(">Unpublished</option></select>");

        html.append("<label>Pin Notice?</label><select name='pinned'>");

        html.append("<option value='yes'")
            .append(n.pinned ? " selected" : "")
            .append(">Pinned</option>");

        html.append("<option value='no'")
            .append(!n.pinned ? " selected" : "")
            .append(">Not Pinned</option></select>");

        html.append("<button type='submit' class='btn'>Update Notice</button>");
        html.append("</form><a href='/admin' class='btn'>Cancel</a></div>");

        sendResponse(ex, 200, page("Edit Notice", html.toString()));
    }

    static void handleUpdate(HttpExchange ex) throws IOException {

        if (!"admin".equals(getSessionRole(ex))) {
            redirect(ex, "/admin-login");
            return;
        }

        Map<String, String> form = parseForm(ex);
        Notice n = findNotice(formId(form));

        if (n == null) {
            redirect(ex, "/admin");
            return;
        }

        String title = form.getOrDefault("title", "").trim();
        String category = form.getOrDefault("category", "").trim();
        String description = form.getOrDefault("description", "").trim();

        if (title.isEmpty() || category.isEmpty() || description.isEmpty()) {
            sendResponse(ex, 400, page("Invalid Notice",
                "<div class='card'><h2>Please fill all fields.</h2>" +
                "<a href='/edit?id=" + n.id + "' class='btn'>Go Back</a></div>"));
            return;
        }

        n.title = title;
        n.category = category;
        n.description = description;
        n.published = form.getOrDefault("published", "no").equals("yes");
        n.pinned = form.getOrDefault("pinned", "no").equals("yes");

        saveData(NOTICE_FILE, notices);
        redirect(ex, "/admin");
    }

    // ============================================
    // MODULE 9: DELETE AND PUBLISH
    // ============================================

    static void handleDelete(HttpExchange ex) throws IOException {

        if (!"admin".equals(getSessionRole(ex))) {
            redirect(ex, "/admin-login");
            return;
        }

        int id = formId(parseForm(ex));

        notices.removeIf(n -> n.id == id);
        saveData(NOTICE_FILE, notices);

        redirect(ex, "/admin");
    }

    static void handleToggle(HttpExchange ex) throws IOException {

        if (!"admin".equals(getSessionRole(ex))) {
            redirect(ex, "/admin-login");
            return;
        }

        int id = formId(parseForm(ex));
        Notice n = findNotice(id);

        if (n != null) {
            n.published = !n.published;
            saveData(NOTICE_FILE, notices);
        }

        redirect(ex, "/admin");
    }

    // ============================================
    // MODULE 10: STUDENT DASHBOARD
    // ============================================

    static void handleStudent(HttpExchange ex) throws IOException {

        if (!"student".equals(getSessionRole(ex))) {
            redirect(ex, "/student-login");
            return;
        }

        String search = getQueryValue(ex, "search");
        String category = getQueryValue(ex, "category");

        StringBuilder html = new StringBuilder();

        long count = notices.stream().filter(n -> n.published).count();

        html.append("<h1 class='blue'>Student Dashboard</h1>");
        html.append("<p class='muted'>Welcome! View college announcements.</p>");

        html.append("<div class='grid'>");

        html.append("<div class='card'><h3>Student Information</h3>")
            .append("<p>Roll Number: ").append(STUDENT_ROLL)
            .append("</p><p>Role: Student</p></div>");

        html.append("<div class='card'><h3>Published Notices</h3><h1>")
            .append(count).append("</h1></div></div>");

        html.append("<h2 class='section-title'>Search College Notices</h2>");

        html.append("<div class='card'><form action='/student' method='GET'>");

        html.append("<label>Search Notice</label>");
        html.append("<input name='search' value='")
            .append(escape(search))
            .append("' placeholder='Search title or description'>");

        html.append("<label>Category</label><select name='category'>");

        String[] categories = {
            "Examination", "Academic", "Events",
            "Sports", "Placement", "General"
        };

        html.append("<option value=''>All Categories</option>");

        for (String c : categories) {
            html.append("<option value='").append(c).append("'")
                .append(c.equalsIgnoreCase(category) ? " selected" : "")
                .append(">").append(c).append("</option>");
        }

        html.append("</select>");
        html.append("<button class='btn' type='submit'>Search Notices</button>");
        html.append("<a href='/student' class='btn'>Clear Filters</a>");
        html.append("</form></div>");

        html.append("<h2 class='section-title'>Latest College Notices</h2>");

        ArrayList<Notice> visible = new ArrayList<>();

        for (Notice n : notices) {
            if (n.published) visible.add(n);
        }

        visible.sort((a, b) -> {
            int pinOrder = Boolean.compare(b.pinned, a.pinned);
            return pinOrder != 0 ? pinOrder : b.date.compareTo(a.date);
        });

        int displayed = 0;

        for (Notice n : visible) {

            boolean matchesSearch =
                n.title.toLowerCase().contains(search.toLowerCase()) ||
                n.description.toLowerCase().contains(search.toLowerCase());

            boolean matchesCategory =
                category.isEmpty() || n.category.equalsIgnoreCase(category);

            if (!matchesSearch || !matchesCategory) continue;

            displayed++;

            html.append("<div class='notice'>");

            if (n.pinned) {
                html.append("<p style='color:#fbbf24'>📌 IMPORTANT NOTICE</p>");
            }

            html.append("<h3>").append(escape(n.title)).append("</h3>");

            html.append("<p class='muted'>Category: ")
                .append(escape(n.category)).append(" | Date: ")
                .append(escape(n.date)).append("</p>");

            html.append("<p>").append(escape(n.description)).append("</p>");

            html.append("<p class='muted'>Posted by: ")
                .append(escape(n.author)).append("</p></div>");
        }

        if (displayed == 0) {
            html.append("<div class='card'><h3>No Notices Found</h3>")
                .append("<p>Try another search or category.</p></div>");
        }

        html.append("<h2 class='section-title'>Need Help?</h2>");
        html.append("<div class='card'><h3>Ask Your Doubt</h3>")
            .append("<p>Submit your question to the administrator.</p>")
            .append("<a href='/ask' class='btn btn-green'>Submit a Doubt</a>")
            .append("</div>");

        html.append("<a href='/logout' class='btn btn-red'>Logout</a>");

        sendResponse(ex, 200, page("Student Dashboard", html.toString()));
    }

    // ============================================
    // MODULE 11: STUDENT DOUBT SUBMISSION
    // ============================================

    static String askForm(String error) {

        StringBuilder html = new StringBuilder();

        html.append("<div class='form-box'><h2 class='blue'>Submit Your Doubt</h2>");
        html.append("<p class='muted'>Roll Number: ")
            .append(STUDENT_ROLL).append("</p>");

        if (error != null && !error.isEmpty()) {
            html.append("<div class='message'>")
                .append(escape(error)).append("</div>");
        }

        html.append("""
        <form action="/ask" method="POST">
            <label>Your Question</label>
            <textarea name="question" placeholder="Type your question..." required></textarea>
            <button type="submit" class="btn btn-green">Submit Doubt</button>
        </form>
        <a href="/student" class="btn">Back</a>
        </div>
        """);

        return html.toString();
    }

    static void handleAsk(HttpExchange ex) throws IOException {

        if (!"student".equals(getSessionRole(ex))) {
            redirect(ex, "/student-login");
            return;
        }

        if (ex.getRequestMethod().equalsIgnoreCase("POST")) {

            Map<String, String> form = parseForm(ex);
            String question = form.getOrDefault("question", "").trim();

            if (question.isEmpty()) {
                sendResponse(ex, 400, page("Ask Doubt",
                    askForm("Please enter your question.")));
                return;
            }

            doubts.add(new Doubt(
                getNextDoubtId(), STUDENT_ROLL, question
            ));

            saveData(DOUBT_FILE, doubts);

            sendResponse(ex, 200, page("Doubt Submitted", """
            <div class="card">
                <h2 style="color:#22c55e">Doubt Submitted Successfully!</h2>
                <p>Your question has been sent to the administrator.</p>
                <a href="/student" class="btn">Back to Dashboard</a>
            </div>
            """));
            return;
        }

        sendResponse(ex, 200, page("Ask Doubt", askForm("")));
    }

    // ============================================
    // MODULE 12: ADMIN DOUBT MANAGEMENT
    // ============================================

    static void handleDoubts(HttpExchange ex) throws IOException {

        if (!"admin".equals(getSessionRole(ex))) {
            redirect(ex, "/admin-login");
            return;
        }

        int answered = 0;

        for (Doubt d : doubts) {
            if (d.answered) answered++;
        }

        StringBuilder html = new StringBuilder();

        html.append("<h1 class='blue'>Student Doubts</h1>");
        html.append("<p class='muted'>Manage questions submitted by students.</p>");

        html.append("<div class='grid'>");

        html.append("<div class='card'><h3>Total Doubts</h3><h1>")
            .append(doubts.size()).append("</h1></div>");

        html.append("<div class='card'><h3>Pending</h3><h1>")
            .append(doubts.size() - answered).append("</h1></div>");

        html.append("<div class='card'><h3>Answered</h3><h1>")
            .append(answered).append("</h1></div></div>");

        html.append("<h2 class='section-title'>Submitted Questions</h2>");

        if (doubts.isEmpty()) {
            html.append("<div class='card'><h3>No Doubts Submitted</h3></div>");
        }

        for (Doubt d : doubts) {

            html.append("<div class='notice'>");

            html.append("<h3>Doubt #").append(d.id).append("</h3>");

            html.append("<p class='muted'>Roll Number: ")
                .append(escape(d.rollNumber)).append("</p>");

            html.append("<p>").append(escape(d.question)).append("</p>");

            html.append("<p class='muted'>Submitted: ")
                .append(escape(d.date)).append("</p>");

            html.append("<p style='color:")
                .append(d.answered ? "#22c55e" : "#f59e0b")
                .append("'>")
                .append(d.answered ? "Answered" : "Pending")
                .append("</p>");

            if (!d.answered) {
                html.append("<form action='/answer-doubt' method='POST' style='display:inline'>")
                    .append("<input type='hidden' name='id' value='")
                    .append(d.id).append("'>")
                    .append("<button class='btn btn-green' type='submit'>Mark Answered</button>")
                    .append("</form>");
            }

            html.append("<form action='/delete-doubt' method='POST' style='display:inline'>")
                .append("<input type='hidden' name='id' value='")
                .append(d.id).append("'>")
                .append("<button class='btn btn-red' type='submit'>Delete</button>")
                .append("</form></div>");
        }

        html.append("<a href='/admin' class='btn'>Back to Admin Dashboard</a>");
        html.append("<a href='/logout' class='btn btn-red'>Logout</a>");

        sendResponse(ex, 200, page("Student Doubts", html.toString()));
    }

    static void handleAnswerDoubt(HttpExchange ex) throws IOException {

        if (!"admin".equals(getSessionRole(ex))) {
            redirect(ex, "/admin-login");
            return;
        }

        int id = formId(parseForm(ex));

        for (Doubt d : doubts) {
            if (d.id == id) {
                d.answered = true;
                break;
            }
        }

        saveData(DOUBT_FILE, doubts);
        redirect(ex, "/doubts");
    }

    static void handleDeleteDoubt(HttpExchange ex) throws IOException {

        if (!"admin".equals(getSessionRole(ex))) {
            redirect(ex, "/admin-login");
            return;
        }

        int id = formId(parseForm(ex));

        doubts.removeIf(d -> d.id == id);

        saveData(DOUBT_FILE, doubts);
        redirect(ex, "/doubts");
    }

    // ============================================
    // MODULE 13: ROUTE MANAGEMENT
    // ============================================

    static void requireMethod(HttpExchange ex, String method,
                              IOHandler handler) throws IOException {

        if (!ex.getRequestMethod().equalsIgnoreCase(method)) {
            sendResponse(ex, 405, page("Method Not Allowed",
                "<div class='card'><h2>Method Not Allowed</h2>" +
                "<a href='/' class='btn'>Home</a></div>"));
            return;
        }

        handler.handle(ex);
    }

    interface IOHandler {
        void handle(HttpExchange ex) throws IOException;
    }

    static void registerRoutes(HttpServer server) {

        server.createContext("/", ex -> {

            try {

                String path = ex.getRequestURI().getPath();

                switch (path) {

                    case "/":
                        requireMethod(ex, "GET",
                            NoticeBoardServer::handleHome);
                        break;

                    case "/admin-login":
                        requireMethod(ex, "GET",
                            e -> sendResponse(e, 200,
                                page("Admin Login",
                                    loginContent("admin", ""))));
                        break;

                    case "/student-login":
                        requireMethod(ex, "GET",
                            e -> sendResponse(e, 200,
                                page("Student Login",
                                    loginContent("student", ""))));
                        break;

                    case "/login":
                        requireMethod(ex, "POST",
                            NoticeBoardServer::handleLogin);
                        break;

                    case "/logout":
                        requireMethod(ex, "GET",
                            NoticeBoardServer::handleLogout);
                        break;

                    case "/admin":
                        requireMethod(ex, "GET",
                            NoticeBoardServer::handleAdmin);
                        break;

                    case "/add":
                        if (ex.getRequestMethod().equalsIgnoreCase("GET") ||
                            ex.getRequestMethod().equalsIgnoreCase("POST")) {
                            handleAdd(ex);
                        } else {
                            requireMethod(ex, "GET", e -> {});
                        }
                        break;

                    case "/edit":
                        requireMethod(ex, "GET",
                            NoticeBoardServer::handleEdit);
                        break;

                    case "/update":
                        requireMethod(ex, "POST",
                            NoticeBoardServer::handleUpdate);
                        break;

                    case "/delete":
                        requireMethod(ex, "POST",
                            NoticeBoardServer::handleDelete);
                        break;

                    case "/toggle":
                        requireMethod(ex, "POST",
                            NoticeBoardServer::handleToggle);
                        break;

                    case "/student":
                        requireMethod(ex, "GET",
                            NoticeBoardServer::handleStudent);
                        break;

                    case "/ask":
                        if (ex.getRequestMethod().equalsIgnoreCase("GET") ||
                            ex.getRequestMethod().equalsIgnoreCase("POST")) {
                            handleAsk(ex);
                        } else {
                            requireMethod(ex, "GET", e -> {});
                        }
                        break;

                    case "/doubts":
                        requireMethod(ex, "GET",
                            NoticeBoardServer::handleDoubts);
                        break;

                    case "/answer-doubt":
                        requireMethod(ex, "POST",
                            NoticeBoardServer::handleAnswerDoubt);
                        break;

                    case "/delete-doubt":
                        requireMethod(ex, "POST",
                            NoticeBoardServer::handleDeleteDoubt);
                        break;

                    default:
                        sendResponse(ex, 404, page("Page Not Found",
                            "<div class='card'><h2>404 - Page Not Found</h2>" +
                            "<a href='/' class='btn'>Go Home</a></div>"));
                        break;
                }

            } catch (Exception e) {

                e.printStackTrace();

                try {
                    sendResponse(ex, 500, page("Server Error",
                        "<div class='card'><h2>Something went wrong.</h2>" +
                        "<p>Check the Java console for details.</p>" +
                        "<a href='/' class='btn'>Home</a></div>"));
                } catch (IOException ignored) {
                    ex.close();
                }
            }
        });
    }

    static void handleHome(HttpExchange ex) throws IOException {
        sendResponse(ex, 200,
            page("RCE College Notice Board", homeContent()));
    }

    // ============================================
    // MODULE 14: MAIN METHOD
    // ============================================

    public static void main(String[] args) {

        try {

            loadData();

            HttpServer server = HttpServer.create(
                new InetSocketAddress(PORT), 0
            );

            registerRoutes(server);

            server.start();

            System.out.println("====================================");
            System.out.println(" RCE COLLEGE NOTICE BOARD");
            System.out.println("====================================");
            System.out.println("Server started successfully!");
            System.out.println("Website: http://localhost:" + PORT);
            System.out.println("Admin: " + ADMIN_USER + " / " + ADMIN_PASS);
            System.out.println("Student: " + STUDENT_ROLL + " / " + STUDENT_PASS);
            System.out.println("====================================");
            System.out.println("Press Ctrl + C to stop the server.");

        } catch (IOException e) {

            System.out.println("Unable to start server.");
            System.out.println("Check whether port " + PORT + " is already in use.");
            e.printStackTrace();
        }
    }
}
