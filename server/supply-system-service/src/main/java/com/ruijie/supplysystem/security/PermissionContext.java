package com.ruijie.supplysystem.security;

import java.util.Set;

public final class PermissionContext {

    private static final ThreadLocal<Set<String>> PERMISSIONS = new ThreadLocal<>();

    private PermissionContext() {}

    public static void set(Set<String> codes) {
        PERMISSIONS.set(codes);
    }

    public static boolean contains(String code) {
        Set<String> codes = PERMISSIONS.get();
        return codes != null && codes.contains(code);
    }

    public static void clear() {
        PERMISSIONS.remove();
    }
}
