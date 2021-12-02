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
package aprs.actions.executor;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 *
 * @author shackle
 */
public interface ExecutorOption {

    public static <K extends Enum & ExecutorOption, V> Map<K, V> map(
            Class<K> keyClass,
            Class<V> valueClass,
            Map.Entry<? extends ExecutorOption, ?>... optionpairs) {
        Map<K, V> map = new EnumMap(keyClass);
        for (int i = 0; i < optionpairs.length; i++) {
            Map.Entry<?, ?> optionpair = optionpairs[i];
            if (keyClass.isInstance(optionpair.getKey())) {
                if (!valueClass.isInstance(optionpair.getValue())) {
                    throw new RuntimeException("MisMatched value type for " + optionpair);
                }
                map.put(keyClass.cast(optionpair.getKey()), valueClass.cast(optionpair.getValue()));
            }
        }
        return map;
    }

    public static <K extends Enum & ExecutorOption, V> Map<K, V> map(
            Class<K> keyClass,
            Class<V> valueClass,
            Collection<Map.Entry<? extends ExecutorOption, ?>> optionpairs) {
        Map<K, V> map = new EnumMap(keyClass);
        for (Map.Entry<?, ?> optionpair : optionpairs) {
            if (keyClass.isInstance(optionpair.getKey())) {
                if (!valueClass.isInstance(optionpair.getValue())) {
                    throw new RuntimeException("MisMatched value type for " + optionpair);
                }
                map.put(keyClass.cast(optionpair.getKey()), valueClass.cast(optionpair.getValue()));
            }
        }
        return map;
    }

    public static <K extends Enum & ExecutorOption, V> Map<K, V> map(
            Class<K> keyClass,
            Class<V> valueClass,
            Map<? extends ExecutorOption, ?> optionpairs) {
        Map<K, V> map = new EnumMap(keyClass);
        for (Map.Entry<?, ?> optionpair : optionpairs.entrySet()) {
            if (keyClass.isInstance(optionpair.getKey())) {
                if (!valueClass.isInstance(optionpair.getValue())) {
                    throw new RuntimeException("MisMatched value type for " + optionpair);
                }
                map.put(keyClass.cast(optionpair.getKey()), valueClass.cast(optionpair.getValue()));
            }
        }
        return map;
    }

    static class MapMaker {

        private final static Map<String, ExecutorOption> map = makeMap();

        public static Map<String, ExecutorOption> getMap() {
            return map;
        }

        private static Map<String, ExecutorOption> makeMap() {
            Map<String, ExecutorOption> map = new TreeMap<>();
            for (ForBoolean ofb : ForBoolean.values()) {
                if (map.containsKey(ofb.name())) {
                    throw new RuntimeException(ofb.name() + " repeated");
                }
                map.put(ofb.name(), ofb);
            }
            for (ForDouble ofd : ForDouble.values()) {
                if (map.containsKey(ofd.name())) {
                    throw new RuntimeException(ofd.name() + " repeated");
                }
                map.put(ofd.name(), ofd);
            }
            for (ForString ofs : ForString.values()) {
                if (map.containsKey(ofs.name())) {
                    throw new RuntimeException(ofs.name() + " repeated");
                }
                map.put(ofs.name(), ofs);
            }
            for (ForInt ofi : ForInt.values()) {
                if (map.containsKey(ofi.name())) {
                    throw new RuntimeException(ofi.name() + " repeated");
                }
                map.put(ofi.name(), ofi);
            }
            return map;
        }

        public static String toCamel(String name) {
            StringTokenizer tokenizer = new StringTokenizer(name, "_", false);
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            while (tokenizer.hasMoreTokens()) {
                String tok = tokenizer.nextToken();
                if (first) {
                    sb.append(tok.toLowerCase());
                    first = false;
                } else {
                    sb.append(tok.substring(0, 1).toUpperCase());
                    sb.append(tok.substring(1).toLowerCase());
                }
            }
            return sb.toString();
        }
        
         public static String toConstantForm(String name) {
            StringBuilder sb = new StringBuilder();
            boolean lastCharacterWasUpperCase = true;
             for (int i = 0; i < name.length(); i++) {
                 char c = name.charAt(i);
                 if(Character.isUpperCase(c)) {
                     if(!lastCharacterWasUpperCase) {
                        sb.append('_');
                     }
                     lastCharacterWasUpperCase = true;
                 }
                 sb.append(Character.toUpperCase(c));
             }
            return sb.toString();
        }
    }

    public static ExecutorOption[] values() {
        return MapMaker.getMap().values().toArray(new ExecutorOption[0]);
    }

    public static ExecutorOption of(String name) {
        ExecutorOption opt = MapMaker.getMap().get(name);
        if (null != opt) {
            return opt;
        }
        String uname = name.toUpperCase();
        ExecutorOption optu = MapMaker.getMap().get(uname);
        if (null != optu) {
            return optu;
        }
        String lname = name.toLowerCase();
        ExecutorOption optl = MapMaker.getMap().get(lname);
        if (null != optl) {
            return optl;
        }
        String camelName = MapMaker.toCamel(name);
        ExecutorOption optCamel = MapMaker.getMap().get(camelName);
        if (null != optCamel) {
            return optCamel;
        }
        String constName = MapMaker.toConstantForm(name);
        ExecutorOption optConst = MapMaker.getMap().get(constName);
        if (null != optConst) {
            return optConst;
        }
        throw new IllegalArgumentException(
                "No ExecutorOption named " + name);
    }

