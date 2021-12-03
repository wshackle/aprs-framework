/*
 * This software is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * Software Copyright/Warranty Disclaimer
 * 
 * This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of their
 * official duties. Pursuant to title 17 Section 105 of the United States
 * Code this software is not subject to copyright protection and is in the
 * public domain.
 * 
 * This software is experimental. NIST assumes no responsibility whatsoever 
 * for its use by other parties, and makes no guarantees, expressed or 
 * implied, about its quality, reliability, or any other characteristic. 
 * We would appreciate acknowledgement if the software is used. 
 * This software can be redistributed and/or modified freely provided 
 * that any derivative works bear some notice that they are derived from it, 
 * and any modified versions bear some notice that they have been modified.
 * 
 *  See http://www.copyright.gov/title17/92chap1.html#105
 * 
 */
package aprs.remote;

import aprs.launcher.LauncherAprsJFrame;
import crcl.base.PointType;
import crcl.base.PoseToleranceType;
import crcl.base.PoseType;
import crcl.utils.CRCLPosemath;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class Scriptable<T> {

    public static <T> Scriptable<T> scriptableOf(Class<T> tclzz, T t) {
        if (t != null) {
            return new Scriptable<>(tclzz, t, getDefaultActionsMap(tclzz), getDefaultFunctionsMap(tclzz));
        } else {
            return scriptableOfStatic(tclzz);
        }
    }

    public static <T> Scriptable<T> scriptableOfStatic(Class<T> tclzz1) {
        return new Scriptable<>(tclzz1, null, getDefaultStaticActionsMap(tclzz1), getDefaultStaticFunctionsMap(tclzz1));
    }

    public static <T> Map<String, ScriptableAction<T>> getDefaultStaticActionsMap(final Class<T> aClass) {
        Map<String, ScriptableAction<T>> map = new TreeMap<>();
        Method methods[] = aClass.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getReturnType() == Void.class || method.getReturnType() == void.class) {
                map.put(method.getName(), new ScriptableAction<T>() {
                    @Override
                    @SuppressWarnings("nullness")
                    public void action(T t, Object[] args, PrintWriter pw) throws Exception {
                        method.invoke(t);
                    }

                    @Override
                    public Class<?>[] getArgTypes() {
                        return method.getParameterTypes();
                    }
                });
            }
        }
        return map;
    }

    public static <T> Map<String, ScriptableAction<T>> getDefaultActionsMap(final Class<T> aClass) throws SecurityException {
        Map<String, ScriptableAction<T>> map = new TreeMap<>();
        Map<String, Integer> nameCountMap = new TreeMap<>();
        Class<?> classForMethods = findClassForMethods(aClass);
        Method methods[] = classForMethods.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            } else if (Modifier.isStatic(method.getModifiers())) {
                continue;
            } else if (method.isBridge() || method.isSynthetic() || method.isVarArgs()) {
                continue;
            } else if (method.getDeclaringClass() == Object.class) {
                continue;
            } else if (method.getDeclaringClass() == java.awt.Component.class) {
                continue;
            } else if (method.getDeclaringClass() == java.awt.Container.class) {
                continue;
            }
            System.out.println("method.getDeclaringClass()=" + method.getDeclaringClass() + ", method = " + method);
            if (method.getReturnType() == Void.class || method.getReturnType() == void.class) {
                int c = nameCountMap.compute(method.getName(), (key, v) -> (v == null) ? 1 : v + 1);
                final String suffix = (c < 2) ? "" : "" + c;
                map.put(method.getName(), new ScriptableAction<T>() {
                    @Override
                    @SuppressWarnings("nullness")
                    public void action(T t, Object[] args, PrintWriter pw) throws Exception {
                        method.invoke(t);
                    }

                    @Override
                    public Class<?>[] getArgTypes() {
                        return method.getParameterTypes();
                    }
                });
            }
        }
        return map;
    }

    public static <T> Map<String, ScriptableFunction<T>> getDefaultStaticFunctionsMap(final Class<T> aClass) {
        Map<String, ScriptableFunction<T>> map = new TreeMap<>();
        Method methods[] = aClass.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            map.put(method.getName(), new ScriptableFunction<T>() {

                @Override
                @SuppressWarnings({"nullness","rawtypes"})
                public @Nullable
                Scriptable<?> applyFunction(T t, Object[] args, PrintWriter pw) throws Exception {
                    Object o = method.invoke(t, args);
                    if (o == null) {
                        return null;
                    }
                    final Class<?> oClass1 = o.getClass();
                    return new Scriptable(oClass1, o, getDefaultActionsMap(oClass1), getDefaultFunctionsMap(oClass1));
                }

                @Override
                public Class<?>[] getArgTypes() {
                    return method.getParameterTypes();
                }
            });
        }
        return map;
    }

    public static <T> Map<String, ScriptableFunction<T>> getDefaultFunctionsMap(Class<T> aClass) throws SecurityException {
        Map<String, ScriptableFunction<T>> map = new TreeMap<>();
        Class<?> classForMethods = findClassForMethods(aClass);
        Method methods[] = classForMethods.getMethods();
        Map<String, Integer> nameCountMap = new TreeMap<>();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            } else if (Modifier.isStatic(method.getModifiers())) {
                continue;
            } else if (method.isBridge() || method.isSynthetic() || method.isVarArgs()) {
                continue;
            } else if (method.getDeclaringClass() == Object.class) {
                continue;
            } else if (method.getDeclaringClass() == java.awt.Component.class) {
                continue;
            } else if (method.getDeclaringClass() == java.awt.Container.class) {
                continue;
            }
            System.out.println("method.getDeclaringClass()=" + method.getDeclaringClass() + ", method = " + method);
            int c = nameCountMap.compute(method.getName(), (key, v) -> (v == null) ? 1 : v + 1);
            final String suffix = (c < 2) ? "" : "" + c;
            map.put(method.getName() + suffix, new ScriptableFunction<T>() {

                @Override
                @SuppressWarnings("nullness")
                public @Nullable
                Scriptable<?> applyFunction(T t, Object[] args, PrintWriter pw) throws Exception {
                    Object o = method.invoke(t, args);
                    if (o == null) {
                        return null;
                    }
                    final Class<? extends Object> oClass = o.getClass();
                    return new Scriptable(oClass, o, getDefaultActionsMap(oClass), getDefaultFunctionsMap(oClass));
                }

                @Override
                public Class<?>[] getArgTypes() {
                    return method.getParameterTypes();
                }
            });
        }
        return map;
    }

    public static Class<?> findClassForMethods(Class<?> aClass) {
        Class<?> classForMethods = aClass;
        if (aClass.getDeclaringClass() == Collections.class) {
            classForMethods = Collections.class;
            Class<?> topClass = aClass;
            do {
                final Class<?>[] interfaces = topClass.getInterfaces();
                System.out.println("interfaces = " + Arrays.toString(interfaces));
                for (int i = 0; i < interfaces.length; i++) {
                    Class<?> iclass = interfaces[i];
                    System.out.println("iclass = " + iclass);
                    if (iclass == List.class) {
                        classForMethods = List.class;
                        break;
                    }
                    if (iclass == Map.class) {
                        classForMethods = Map.class;
                        break;
                    }
                    if (iclass == Set.class) {
                        classForMethods = Set.class;
                        break;
                    }
                }
                System.out.println("aClass = " + aClass);
                System.out.println("classForMethods = " + classForMethods);
            } while (null != (topClass = topClass.getSuperclass()));
        }
        return classForMethods;
    }

    public @Nullable
    T getObj() {
        return obj;
    }

    private final Class<T> tclzz;
    private final @Nullable T obj;
    private final Map<String, ? extends ScriptableAction<T>> actions;
    private final Map<String, ? extends ScriptableFunction<T>> functions;

    public Scriptable(Class<T> tclzz, @Nullable T obj, Map<String, ? extends ScriptableAction<T>> actions, Map<String, ? extends ScriptableFunction<T>> functions) {
        this.tclzz = tclzz;
        this.obj = obj;
        this.actions = actions;
        this.functions = functions;
    }

    public void action(String name, @Nullable Object args[], PrintWriter pw) {
        try {
            final ScriptableAction<T> action = actions.get(name);
            if (null != action) {
                if (action.getArgTypes().length != args.length) {
                    pw.print("Action named " + name + " requires " + action.getArgTypes().length + " arguments not " + args.length + "\r\n");
                    pw.print("        " + name + "(" + Arrays.toString(action.getArgTypes()) + ")\r\n");
                } else {
                    action.action(obj, args, pw);
                    pw.print("Action " + name + " ran\r\n");
                }
            } else {
                pw.print("No action named " + name + "\r\n");
                List<String> actionsList = new ArrayList<>(actions.keySet());
                Collections.sort(actionsList);
                pw.print("actions =  " + actionsList + "\r\n");
            }
        } catch (Exception ex) {
            pw.print("Exception occured for action " + name + " args = " + Arrays.toString(args) + ":\r\n");
            pw.print(ex + "r\n");
            Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    private static @Nullable
    Method findMethod(Class<?> clzz, boolean isStatic, String name, Class<?> inputParamTypes[]) {
        Method methods[] = clzz.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            if (Modifier.isStatic(method.getModifiers()) != isStatic) {
                continue;
            }
            if (method.getName().equals("name")) {
                final Class<?>[] methodParamTypes = method.getParameterTypes();
                if (methodParamTypes.length == inputParamTypes.length) {
                    boolean allParamsMatch = true;
                    for (int j = 0; j < methodParamTypes.length; j++) {
                        Class<?> methodParamType = methodParamTypes[j];
                        Class<?> inputParamType = inputParamTypes[j];
                        if (methodParamType != inputParamType) {
                            allParamsMatch = false;
                            break;
                        }
                    }
                    if (allParamsMatch) {
                        return method;
                    }
                }
            }
        }
        return null;
    }

    private static @Nullable
    Constructor<?> findConstructor(Class<?> clzz, Class<?> inputParamTypes[]) {
        Constructor<?> constructors[] = clzz.getConstructors();
        for (int i = 0; i < constructors.length; i++) {
            Constructor<?> constructor = constructors[i];
            if (!Modifier.isPublic(constructor.getModifiers())) {
                continue;
            }
            final Class<?>[] methodParamTypes = constructor.getParameterTypes();
            if (methodParamTypes.length == inputParamTypes.length) {
                boolean allParamsMatch = true;
                for (int j = 0; j < methodParamTypes.length; j++) {
                    Class<?> methodParamType = methodParamTypes[j];
                    Class<?> inputParamType = inputParamTypes[j];
                    if (methodParamType != inputParamType) {
                        allParamsMatch = false;
                        break;
                    }
                }
                if (allParamsMatch) {
                    return constructor;
                }
            }
        }
        return null;
    }

    private static @Nullable Object[] convertArgs(@Nullable Object args[], Class<?> argTypes[]) throws Exception {
        Object out[] = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            System.out.println("arg = " + arg);
            if (null == arg) {
                continue;
            }
            Class<?> argType = argTypes[i];
            System.out.println("argType = " + argType);
            if (argType == short.class) {
                argType = Short.class;
            } else if (argType == int.class) {
                argType = Integer.class;
            } else if (argType == long.class) {
                argType = Long.class;
            } else if (argType == float.class) {
                argType = Float.class;
            } else if (argType == double.class) {
                argType = Double.class;
            }
            if (argType.isInstance(arg)) {
                out[i] = arg;
                continue;
            }
            if (arg instanceof String) {
                final String stringArg = (String) arg;
                if (stringArg.equals("null")) {
                    continue;
                }
                Method valueOf = findMethod(argType, true, "valueOf", new Class[]{String.class});
                System.out.println("valueOf = " + valueOf);
                if (null != valueOf) {
                    final Object value = valueOf.invoke(stringArg);
                    if (null != value) {
                        out[i] = value;
                    }
                    System.out.println("out[i] = " + out[i]);
                    continue;
                }
                Constructor<?> constructor = findConstructor(argType, new Class[]{String.class});
                System.out.println("constructor = " + constructor);
                if (null != constructor) {
                    out[i] = constructor.newInstance(stringArg);
                    System.out.println("out[i] = " + out[i]);
                    continue;
                }
            }
        }
        return out;
    }

    public @Nullable
    Scriptable<?> function(String name, @Nullable Object args[], PrintWriter pw) {
        try {
            final ScriptableFunction<T> func = functions.get(name);
            if (null != func) {
                final Class<?>[] argTypes = func.getArgTypes();
                if (argTypes.length != args.length) {
                    pw.print("Function named " + name + " requires " + argTypes.length + " arguments not " + args.length + "\r\n");
                    pw.print("        " + name + "(" + Arrays.toString(argTypes) + ")\r\n");
                    return null;
                } else {
                    @Nullable Object newargs[] = convertArgs(args, argTypes);
                    return func.applyFunction(obj, newargs, pw);
                }
            } else {
                pw.print("No function named " + name + "\r\n");
                List<String> functionList = new ArrayList<>(functions.keySet());
                Collections.sort(functionList);
                pw.print("functions =  " + functionList + "\r\n");
                return null;
            }
        } catch (Exception ex) {
            pw.print("Exception occured for action " + name + " args = " + Arrays.toString(args) + ":\r\n");
            pw.print(ex + "r\n");
            Logger.getLogger(LauncherAprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public String toString() {
        if (null == obj) {
            return "static " + tclzz;
        } else if (tclzz.isArray()) {
            return tclzz.getComponentType() + "[] " + Arrays.toString((Object[]) obj);
        } else if (obj instanceof PoseType) {
            return tclzz + " " + CRCLPosemath.poseToString((PoseType) obj);
        } else if (obj instanceof PointType) {
            return tclzz + " " + CRCLPosemath.pointToString((PointType) obj);
        } else if (obj instanceof PoseToleranceType) {
            return tclzz + " " + CRCLPosemath.toString((PoseToleranceType) obj);
        } else {
            return tclzz + " " + obj;
        }
    }

    public boolean hasAction(String name) {
        return actions.containsKey(name);
    }

    public boolean hasFunction(String name) {
        return functions.containsKey(name);
    }

    public String toVerboseString() {
        final Set<String> actionsKeySet = actions.keySet();
        List<String> actionsNameList = new ArrayList<>(actionsKeySet);
        Collections.sort(actionsNameList);
        final Set<String> functionsKeySet = functions.keySet();
        List<String> functionsNameList = new ArrayList<>(functionsKeySet);
        Collections.sort(functionsNameList);
        StringBuilder sb = new StringBuilder();
        sb
                = sb
                        .append("Scriptable{\r\n tclzz=")
                        .append(tclzz)
                        .append(",\r\n obj=")
                        .append(obj)
                        .append(",\r\n actions={");
        for (String name : actionsNameList) {
            final ScriptableAction<T> action = actions.get(name);
            if (null != action) {
                sb = sb
                        .append("\r\n        ")
                        .append(name)
                        .append("(")
                        .append(Arrays.toString(action.getArgTypes()))
                        .append(")");
            } else {
                sb = sb
                        .append("    ERROR: actions.get(")
                        .append(name)
                        .append(") returned null");
            }
        }
        sb = sb
                .append("\r\n    },\r\n functions={");
        for (String name : functionsNameList) {
            final ScriptableFunction<T> function = functions.get(name);
            if (null != function) {
                sb = sb
                    .append("\r\n        ")
                    .append(name)
                    .append("(")
                    .append(Arrays.toString(function.getArgTypes()))
                    .append(")");
            } else {
                sb = sb
                        .append("    ERROR: functions.get(")
                        .append(name)
                        .append(") returned null");
            }
        }
        sb = sb
                .append("\r\n    }\r\n}");
        return sb.toString();
    }

}
