/*
 * Copyright (C) 2011 Baidu.com Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.cafe.local;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2011-4-28
 * @version
 * @todo
 */
public class PrivateOperator {

    /**
     * invoke object's private method
     * 
     * @param owner
     *            : target object
     * @param classLevel
     *            : 0 means itself, 1 means it's father, and so on...
     * @param methodName
     *            : name of the target method
     * @param parameterTypes
     *            : types of the target method's parameters
     * @param parameters
     *            : parameters of the target method
     * @return result of invoked method
     * 
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public static Object invokeObjectMethod(Object owner, int classLevel, String methodName, Class[] parameterTypes,
            Object[] parameters) throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        // get class
        Class ownerclass = getOwnerclass(owner, classLevel);

        // get property
        Method method = ownerclass.getDeclaredMethod(methodName, parameterTypes);
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        Object result = method.invoke(owner, parameters);
        return result;
    }

    /**
     * set object's private property with custom value
     * 
     * @param owner
     *            : target object
     * @param classLevel
     *            : 0 means itself, 1 means it's father, and so on...
     * @param fieldName
     *            : name of the target field
     * @param value
     *            : new value of the target field
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public static void setObjectProperty(Object owner, int classLevel, String fieldName, Object value)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        // get class
        Class ownerclass = getOwnerclass(owner, classLevel);

        // get property
        Field field = ownerclass.getDeclaredField(fieldName);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        field.set(owner, value);
    }

    /**
     * get object's private property
     * 
     * @param owner
     *            : target object
     * @param classLevel
     *            : 0 means itself, 1 means it's father, and so on...
     * @param fieldName
     *            : name of the target field
     * @return value of the target field
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public static Object getObjectProperty(Object owner, int classLevel, String fieldName) throws SecurityException,
            NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        // get class
        Class ownerclass = getOwnerclass(owner, classLevel);

        // get property
        Field field = ownerclass.getDeclaredField(fieldName);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        Object property = field.get(owner);
        return property;
    }

    private static Class getOwnerclass(Object owner, int classLevel) {
        Class ownerclass = owner.getClass();
        for (int i = 0; i < classLevel; i++) {
            ownerclass = ownerclass.getSuperclass();
        }
        return ownerclass;
    }

    /**
     * @param owner
     *            target object
     * @param classLevel
     *            0 means itself, 1 means it's father, and so on...
     * @param typeString
     *            e.g. java.lang.String
     * @return ArrayList<String> of property's name
     */
    public static ArrayList<String> getPropertyNameByType(Object owner, int classLevel, String typeString) {
        ArrayList<String> names = new ArrayList<String>();
        // get class
        Class ownerclass = getOwnerclass(owner, classLevel);

        // get type
        for (Field field : ownerclass.getDeclaredFields()) {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }

            String type = field.getType().toString();
            if (type.startsWith("class ")) {
                type = type.substring("class ".length());
            }

            if (type.equals(typeString)) {
                names.add(field.getName());
            }
        }

        return names;
    }

}
