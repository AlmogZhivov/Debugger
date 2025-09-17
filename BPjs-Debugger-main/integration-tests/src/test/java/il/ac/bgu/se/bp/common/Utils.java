package il.ac.bgu.se.bp.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import il.ac.bgu.se.bp.socket.state.BThreadInfo;
import il.ac.bgu.se.bp.socket.state.BThreadScope;
import il.ac.bgu.se.bp.socket.state.EventInfo;
import il.ac.bgu.se.bp.utils.Pair;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;

public class Utils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static boolean isNull(String str) {
        return str.equals("null");
    }

    public static BThreadScope getLastEnvOfMatchingBThread(List<BThreadInfo> bThreadInfoList, List<String> bThreads) {
        for (String bThread : bThreads) {
            BThreadScope envByBThread = getLastEnvOfBThread(bThreadInfoList, bThread);
            if (envByBThread != null) {
                return envByBThread;
            }
        }
        return null;
    }

    private static BThreadScope getLastEnvOfBThread(List<BThreadInfo> bThreadInfoList, String bThread) {
        for (BThreadInfo bThreadInfo : bThreadInfoList) {
            if (bThreadInfo.getName().equals(bThread))
                return bThreadInfo.getEnv().get(0);
        }
        return null;
    }

    public static List<String> getBThreadNamesByBreakpoint(String bThreads, int currentLineNumber) {
        return getBThreadsNamesByBreakpoints(bThreads).get(currentLineNumber);
    }

    public static BThreadInfo getBThreadInfoByName(List<BThreadInfo> bThreadInfoList, String bThread) {
        for (BThreadInfo bThreadInfo : bThreadInfoList) {
            if (bThreadInfo.getName().equals(bThread)) {
                return bThreadInfo;
            }
        }
        return null;
    }

    private static Map<Integer, List<String>> getBThreadsNamesByBreakpoints(String bThreads) {
        Map<Integer, List<String>> bThreadsByBreakpoint = new HashMap<>();
        String[] splatBThreadsPerBreakpoint = bThreads.split("[{}]");
        for (String bThreadsPerBreakpoint : splatBThreadsPerBreakpoint) {
            if (StringUtils.isEmpty(bThreadsPerBreakpoint))
                continue;
            String[] breakpointAndBThreads = bThreadsPerBreakpoint.split(":");
            bThreadsByBreakpoint.put(strToInt(breakpointAndBThreads[0]), strToStringList(breakpointAndBThreads[1]));
        }
        return bThreadsByBreakpoint;
    }

    public static Map<Integer, List<Pair<String, String>>> createStringEnvByBreakpoints(String vars) {
        Map<Integer, List<Pair<String, String>>> varsByBreakpointsMap = new HashMap<>();
        String[] splatVarsPerBreakpoint = vars.split("[{}]");
        for (String env : splatVarsPerBreakpoint) {
            if (StringUtils.isEmpty(env))
                continue;
            String[] splatEnv = env.split(":");
            varsByBreakpointsMap.put(strToInt(splatEnv[0]), strToStringVarsList(splatEnv[1]));
        }
        return varsByBreakpointsMap;
    }

    public static List<Map<String, String>> createEnvsMappings(String envsStr) {
        try {
            String envsStrWithQuotes = envsStr.replaceAll("([\\w.]+)", "\"$1\"");
            return objectMapper.readValue(envsStrWithQuotes, List.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<Pair<String, String>> strToStringVarsList(String stringVars) {
        return StringUtils.isEmpty(stringVars) ? new LinkedList<>() :
                Arrays.stream(stringVars.split(",")).map(strVar -> Pair.of(getVar(strVar), getStrVal(strVar))).collect(Collectors.toList());
    }

    public static Map<Integer, List<Pair<String, Double>>> createDoubleEnvByBreakpoints(String vars) {
        Map<Integer, List<Pair<String, Double>>> varsByBreakpointsMap = new HashMap<>();
        String[] splatVarsPerBreakpoint = vars.split("[{}]");
        for (String env : splatVarsPerBreakpoint) {
            if (StringUtils.isEmpty(env))
                continue;
            String[] splatEnv = env.split(":");
            varsByBreakpointsMap.put(strToInt(splatEnv[0]), strToDoubleVarsList(splatEnv[1]));
        }
        return varsByBreakpointsMap;
    }

    private static List<Pair<String, Double>> strToDoubleVarsList(String doubleVars) {
        return StringUtils.isEmpty(doubleVars) ? new LinkedList<>() :
                Arrays.stream(doubleVars.split(",")).map(strVar -> Pair.of(getVar(strVar), getDoubleVal(strVar))).collect(Collectors.toList());
    }

    private static String getVar(String str) {
        return splitAndGetBy(str, "=", 0);
    }

    private static String getStrVal(String str) {
        return splitAndGetBy(str, "=", 1);
    }

    public static Double getDoubleVal(String str) {
        return strToDouble(splitAndGetBy(str, "=", 1));
    }

    public static String splitAndGetBy(String str, String regex, int i) {
        return str.split(regex)[i];
    }

    public static boolean strToBoolean(String str) {
        return Boolean.parseBoolean(str);
    }

    public static String strToString(String str) {
        str = str.charAt(0) == '\"' ? str.substring(1) : str;
        str = str.charAt(str.length() - 1) == '\"' ? str.substring(0, str.length() -1) : str;
        return cleanString(str);
    }

    public static Double strToDouble(String str) {
        return Double.parseDouble(str);
    }

    public static int strToInt(String str) {
        return Integer.parseInt(str);
    }

    public static List<String> strToStringList(String strings) {
        strings = cleanString(strings);
        if (strings.isEmpty()) {
            return new LinkedList<>();
        }

        if (strings.charAt(0) == '[' && strings.charAt(strings.length() -1) == ']') {
            strings = strings.substring(1, strings.length() - 1);
        }
        return Arrays.stream(strings.split(",")).collect(Collectors.toList());
    }

    public static List<Double> strToDoubleList(String doubles) {
        return Arrays.stream(doubles.split(",")).map(Double::parseDouble).collect(Collectors.toList());
    }

    public static List<Integer> strToIntList(String breakpoints) {
        return cleanString(breakpoints).isEmpty() ? new LinkedList<>() :
                Arrays.stream(breakpoints.split(",")).map(Integer::parseInt).collect(Collectors.toList());
    }

    public static void waitUntilPredicateSatisfied(Callable<Boolean> predicate, int timeToSleep, int maxToTry) {
        try {
            for (int i = 0; i < maxToTry; i++) {
                if (predicate.call()) {
                    return;
                }
                sleep(timeToSleep);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        fail("predicate was not satisfied after " + maxToTry + " retries");
    }

    public static void sleep(int timeToSleep) {
        try {
            double secToSleep = ((double) timeToSleep) / 1000.0;
            System.out.println("sleeping " + secToSleep + " sec");
            Thread.sleep(timeToSleep);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String cleanString(String string) {
        return StringUtils.isEmpty(string) ? "" : string.replace("[blank]", "");
    }

    public static List<BThreadInfo> strToBThreadInfo(String bThreadInfoListStr) {
        List<BThreadInfo> bThreadInfoList = new LinkedList<>();
        bThreadInfoListStr = cleanString(bThreadInfoListStr);
        String[] splatBThreadInfoListStr = bThreadInfoListStr.split("[{}]");
        for (String bThreadInfoStr : splatBThreadInfoListStr) {
            if (StringUtils.isEmpty(bThreadInfoStr))
                continue;
            String[] bThreadInfoDataArray = bThreadInfoStr.split(",");
            HashMap<String, String> bThreadInfoDataMap = new HashMap<>();
            for (String bThreadInfoData : bThreadInfoDataArray) {
                String[] keyAndValue = bThreadInfoData.split(":");
                bThreadInfoDataMap.put(keyAndValue[0], keyAndValue[1]);
            }
            BThreadInfo bThreadInfo = new BThreadInfo(bThreadInfoDataMap.get("name"), null,
                    toEventInfoList(strToStringList(bThreadInfoDataMap.get("wait"))),
                    toEventInfoList(strToStringList(bThreadInfoDataMap.get("blocked"))),
                    toEventInfoList(strToStringList(bThreadInfoDataMap.get("requested"))));
            bThreadInfoList.add(bThreadInfo);
        }
        return bThreadInfoList;
    }

    private static Set<EventInfo> toEventInfoList(List<String> requested) {
        return requested.stream().map(EventInfo::new).collect(Collectors.toSet());
    }

    private static EventInfo toEventInfo(String name) {
        return name == null ? null : new EventInfo(name);
    }
}
