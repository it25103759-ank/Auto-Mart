package com.automart;

// ============================================================
//  AutoMart Application  —  Component 01: User Management
//  FILE    : BaseManager.java
//  PURPOSE : Abstract base class for all manager classes.
//            Holds the data file path that each manager needs,
//            so the path is set once at construction and reused.
//
//  OOP CONCEPTS:
//    • Abstraction  – abstract class, cannot be used directly
//    • Inheritance  – UserManager extends BaseManager
//    • Encapsulation – dataFile is protected (accessible to subclasses)
// ============================================================

import java.nio.file.Path;

abstract class BaseManager {

    // ----------------------------------------------------------
    //  Protected field – accessible to subclasses only
    // ----------------------------------------------------------
    /** Path to the data file this manager reads from and writes to. */
    protected final Path dataFile;

    // ----------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------
    /**
     * @param dataFile  the path to the flat-file data store
     *                  e.g. Paths.get("src/main/resources/data/users.txt")
     */
    BaseManager(Path dataFile) {
        this.dataFile = dataFile;
    }

    // ----------------------------------------------------------
    //  Shared utility
    // ----------------------------------------------------------
    /**
     * Returns just the filename from the full path.
     * e.g. "users.txt" from ".../data/users.txt"
     *
     * @return file name string
     */
    String fileName() {
        return dataFile.getFileName().toString();
    }
}
