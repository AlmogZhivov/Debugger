package il.ac.bgu.se.bp.debugger.engine;

import il.ac.bgu.se.bp.socket.state.*;

import java.util.*;

public class ExpectedResults {
    public static BPDebuggerState testEnvChangedInBreakPoints_ENV1(){
        List<BThreadInfo> bThreadInfoList = new ArrayList<>();
        Map<Integer, BThreadScope> bt1Env =  new HashMap<>();
        HashMap<String, String> env = new HashMap<>();
        env.put("myvar1", "null");
        env.put("z", "null");
        bt1Env.put(0, new BThreadScope("BTMain", "2", env));
        Map<Integer, BThreadScope> bt2Env =  new HashMap<>();
        bThreadInfoList.add(new BThreadInfo("bt-test-1", bt1Env));
        bThreadInfoList.add(new BThreadInfo("bt-test-2", bt2Env));
        return new BPDebuggerState(bThreadInfoList, new EventsStatus(), null, "bt-test-1", 2);
    }
    public static BPDebuggerState testEnvChangedInBreakPoints_ENV2(){
        List<BThreadInfo> bThreadInfoList = new ArrayList<>();
        Map<Integer, BThreadScope> bt1Env =  new HashMap<>();
        HashMap<String, String> env = new HashMap<>();
        env.put("myvar1", "10.0");
        env.put("z", "null");
        bt1Env.put(0, new BThreadScope("BTMain", "3", env));
        Map<Integer, BThreadScope> bt2Env =  new HashMap<>();
        bThreadInfoList.add(new BThreadInfo("bt-test-1", bt1Env));
        bThreadInfoList.add(new BThreadInfo("bt-test-2", bt2Env));
        return new BPDebuggerState(bThreadInfoList, new EventsStatus(), null,"bt-test-1", 3);
    }
    public static BPDebuggerState testEnvChangedInBreakPoints_ENV3(){
        List<BThreadInfo> bThreadInfoList = new ArrayList<>();
        Map<Integer, BThreadScope> bt1Env =  new HashMap<>();
        HashMap<String, String> env1 = new HashMap<>();
        env1.put("myvar1", "10.0");
        env1.put("z", "null");
        bt1Env.put(0, new BThreadScope("BTMain", "3", env1));
        Map<Integer, BThreadScope> bt2Env =  new HashMap<>();
        HashMap<String, String> env2 = new HashMap<>();
        env2.put("myvar2", "10.0");
        env2.put("z", "null");
        bt2Env.put(0, new BThreadScope("BTMain", "25", env2));
        Set<EventInfo> requested1= new HashSet<>(), requested2= new HashSet<>(), requested = new HashSet<>();
        requested1.add(new EventInfo("bt-1-event-1"));
        bThreadInfoList.add(new BThreadInfo("bt-test-1", bt1Env, new HashSet<>(), new HashSet<>(), requested1));
        requested2.add(new EventInfo("bt-2-event-1"));
        requested.addAll(requested1);
        requested.addAll(requested2);
        bThreadInfoList.add(new BThreadInfo("bt-test-2", bt2Env, new HashSet<>(), new HashSet<>(), requested2));
        List<EventInfo> emptyEvents = new ArrayList<>();
        return new BPDebuggerState(bThreadInfoList, new EventsStatus(emptyEvents, emptyEvents, requested, new LinkedList<>()));
    }
}
