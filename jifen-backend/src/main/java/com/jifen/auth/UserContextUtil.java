package com.jifen.auth;

public class UserContextUtil {
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IS_ADMIN_HOLDER = new ThreadLocal<>();

    public static void setUserId(Long userId) { USER_ID_HOLDER.set(userId); }
    public static Long getUserId() { return USER_ID_HOLDER.get(); }

    public static void setUsername(String username) { USERNAME_HOLDER.set(username); }
    public static String getUsername() { return USERNAME_HOLDER.get(); }

    public static void setIsAdmin(Boolean isAdmin) { IS_ADMIN_HOLDER.set(isAdmin); }
    public static Boolean getIsAdmin() { 
        Boolean v = IS_ADMIN_HOLDER.get();
        return v != null && v;
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
        USERNAME_HOLDER.remove();
        IS_ADMIN_HOLDER.remove();
    }
}
