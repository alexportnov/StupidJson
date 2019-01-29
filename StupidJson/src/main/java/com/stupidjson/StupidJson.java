/*
 * Copyright 2018 Alex Portnov
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


package com.stupidjson;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

public class StupidJson
{
    private final static String TAG = "StupidJson";

    private final static char[] NULL = new char[] {'n', 'u', 'l', 'l'};
    private final static char QUOTE = '"';
    private final static char QUOTE_ESCAPE = '\\';
    private final static char[] QUOTE_END = new char[] {'"', ':'};
    private final static char DELIMITER = ',';
    private final static char OBJECT_START = '{';
    private final static char OBJECT_END = '}';
    private final static char ARRAY_START = '[';
    private final static char ARRAY_END = ']';

    /***********************************************************************************************/
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NameOverride
    {
        String value() default "";
    }

    /***********************************************************************************************/
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ignore
    {

    }

    /***********************************************************************************************/
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DontQuote
    {

    }

    /***********************************************************************************************/
    public static String toJson(Object src)
    {
        if(src == null)
        {
            return null;
        }

        StringBuilder sb = new StringBuilder(256);

        try
        {
            Class<?> fc = src.getClass();
            if(fc.isArray())
            {
                storeArray(sb, src);
            }
            else if (Collection.class.isAssignableFrom(fc))
            {
                storeCollection(sb, (Collection)src);
            }
            else if (Hashtable.class.isAssignableFrom(fc))
            {
                storeHashtable(sb, (Hashtable)src);
            }
            else
            {
                storeObject(sb, src);
            }

            return sb.toString();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /***********************************************************************************************/
    private static void storeObject(StringBuilder sb, Object src) throws IllegalAccessException, NoSuchFieldException
    {
        if(src == null)
        {
            sb.append(NULL);
            return;
        }

        // handle primitives
        Class<?> sc = src.getClass();
        if(sc.isPrimitive() || canAssign(String.class, sc) || sc.isEnum() ||
                canAssign(Number.class, sc) || canAssign(Boolean.class, sc) ||
                sc.isArray() || canAssign(Character.class, sc) ||
                Collection.class.isAssignableFrom(sc) || Hashtable.class.isAssignableFrom(sc))
        {
            storeField(sb, null, sc, src);
            return;
        }

        // composite object
        sb.append(OBJECT_START);

        int added = 0;
        // TODO: this will not pull fields from extended classes
        Field[] fields = sc.getDeclaredFields();
        for (Field f: fields)
        {
            int mods = f.getModifiers();
            if(Modifier.isStatic(mods) || f.isSynthetic())
            {
                continue;
            }

            if(f.isAnnotationPresent(Ignore.class))
            {
                continue;
            }

            if(Modifier.isPrivate(mods) || Modifier.isProtected(mods))
            {
                f.setAccessible(true);
            }

            Object value = f.get(src);
            Class<?> fc = f.getType();
            String name = getFieldName(f);

            if(added > 0)
            {
                sb.append(DELIMITER);
            }

            sb.append(QUOTE);
            sb.append(name);
            sb.append(QUOTE_END);
            storeField(sb, f, fc, value);
            added++;
        }

        sb.append(OBJECT_END);
    }

    /***********************************************************************************************/
    private static void storeField(StringBuilder sb, Field f, Class<?> fc, Object value) throws NoSuchFieldException, IllegalAccessException
    {
        if(value == null)
        {
            sb.append(NULL);
            return;
        }

        if(fc.isPrimitive() || canAssign(Number.class, fc) ||
                canAssign(Boolean.class, fc) || canAssign(Character.class, fc))
        {
            String v = value.toString();
            sb.append(v);
        }
        else if(canAssign(String.class, fc))
        {
            if(f != null && f.isAnnotationPresent(DontQuote.class))
            {
                sb.append((String)value);
            }
            else
            {
                sb.append(QUOTE);
                sb.append(escapeString((String)value));
                sb.append(QUOTE);
            }
        }
        else if(fc.isEnum())
        {
            String v = value.toString();
            Field ff = fc.getField(v);
            if(ff.isAnnotationPresent(NameOverride.class))
            {
                Annotation[] annotations = ff.getAnnotations();
                for (Annotation a : annotations)
                {
                    if (a instanceof NameOverride)
                    {
                        v = ((NameOverride) a).value();
                        break;
                    }
                }
            }

            sb.append(QUOTE);
            sb.append(v);
            sb.append(QUOTE);
        }
        else if(fc.isArray())
        {
            storeArray(sb, value);
        }
        else if (Collection.class.isAssignableFrom(fc))
        {
            storeCollection(sb, (Collection)value);
        }
        else if (Hashtable.class.isAssignableFrom(fc))
        {
            storeHashtable(sb, (Hashtable)value);
        }
        else
        {
            storeObject(sb, value);
        }
    }

    /***********************************************************************************************/
    private static String getFieldName(Field f)
    {
        String name = f.getName();

        if(!f.isAnnotationPresent(NameOverride.class))
        {
            return name;
        }

        Annotation[] annotations = f.getDeclaredAnnotations();
        for(Annotation a: annotations)
        {
            if(a instanceof NameOverride)
            {
                name = ((NameOverride)a).value();
                break;
            }
        }

        return name;
    }

    /***********************************************************************************************/
    private static void storeArray(StringBuilder sb, Object fc) throws IllegalAccessException, NoSuchFieldException
    {
        if(fc == null)
        {
            sb.append(NULL);
            return;
        }

        sb.append(ARRAY_START);

        int length = Array.getLength(fc);
        for (int j = 0; j < length; j++)
        {
            if (j > 0)
            {
                sb.append(DELIMITER);
            }

            Object e = Array.get(fc, j);
            storeObject(sb, e);
        }

        sb.append(ARRAY_END);
    }

    /***********************************************************************************************/
    private static void storeCollection(StringBuilder sb, Collection list) throws IllegalAccessException, NoSuchFieldException
    {
        if(list == null)
        {
            sb.append(NULL);
            return;
        }

        sb.append(ARRAY_START);

        int j = 0;
        for (Object o: list)
        {
            if(j > 0)
            {
                sb.append(DELIMITER);
            }

            storeObject(sb, o);
            j++;
        }

        sb.append(ARRAY_END);
    }

    /***********************************************************************************************/
    private static void storeHashtable(StringBuilder sb, Hashtable hash) throws IllegalAccessException, NoSuchFieldException
    {
        if(hash == null)
        {
            sb.append(NULL);
            return;
        }

        sb.append(OBJECT_START);

        int j = 0;
        for (Object k: hash.keySet())
        {
            if(j > 0)
            {
                sb.append(DELIMITER);
            }

            sb.append(QUOTE);
            sb.append(k.toString());
            sb.append(QUOTE_END);

            storeObject(sb, hash.get(k));
            j++;
        }

        sb.append(OBJECT_END);
    }

    /***********************************************************************************************/
    public static <T> T fromJson(byte[] json, Class<T> classOfT)
    {
        return fromJson(new String(json), classOfT);
    }

    /***********************************************************************************************/
    @SuppressWarnings("unchecked")
    public static <T> T fromJson(String json, Class<T> classOfT)
    {
        if(json == null)
        {
            return null;
        }

        try
        {
            if(classOfT.isArray())
            {
                JSONArray a = new JSONArray(json);
                return (T)parseArray(a, classOfT.getComponentType());
            }
            else if (Collection.class.isAssignableFrom(classOfT))
            {
                JSONArray a = new JSONArray(json);
                ParameterizedType pt = (ParameterizedType)classOfT.getGenericSuperclass();
                if(pt == null)
                {
                    return null;
                }

                Type[] tt = pt.getActualTypeArguments();
                Class<T> fcc = (Class<T>)tt[0];
                return (T)parseList(a, fcc);
            }
            else if (Hashtable.class.isAssignableFrom(classOfT))
            {
                //TODO
                return null;
            }
            else
            {
                T instance = classOfT.newInstance();
                JSONObject j = new JSONObject(json);
                return fromJson(j, instance);
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "Error fromJson on=(" + json + ") class= " + classOfT);
            e.printStackTrace();
            return null;
        }
    }

    /***********************************************************************************************/
    private static <T> T fromJson(JSONObject j, T instance) throws JSONException, InstantiationException
    {
        if(j == null)
        {
            return null;
        }

        // TODO: this will not pull fields from extended classes
        Field[] fields = instance.getClass().getDeclaredFields();
        for (Field f: fields)
        {
            int mods = f.getModifiers();
            if(f.isSynthetic() || Modifier.isStatic(mods))
            {
                continue;
            }

            if(f.isAnnotationPresent(Ignore.class))
            {
                continue;
            }

            if(Modifier.isPrivate(mods) || Modifier.isProtected(mods))
            {
                f.setAccessible(true);
            }

            String name = getFieldName(f);
            Class<?> fc = f.getType();

            try
            {
                if (j.isNull(name) && !fc.isPrimitive())
                {
                    f.set(instance, null);
                }
                else if (canAssign(fc,String.class))
                {
                    f.set(instance, j.optString(name, null));
                }
                else if (canAssign(fc,int.class))
                {
                    f.setInt(instance, j.optInt(name, 0));
                }
                else if (canAssign(fc,Integer.class))
                {
                    f.set(instance, j.optInt(name, 0));
                }
                else if (canAssign(fc,boolean.class))
                {
                    f.setBoolean(instance, j.optBoolean(name, false));
                }
                else if (canAssign(fc,Boolean.class))
                {
                    f.set(instance, j.optBoolean(name, false) ? Boolean.TRUE : Boolean.FALSE);
                }
                else if (canAssign(fc,double.class))
                {
                    f.setDouble(instance, j.optDouble(name, 0));
                }
                else if (canAssign(fc,Double.class))
                {
                    f.set(instance, j.optDouble(name, 0));
                }
                else if (canAssign(fc,float.class))
                {
                    f.setFloat(instance, (float) j.optDouble(name, 0));
                }
                else if (canAssign(fc,Float.class))
                {
                    f.set(instance, (float) j.optDouble(name, 0));
                }
                else if (canAssign(fc,byte.class))
                {
                    f.setByte(instance, (byte) j.optInt(name, 0));
                }
                else if (canAssign(fc,Byte.class))
                {
                    f.set(instance, (byte) j.optInt(name, 0));
                }
                else if (canAssign(fc,long.class))
                {
                    f.setLong(instance, j.optLong(name, 0));
                }
                else if (canAssign(fc,Long.class))
                {
                    f.set(instance, j.optLong(name, 0));
                }
                else if (canAssign(fc,short.class))
                {
                    f.setShort(instance, (short) j.optInt(name, 0));
                }
                else if (canAssign(fc,Short.class))
                {
                    f.set(instance, (short) j.optInt(name, 0));
                }
                else if (canAssign(fc,char.class))
                {
                    f.setChar(instance, (char) j.optInt(name, 0));
                }
                else if (canAssign(fc,Character.class))
                {
                    f.set(instance, (char) j.optInt(name, 0));
                }

                else if (fc.isEnum())
                {
                    f.set(instance, enumValueFromString(fc, j.optString(name, null)));
                }
                else if (fc.isArray())
                {
                    JSONArray a = j.optJSONArray(name);
                    f.set(instance, parseArray(a, fc.getComponentType()));
                }
                else if (Collection.class.isAssignableFrom(fc))
                {
                    JSONArray a = j.optJSONArray(name);
                    Type genericType = f.getGenericType();
                    ParameterizedType parameterizedType = (ParameterizedType) genericType;
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    Class<?> fcc = (Class<?>) actualTypeArguments[0];

                    f.set(instance, parseList(a, fcc));
                }
                else
                {
                    JSONObject jj = j.optJSONObject(name);
                    Object ii = fc.newInstance();
                    f.set(instance, fromJson(jj, ii));
                }
            }
            catch(IllegalAccessException ex)
            {
                Log.d(TAG, "Exception=" + ex, ex);
                // field was optimized out? NO-OP
            }
        }

        return instance;
    }

    /***********************************************************************************************/
    private static Object parseArray(JSONArray a, Class<?> fcc) throws JSONException, InstantiationException, IllegalAccessException
    {
        if(a == null)
        {
            return null;
        }

        Object arr = Array.newInstance(fcc, a.length());
        for(int k = 0; k < a.length(); ++k)
        {
            if(canAssign(fcc,String.class))
            {
                Array.set(arr, k, a.optString(k, null));
            }
            else if(canAssign(fcc,boolean.class))
            {
                Array.setBoolean(arr, k, a.optBoolean(k, false));
            }
            else if(canAssign(fcc,Boolean.class))
            {
                Array.set(arr, k, a.optBoolean(k, false));
            }
            else if(canAssign(fcc,int.class) )
            {
                Array.setInt(arr, k, a.optInt(k, 0));
            }
            else if(canAssign(fcc,Integer.class))
            {
                Array.set(arr, k, a.optInt(k, 0));
            }
            else if(fcc.isEnum())
            {
                Array.set(arr, k, enumValueFromString(fcc, a.optString(k, null)));
            }
            else if(canAssign(fcc,double.class))
            {
                Array.setDouble(arr, k, a.optDouble(k, 0));
            }
            else if(canAssign(fcc,Double.class))
            {
                Array.set(arr, k, a.optDouble(k, 0));
            }
            else if(canAssign(fcc,float.class))
            {
                Array.setFloat(arr, k, (float)a.optDouble(k, 0));
            }
            else if(canAssign(fcc,Float.class))
            {
                Array.set(arr, k, (float)a.optDouble(k, 0));
            }
            else if(canAssign(fcc,byte.class))
            {
                Array.setByte(arr, k, (byte)a.optInt(k, 0));
            }
            else if(canAssign(fcc,Byte.class))
            {
                Array.set(arr, k, (byte)a.optInt(k, 0));
            }
            else if(canAssign(fcc,long.class))
            {
                Array.setLong(arr, k, a.optLong(k, 0));
            }
            else if(canAssign(fcc,Long.class))
            {
                Array.set(arr, k,  a.optLong(k, 0));
            }
            else if(canAssign(fcc,short.class))
            {
                Array.setShort(arr, k, (short)a.optInt(k, 0));
            }
            else if(canAssign(fcc,Short.class))
            {
                Array.set(arr, k, (short)a.optInt(k, 0));
            }
            else if(canAssign(fcc,char.class))
            {
                Array.setChar(arr, k, (char)a.optInt(k, 0));
            }
            else if(canAssign(fcc,Character.class))
            {
                Array.set(arr, k, (char)a.optInt(k, 0));
            }
            else
            {
                Object ii = fcc.newInstance();
                Array.set(arr, k, fromJson(a.optJSONObject(k), ii));
            }
        }

        return arr;
    }

    /***********************************************************************************************/
    @SuppressWarnings("unchecked")
    private static Collection parseList(JSONArray a, Class<?> fcc) throws JSONException, InstantiationException, IllegalAccessException
    {
        if(a == null)
        {
            return null;
        }

        Collection list = new ArrayList<>();
        for(int k = 0; k < a.length(); ++k)
        {
            // list cannot have primitives as templated value
            if(canAssign(fcc,String.class))
            {
                list.add(a.optString(k, null));
            }
            else if(canAssign(fcc,Integer.class))
            {
                list.add(a.optInt(k, 0));
            }
            else if(canAssign(fcc,Boolean.class))
            {
                list.add(a.optBoolean(k, false));
            }
            else if(fcc.isEnum())
            {
                list.add(enumValueFromString(fcc, a.optString(k, null)));
            }
            else if(canAssign(fcc,Double.class))
            {
                list.add(a.optDouble(k, 0));
            }
            else if(canAssign(fcc,Float.class))
            {
                list.add((float)a.optDouble(k, 0));
            }
            else if(canAssign(fcc,Byte.class))
            {
                list.add((byte)a.optInt(k, 0));
            }
            else if(canAssign(fcc,Long.class))
            {
                list.add(a.optLong(k, 0));
            }
            else if(canAssign(fcc, Short.class))
            {
                list.add((short)a.optInt(k, 0));
            }
            else if(canAssign(fcc,Character.class))
            {
                list.add((char)a.optInt(k, 0));
            }
            else
            {
                Object ii = fcc.newInstance();
                list.add(fromJson(a.optJSONObject(k), ii));
            }
        }

        return list;
    }

    /***********************************************************************************************/
    @SuppressWarnings("unchecked")
    private static Enum enumValueFromString(Class<?> fc, String name)
    {
        if(name == null)
        {
            return (Enum)fc.getEnumConstants()[0];
        }

        try
        {
            return Enum.valueOf((Class<Enum>)fc, name);
        }
        catch (IllegalArgumentException e)
        {
            Field[] fs = fc.getFields();
            for(Field f: fs)
            {
                Annotation[] annotations = f.getAnnotations();
                for(Annotation a: annotations)
                {
                    if(a instanceof NameOverride)
                    {
                        String override = ((NameOverride)a).value();
                        if(name.equals(override))
                        {
                            return Enum.valueOf((Class<Enum>)fc, f.getName());
                        }
                    }
                }
            }
        }

        return null;
    }

    /***********************************************************************************************/
    private static boolean canAssign(Class<?> a, Class<?> b)
    {
        if(a == b)
        {
            return true;
        }

        for (b = b.getSuperclass(); b != null; b = b.getSuperclass())
        {
            if (a == b) {
                return true;
            }
        }

        return false;
    }

    /***********************************************************************************************/
    private static String escapeString(String v)
    {
        int idx = v.indexOf(QUOTE);
        if(idx < 0)
        {
            return v;
        }

        StringBuilder sbb = new StringBuilder(v.length() + 40);
        sbb.append(v);

        int added = 0;
        char prev = 0;
        while(idx < v.length() + added)
        {
            char c = sbb.charAt(idx);
            if((QUOTE == c || QUOTE_ESCAPE == c) && QUOTE_ESCAPE != prev)
            {
                sbb.insert(idx++, QUOTE_ESCAPE);
                added++;

                if(QUOTE_ESCAPE == c)
                {
                    sbb.insert(idx++, QUOTE_ESCAPE);
                    added++;
                }
            }

            idx++;
            prev = c;
        }

        return sbb.toString();
    }
}
