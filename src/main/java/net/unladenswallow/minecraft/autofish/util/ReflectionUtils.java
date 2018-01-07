package net.unladenswallow.minecraft.autofish.util;

import java.lang.reflect.Field;

public class ReflectionUtils {

    /**
     * Using Java reflection APIs, access a private member data of type int
     * 
     * @param object The target object
     * @param fieldName The name of the private data field in object
     * 
     * @return The int value of the private member data from object with fieldName
     */
    public static int getPrivateIntFieldFromObject(Object object, String forgeFieldName, String vanillaFieldName) throws NoSuchFieldException, SecurityException, NumberFormatException, IllegalArgumentException, IllegalAccessException {
        Field targetField = null;
        try {
            targetField = object.getClass().getDeclaredField(forgeFieldName);
        } catch (NoSuchFieldException e) {
            targetField = object.getClass().getDeclaredField(vanillaFieldName);
        }
        if (targetField != null) {
            targetField.setAccessible(true);
            return Integer.valueOf(targetField.get(object).toString()).intValue();
        } else {
            return 0;
        }
            
    }

    /**
     * Using Java reflection APIs, set a private member data of type int
     * 
     * @param object The target object
     * @param fieldName The name of the private data field in object
     * @param value The int value to set the private member data from object with fieldName
     * 
     */
    public static void setPrivateIntFieldOfObject(Object object, String forgeFieldName, String vanillaFieldName, int value) throws NoSuchFieldException, SecurityException, NumberFormatException, IllegalArgumentException, IllegalAccessException {
        Field targetField = null;
        try {
            targetField = object.getClass().getDeclaredField(forgeFieldName);
        } catch (NoSuchFieldException e) {
            targetField = object.getClass().getDeclaredField(vanillaFieldName);
        }
        if (targetField != null) {
            targetField.setAccessible(true);
            targetField.set(object, value);
        }
    }


}
