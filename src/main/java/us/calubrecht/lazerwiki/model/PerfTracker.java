package us.calubrecht.lazerwiki.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

public class PerfTracker {
    private Map<String, Long> timers = new HashMap<>();

    @JsonIgnore
    private Map<String, Long> timerStarts = new HashMap<>();

    public void startTimer(String name) {
        timerStarts.put(name, System.currentTimeMillis());
    }

    public void stopTimer(String name) {
        if (timerStarts.containsKey(name)) {
            long start = timerStarts.get(name);
            timerStarts.remove(name);
            timers.put(name, System.currentTimeMillis() - start + timers.getOrDefault(name, 0L));
        }
    }

    public void stopAll() {
        new HashSet<>(timerStarts.keySet()).forEach(this::stopTimer);
    }

    public void clearTimer(String name) {
       timers.remove(name);
       timerStarts.remove(name);
    }

    public void assignTime(String name, long time) {
        timers.put(name, time);
        timerStarts.remove(name);
    }

    public Map<String, Double> getTimers() {
        return timers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()/1000.0));
    }
}
