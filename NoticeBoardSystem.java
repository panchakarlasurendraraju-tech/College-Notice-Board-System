
import java.util.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// ================= NOTICE CLASS =================
class Notice implements Serializable {

    private static final long serialVersionUID = 1L;

    int id;
    String title;
    String description;
    String category;
    boolean pinned;
    boolean published;
    String date;

    Notice(int id, String title, String description,
           String category, boolean pinned, boolean published) {

        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.pinned = pinned;
        this.published = published;

        date = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
        );
    }
}

// ================= MAIN CLASS =================
public class NoticeBoardSystem {

    static Scanner sc = new Scanner(System.in);

    static ArrayList<Notice> notices = new ArrayList<>();

    static int nextId = 1;

    static final String FILE_NAME = "notices.dat";

    // ================= SAFE TEXT =================
    static String safe(String value) {
        return value == null ? "" : value;
    }

    // ================= LOAD NOTICES =================
    @SuppressWarnings("unchecked")
    static void loadNotices() {

        File file = new File(FILE_NAME);

        if (!file.exists()) {
            return;
        }

        try (ObjectInputStream in =
             new ObjectInputStream(new FileInputStream(file))) {

            Object data = in.readObject();

            if (data instanceof ArrayList<?>) {

                ArrayList<?> saved = (ArrayList<?>) data;

                notices.clear();

                for (Object obj : saved) {

                    if (obj instanceof Notice) {

                        Notice n = (Notice) obj;

                        if (n != null) {

                            n.title = safe(n.title);
                            n.description = safe(n.description);
                            n.category = safe(n.category);
                            n.date = safe(n.date);

                            notices.add(n);

                            if (n.id >= nextId) {
                                nextId = n.id + 1;
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {

            System.out.println("Error loading saved notices.");
            System.out.println("Check the notices.dat file.");
        }
    }

    // ================= SAVE NOTICES =================
    static void saveNotices() {

        try (ObjectOutputStream out =
             new ObjectOutputStream(
                 new FileOutputStream(FILE_NAME))) {

            out.writeObject(notices);

        } catch (IOException e) {

            System.out.println("Error saving notices!");
        }
    }

    // ================= INPUT NUMBER =================
    static int getNumber() {

        while (true) {

            try {

                return Integer.parseInt(sc.nextLine().trim());

            } catch (NumberFormatException e) {

                System.out.print("Enter a valid number: ");
            }
        }
    }

    // ================= DISPLAY NOTICE =================
    static void displayNotice(Notice n) {

        System.out.println("\n======================================");
        System.out.println("NOTICE ID   : " + n.id);
        System.out.println("TITLE       : " + safe(n.title));
        System.out.println("DESCRIPTION : " + safe(n.description));
        System.out.println("CATEGORY    : " + safe(n.category));
        System.out.println("DATE        : " + safe(n.date));
        System.out.println("PINNED      : " + (n.pinned ? "Yes" : "No"));
        System.out.println("STATUS      : " +
            (n.published ? "Published" : "Draft"));
        System.out.println("======================================");
    }

    // ================= FIND NOTICE =================
    static Notice findNotice(int id) {

        for (Notice n : notices) {

            if (n != null && n.id == id) {
                return n;
            }
        }

        return null;
    }

    // ================= SORT NOTICES =================
    static ArrayList<Notice> getSortedNotices(
            boolean publishedOnly) {

        ArrayList<Notice> result = new ArrayList<>();

        for (Notice n : notices) {

            if (n != null &&
                (!publishedOnly || n.published)) {

                result.add(n);
            }
        }

        // Pinned notices first, then newest ID first.
        // Does not use nullable String comparisons.
        result.sort((a, b) -> {

            int pinCompare =
                Boolean.compare(b.pinned, a.pinned);

            if (pinCompare != 0) {
                return pinCompare;
            }

            return Integer.compare(b.id, a.id);
        });

        return result;
    }

    // ================= ADMIN LOGIN =================
    static boolean adminLogin() {

        System.out.println("\n========== ADMIN LOGIN ==========");

        System.out.print("Username: ");
        String username = sc.nextLine();

        System.out.print("Password: ");
        String password = sc.nextLine();

        if (username.equals("admin") &&
            password.equals("admin123")) {

            System.out.println("\nAdmin login successful!");
            return true;
        }

        System.out.println("\nInvalid username or password!");
        return false;
    }

    // ================= ADD NOTICE =================
    static void addNotice() {

        System.out.println("\n========== ADD NEW NOTICE ==========");

        System.out.print("Enter title: ");
        String title = sc.nextLine().trim();

        if (title.isEmpty()) {
            System.out.println("Title cannot be empty!");
            return;
        }

        System.out.print("Enter description: ");
        String description = sc.nextLine().trim();

        System.out.print("Enter category: ");
        String category = sc.nextLine().trim();

        if (category.isEmpty()) {
            category = "General";
        }

        System.out.print("Pin this notice? (yes/no): ");
        boolean pinned =
            sc.nextLine().equalsIgnoreCase("yes");

        System.out.print("Publish now? (yes/no): ");
        boolean published =
            sc.nextLine().equalsIgnoreCase("yes");

        Notice n = new Notice(
            nextId++,
            title,
            description,
            category,
            pinned,
            published
        );

        notices.add(n);

        saveNotices();

        System.out.println("\nNotice added successfully!");
    }

    // ================= VIEW ALL NOTICES =================
    static void viewAllNotices() {

        System.out.println("\n========== ALL NOTICES ==========");

        ArrayList<Notice> list =
            getSortedNotices(false);

        if (list.isEmpty()) {

            System.out.println("No notices available.");
            return;
        }

        for (Notice n : list) {
            displayNotice(n);
        }
    }

    // ================= EDIT NOTICE =================
    static void editNotice() {

        System.out.print("\nEnter Notice ID to edit: ");

        int id = getNumber();

        Notice n = findNotice(id);

        if (n == null) {

            System.out.println("Notice not found!");
            return;
        }

        System.out.println(
            "\nLeave a field empty to keep its old value."
        );

        System.out.print("New title: ");
        String title = sc.nextLine().trim();

        if (!title.isEmpty()) {
            n.title = title;
        }

        System.out.print("New description: ");
        String description = sc.nextLine().trim();

        if (!description.isEmpty()) {
            n.description = description;
        }

        System.out.print("New category: ");
        String category = sc.nextLine().trim();

        if (!category.isEmpty()) {
            n.category = category;
        }

        System.out.print("Pin? (yes/no/keep): ");
        String pin = sc.nextLine();

        if (pin.equalsIgnoreCase("yes")) {
            n.pinned = true;
        } else if (pin.equalsIgnoreCase("no")) {
            n.pinned = false;
        }

        System.out.print("Publish? (yes/no/keep): ");
        String status = sc.nextLine();

        if (status.equalsIgnoreCase("yes")) {
            n.published = true;
        } else if (status.equalsIgnoreCase("no")) {
            n.published = false;
        }

        saveNotices();

        System.out.println("\nNotice updated successfully!");
    }

    // ================= DELETE NOTICE =================
    static void deleteNotice() {

        System.out.print("\nEnter Notice ID to delete: ");

        int id = getNumber();

        Notice n = findNotice(id);

        if (n == null) {

            System.out.println("Notice not found!");
            return;
        }

        displayNotice(n);

        System.out.print(
            "\nAre you sure you want to delete? (yes/no): "
        );

        String confirm = sc.nextLine();

        if (confirm.equalsIgnoreCase("yes")) {

            notices.remove(n);

            saveNotices();

            System.out.println("\nNotice deleted successfully!");

        } else {

            System.out.println("Delete cancelled.");
        }
    }

    // ================= STUDENT VIEW =================
    static void studentView() {

        System.out.println("\n========== STUDENT NOTICE BOARD ==========");

        ArrayList<Notice> list =
            getSortedNotices(true);

        if (list.isEmpty()) {

            System.out.println("No published notices available.");
            return;
        }

        for (Notice n : list) {
            displayNotice(n);
        }
    }

    // ================= SEARCH NOTICES =================
    static void searchNotices(boolean publishedOnly) {

        System.out.print("\nEnter title or keyword: ");

        String keyword =
            sc.nextLine().trim().toLowerCase();

        if (keyword.isEmpty()) {
            System.out.println("Please enter a keyword.");
            return;
        }

        boolean found = false;

        for (Notice n : getSortedNotices(publishedOnly)) {

            if (safe(n.title).toLowerCase().contains(keyword) ||
                safe(n.description).toLowerCase().contains(keyword) ||
                safe(n.category).toLowerCase().contains(keyword)) {

                displayNotice(n);
                found = true;
            }
        }

        if (!found) {
            System.out.println("No matching notices found.");
        }
    }

    // ================= FILTER CATEGORY =================
    static void filterByCategory(boolean publishedOnly) {

        System.out.print("\nEnter category: ");

        String category =
            sc.nextLine().trim();

        boolean found = false;

        for (Notice n : getSortedNotices(publishedOnly)) {

            if (safe(n.category).equalsIgnoreCase(category)) {

                displayNotice(n);
                found = true;
            }
        }

        if (!found) {
            System.out.println("No notices in this category.");
        }
    }

    // ================= ADMIN MENU =================
    static void adminMenu() {

        if (!adminLogin()) {
            return;
        }

        while (true) {

            System.out.println("\n================================");
            System.out.println("          ADMIN DASHBOARD");
            System.out.println("================================");
            System.out.println("1. Add Notice");
            System.out.println("2. View All Notices");
            System.out.println("3. Edit Notice");
            System.out.println("4. Delete Notice");
            System.out.println("5. Search Notices");
            System.out.println("6. Filter by Category");
            System.out.println("7. Logout");

            System.out.print("\nEnter choice: ");

            int choice = getNumber();

            switch (choice) {

                case 1:
                    addNotice();
                    break;

                case 2:
                    viewAllNotices();
                    break;

                case 3:
                    editNotice();
                    break;

                case 4:
                    deleteNotice();
                    break;

                case 5:
                    searchNotices(false);
                    break;

                case 6:
                    filterByCategory(false);
                    break;

                case 7:
                    System.out.println("Admin logged out.");
                    return;

                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    // ================= STUDENT MENU =================
    static void studentMenu() {

        while (true) {

            System.out.println("\n================================");
            System.out.println("         STUDENT DASHBOARD");
            System.out.println("================================");
            System.out.println("1. View Published Notices");
            System.out.println("2. Search Notices");
            System.out.println("3. Filter by Category");
            System.out.println("4. Back to Main Menu");

            System.out.print("\nEnter choice: ");

            int choice = getNumber();

            switch (choice) {

                case 1:
                    studentView();
                    break;

                case 2:
                    searchNotices(true);
                    break;

                case 3:
                    filterByCategory(true);
                    break;

                case 4:
                    return;

                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    // ================= MAIN METHOD =================
    public static void main(String[] args) {

        loadNotices();

        System.out.println("\n");
        System.out.println("==========================================");
        System.out.println("       COLLEGE NOTICE BOARD SYSTEM");
        System.out.println("==========================================");
        System.out.println("       Welcome to College Portal");
        System.out.println("==========================================");

        while (true) {

            System.out.println("\n========== MAIN MENU ==========");
            System.out.println("1. Admin Login");
            System.out.println("2. Student View");
            System.out.println("3. Exit");

            System.out.print("\nEnter choice: ");

            int choice = getNumber();

            switch (choice) {

                case 1:
                    adminMenu();
                    break;

                case 2:
                    studentMenu();
                    break;

                case 3:

                    saveNotices();

                    System.out.println(
                        "\nThank you for using College Notice Board!"
                    );

                    sc.close();

                    return;

                default:
                    System.out.println(
                        "Invalid choice! Please try again."
                    );
            }
        }
    }
}
