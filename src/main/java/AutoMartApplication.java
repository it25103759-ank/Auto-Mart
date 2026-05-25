package com.automart;

import java.util.Scanner;

/**
 * ============================================================
 *  AutoMartApplication.java  (Profile Settings Entry Point)
 *  Component 05 — Profile Settings
 *  Student : Siriwardana A.K.L.B.  |  IT25103761
 * ============================================================
 *
 *  PURPOSE
 *  -------
 *  This file is the main entry point for the Profile Settings
 *  component of the AutoMart Second-Hand Car Marketplace.
 *
 *  It presents a console menu that connects every CRUD operation
 *  in ProfileSettingsManager to the user through Scanner input.
 *
 *  MENU STRUCTURE
 *  --------------
 *  1. Create Profile Settings   → ProfileSettingsManager.createProfile()
 *  2. View Profile Information  → ProfileSettingsManager.viewProfile()
 *  3. Update Username           → ProfileSettingsManager.updateUsername()
 *  4. Update Appearance         → ProfileSettingsManager.updateAppearance()
 *  5. Update Password           → ProfileSettingsManager.updatePassword()
 *  6. Delete Profile            → ProfileSettingsManager.deleteProfile()
 *  0. Exit
 *
 *  OOP CONCEPTS USED
 *  -----------------
 *  Encapsulation  : Menu logic is isolated inside printMenu() and
 *                   handleChoice(); no logic leaks into main().
 *  Abstraction    : main() only knows about ProfileSettingsManager —
 *                   it has no knowledge of files or Profile internals.
 *  Composition    : AutoMartApplication uses (has-a) a
 *                   ProfileSettingsManager object.
 *
 *  HOW TO RUN (from project root)
 *  --------------------------------
 *  javac -d out src/main/java/com/automart/Profile.java \
 *               src/main/java/com/automart/ProfileSettingsManager.java \
 *               src/main/java/com/automart/AutoMartApplication.java
 *
 *  java -cp out com.automart.AutoMartApplication
 * ============================================================
 */
public class AutoMartApplication {

    // ── Shared Scanner (one instance for the whole application) ──────────
    // We create it once here and pass it to ProfileSettingsManager so
    // we never accidentally open two Scanners on System.in at the same time.
    private static final Scanner scanner = new Scanner(System.in);

    // ── The service that handles all CRUD logic ───────────────────────────
    private static final ProfileSettingsManager profileManager =
            new ProfileSettingsManager(scanner);

    // ─────────────────────────────────────────────────────────────────────

    /**
     * Application entry point.
     *
     * Displays the welcome banner, then loops on the main menu until
     * the user chooses to exit.
     *
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {
        printWelcomeBanner();

        boolean running = true;

        while (running) {
            printMenu();

            System.out.print("  Enter your choice : ");
            String choice = scanner.nextLine().trim();

            running = handleChoice(choice);
        }

        printGoodbye();
        scanner.close(); // always 
    }


    // ══════════════════════════════════════════════════════════════════════

    // ══════════════════════════════════════════════════════════════════════

    /**
     * Prints the welcome banner shown once on application sta.
     */
    private static void printWelcomeBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║          AUTO MART — Second-Hand Car Marketplace     ║");
        System.out.println("║            Component 05 : Profile Settings           ║");
        System.out.println("║         Siriwardana A.K.L.B.  |  IT25103761          ║");
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.println("║  Data file : src/main/resources/data/profiles.txt   ║");
        System.out.println("║  Format    : profileId,username,appearance,password  ║");
        System.out.println("║  Example   : P001,Kasun,DarkMode,1234               ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Prints the main CRUD menu.
     * Each option maps directly to one CRUD operation.
     */
    private static void printMenu() {
        System.out.println("┌──────────────────────────────────────────────────────┐");
        System.out.println("│               PROFILE SETTINGS MENU                 │");
        System.out.println("├──────────────────────────────────────────────────────┤");
        System.out.println("│  [C]  CREATE                                         │");
        System.out.println("│   1.  Create Profile Settings                        │");
        System.out.println("│                                                      │");
        System.out.println("│  [R]  READ                                           │");
        System.out.println("│   2.  View Profile Information                       │");
        System.out.println("│                                                      │");
        System.out.println("│  [U]  UPDATE                                         │");
        System.out.println("│   3.  Update Username                                │");
        System.out.println("│   4.  Update Appearance Settings                     │");
        System.out.println("│   5.  Update Password                                │");
        System.out.println("│                                                      │");
        System.out.println("│  [D]  DELETE                                         │");
        System.out.println("│   6.  Delete Profile                                 │");
        System.out.println("│                                                      │");
        System.out.println("│   0.  Exit                                           │");
        System.out.println("└──────────────────────────────────────────────────────┘");
    }


    // ══════════════════════════════════════════════════════════════════════
    //  Menu routing — connects menu choices to ProfileSettingsManager
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Routes the user's menu choice to the correct CRUD method.
     *
     * @param choice The raw string the user typed
     * @return true to keep the loop running, false to exit
     */
    private static boolean handleChoice(String choice) {
        switch (choice) {

            // ── CREATE ──────────────────────────────────────────────────
            case "1":
                profileManager.createProfile();
                break;

            // ── READ ────────────────────────────────────────────────────
            case "2":
                profileManager.viewProfile();
                break;

            // ── UPDATE (three sub-operations) ───────────────────────────
            case "3":
                profileManager.updateUsername();
                break;

            case "4":
                profileManager.updateAppearance();
                break;

            case "5":
                profileManager.updatePassword();
                break;

            // ── DELETE ──────────────────────────────────────────────────
            case "6":
                profileManager.deleteProfile();
                break;

            // ── EXIT ────────────────────────────────────────────────────
            case "0":
                return false; // signal the loop to stop

            // ── INVALID INPUT ───────────────────────────────────────────
            default:
                System.out.println("\n  ✘  Invalid choice '" + choice
                        + "'. Please enter a number from 0 to 6.\n");
                break;
        }

        // Pause briefly so the user can read the output before the menu redraws
        pauseForReadability();
        return true; // keep looping
    }

    /**
     * Prints the goodbye message shown on exit.
     */
    private static void printGoodbye() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║    Thank you for using AutoMart Profile Settings!    ║");
        System.out.println("║          All changes saved to profiles.txt           ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Asks the user to press ENTER before redrawing the menu.
     * This keeps the console readable on small terminals.
     */
    private static void pauseForReadability() {
        System.out.print("  [ Press ENTER to return to the menu ] ");
        scanner.nextLine();
    }
}
