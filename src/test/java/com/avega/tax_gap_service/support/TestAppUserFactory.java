
package com.avega.tax_gap_service.support;

import com.avega.tax_gap_service.domain.AppUser;

import java.lang.reflect.Field;

public final class TestAppUserFactory {

    private TestAppUserFactory() {
    }

    public static AppUser enabledAdmin(String username, String passwordHash)
            throws ReflectiveOperationException {

        var constructor = AppUser.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        AppUser user = constructor.newInstance();

        setField(user, "username", username);
        setField(user, "passwordHash", passwordHash);
        setField(user, "role", "ADMIN");
        setField(user, "enabled", true);

        return user;
    }

    private static void setField(
            AppUser user, String fieldName, Object value)
            throws ReflectiveOperationException {

        Field field = AppUser.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(user, value);
    }
}