    static public class WithValue<K extends Enum & ExecutorOption, V> implements Map.Entry<K, V> {

        protected final K key;
        protected V value;

        public WithValue(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            this.value = value;
            return value;
        }

    }

    static public enum ForBoolean implements ExecutorOption {
        REVERSE,
        FORCE_NAME_CHANGE,
        takeSnapshots,
        pauseInsteadOfRecover,
        doInspectKit,
        requireNewPoses,
        skipMissingParts,
        useJointMovesForToolHolderApproach,
        useJointLookFor,
        saveProgramRunData,
        enableOptaPlanner,
        useEndPoseTolerance,
        useMessageCommands,
        INVALID_BOOL_OPT;

        public WithValue<ForBoolean, Boolean> with(boolean arg) {
            return new WithValue(this, arg);
        }

        public static Map<ForBoolean, Boolean> map(Map.Entry<? extends ExecutorOption, ?>... optionpairs) {
            return ExecutorOption.map(ForBoolean.class, Boolean.class, optionpairs);
        }

        public static Map<ForBoolean, Boolean> map(Collection<Map.Entry<? extends ExecutorOption, ?>> optionpairs) {
            return ExecutorOption.map(ForBoolean.class, Boolean.class, optionpairs);
        }

        public static Map<ForBoolean, Boolean> map(Map<? extends ExecutorOption, ?> optionpairs) {
            return ExecutorOption.map(ForBoolean.class, Boolean.class, optionpairs);
        }
    }

    static public enum ForDouble implements ExecutorOption {
        approachZOffset,
        approachToolChangerZOffset,
        placeZOffset,
        takeZOffset,
        joint0DiffTolerance,
        settleDwellTime,
        toolChangerDwellTime,
        lookDwellTime,
        skipLookDwellTime,
        afterMoveToLookForDwellTime,
        firstLookDwellTime,
        lastLookDwellTime,
        fastTransSpeed,
        testTransSpeed,
        rotSpeed,
        jointSpeed,
        jointAccel,
        kitInspectDistThreshold,
        slowTransSpeed,
        verySlowTransSpeed,
        endPoseXPointTolerance,
        endPoseYPointTolerance,
        endPoseZPointTolerance,
        endPoseXAxisTolerance,
        endPoseZAxisTolerance,
        INVALID_DOUBLE_OPT;

        public WithValue<ForDouble, Double> with(double arg) {
            return new WithValue(this, arg);
        }

        public static Map<ForDouble, Double> map(Map.Entry<? extends ExecutorOption, ?>... optionpairs) {
            return ExecutorOption.map(ForDouble.class, Double.class, optionpairs);
        }

        public static Map<ForDouble, Double> map(Collection<Map.Entry<? extends ExecutorOption, ?>> optionpairs) {
            return ExecutorOption.map(ForDouble.class, Double.class, optionpairs);
        }

        public static Map<ForDouble, Double> map(Map<? extends ExecutorOption, ?> optionpairs) {
            return ExecutorOption.map(ForDouble.class, Double.class, optionpairs);
        }
    }

    static public enum ForString implements ExecutorOption {
        RPY,
        recordedPositionsFile,
        toolChangerPoseFile,
        recordedPoses,
        partToolFile,
        lookForXYZ,
        lookForJoints,
        jointTolerances,
        INVALID_STRING_OPT;

        public WithValue<ForString, String> with(String arg) {
            return new WithValue(this, arg);
        }

        public static Map<ForString, String> map(Map.Entry<? extends ExecutorOption, ?>... optionpairs) {
            return ExecutorOption.map(ForString.class, String.class, optionpairs);
        }

        public static Map<ForString, String> map(Collection<Map.Entry<? extends ExecutorOption, ?>> optionpairs) {
            return ExecutorOption.map(ForString.class, String.class, optionpairs);
        }

        public static Map<ForString, String> map(Map<? extends ExecutorOption, ?> optionpairs) {
            return ExecutorOption.map(ForString.class, String.class, optionpairs);
        }
    }

    static public enum ForInt implements ExecutorOption {
        takePartArgIndex,
        placePartSlotArgIndex,
        visionCycleNewDiffThreshold,
        INVALID_INT_OPT;

        public WithValue<ForString, String> with(int arg) {
            return new WithValue(this, arg);
        }

        public static Map<ForInt, Integer> map(Map.Entry<? extends ExecutorOption, ?>... optionpairs) {
            return ExecutorOption.map(ForInt.class, Integer.class, optionpairs);
        }

        public static Map<ForInt, Integer> map(Collection<Map.Entry<? extends ExecutorOption, ?>> optionpairs) {
            return ExecutorOption.map(ForInt.class, Integer.class, optionpairs);
        }

        public static Map<ForInt, Integer> map(Map<? extends ExecutorOption, ?> optionpairs) {
            return ExecutorOption.map(ForInt.class, Integer.class, optionpairs);
        }
    }

}
