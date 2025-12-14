package com.invex.invex.util;

import com.invex.invex.entities.User;

public final class SessionContext {

    private static User currentUser;

    private SessionContext() {}

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        currentUser = user;
    }

    public static boolean isAuthenticated() {
        return currentUser != null;
    }

    public static void clear() {
        currentUser = null;
    }
}
