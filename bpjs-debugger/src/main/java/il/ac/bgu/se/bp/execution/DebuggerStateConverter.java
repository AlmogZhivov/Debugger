package il.ac.bgu.se.bp.execution;

import il.ac.bgu.se.bp.rest.response.*;
import il.ac.bgu.se.bp.socket.state.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts internal debugger state objects to serialization-safe DTOs.
 * This prevents circular reference issues during Gson serialization.
 */
public class DebuggerStateConverter {
    
    /**
     * Converts BPDebuggerState to DebuggerStateDTO
     */
    public static DebuggerStateDTO toDTO(BPDebuggerState state) {
        if (state == null) {
            return new DebuggerStateDTO();
        }
        
        DebuggerStateDTO dto = new DebuggerStateDTO();
        
        // Copy basic fields
        dto.setCurrentRunningBT(state.getCurrentRunningBT());
        dto.setCurrentLineNumber(state.getCurrentLineNumber());
        
        // Convert b-thread info list
        if (state.getbThreadInfoList() != null) {
            List<BThreadInfoDTO> bThreadDTOs = state.getbThreadInfoList().stream()
                .map(DebuggerStateConverter::toDTO)
                .collect(Collectors.toList());
            dto.setbThreadInfoList(bThreadDTOs);
        }
        
        // Convert events status
        if (state.getEventsStatus() != null) {
            dto.setEventsStatus(toDTO(state.getEventsStatus()));
        }
        
        // Convert global environment (already String->String map)
        if (state.getGlobalEnv() != null) {
            dto.setGlobalEnv(new HashMap<>(state.getGlobalEnv()));
        }
        
        // Convert events history (simplify to Long->String map)
        if (state.getEventsHistory() != null) {
            Map<Long, String> eventsHistoryDTO = new HashMap<>();
            for (Map.Entry<Long, EventInfo> entry : state.getEventsHistory().entrySet()) {
                eventsHistoryDTO.put(entry.getKey(), entry.getValue().getName());
            }
            dto.setEventsHistory(eventsHistoryDTO);
        }
        
        // Convert breakpoints
        if (state.getBreakpoints() != null) {
            dto.setBreakpoints(state.getBreakpoints());
        }
        
        // Convert debugger configs
        if (state.getDebuggerConfigs() != null) {
            dto.setDebuggerConfigs(toDTO(state.getDebuggerConfigs()));
        }
        
        return dto;
    }
    
    /**
     * Converts BThreadInfo to BThreadInfoDTO
     */
    public static BThreadInfoDTO toDTO(BThreadInfo bThreadInfo) {
        if (bThreadInfo == null) {
            return new BThreadInfoDTO();
        }
        
        BThreadInfoDTO dto = new BThreadInfoDTO();
        dto.setName(bThreadInfo.getName());
        
        // Convert environment
        if (bThreadInfo.getEnv() != null) {
            Map<Integer, BThreadScopeDTO> envDTO = new HashMap<>();
            for (Map.Entry<Integer, BThreadScope> entry : bThreadInfo.getEnv().entrySet()) {
                envDTO.put(entry.getKey(), toDTO(entry.getValue()));
            }
            dto.setEnv(envDTO);
        }
        
        // Convert event sets
        if (bThreadInfo.getRequested() != null) {
            Set<EventInfoDTO> requestedDTO = bThreadInfo.getRequested().stream()
                .map(DebuggerStateConverter::toDTO)
                .collect(Collectors.toSet());
            dto.setRequested(requestedDTO);
        }
        
        if (bThreadInfo.getBlocked() != null) {
            Set<EventInfoDTO> blockedDTO = bThreadInfo.getBlocked().stream()
                .map(DebuggerStateConverter::toDTO)
                .collect(Collectors.toSet());
            dto.setBlocked(blockedDTO);
        }
        
        if (bThreadInfo.getWait() != null) {
            Set<EventInfoDTO> waitDTO = bThreadInfo.getWait().stream()
                .map(DebuggerStateConverter::toDTO)
                .collect(Collectors.toSet());
            dto.setWait(waitDTO);
        }
        
        return dto;
    }
    
    /**
     * Converts BThreadScope to BThreadScopeDTO
     */
    public static BThreadScopeDTO toDTO(BThreadScope scope) {
        if (scope == null) {
            return new BThreadScopeDTO();
        }
        
        BThreadScopeDTO dto = new BThreadScopeDTO();
        dto.setScopeName(scope.getScopeName());
        dto.setCurrentLineNumber(scope.getCurrentLineNumber());
        
        // Convert variables (simplify to String->String map)
        if (scope.getVariables() != null) {
            Map<String, String> variablesDTO = new HashMap<>();
            for (Map.Entry<String, String> entry : scope.getVariables().entrySet()) {
                variablesDTO.put(entry.getKey(), entry.getValue());
            }
            dto.setVariables(variablesDTO);
        }
        
        return dto;
    }
    
    /**
     * Converts EventInfo to EventInfoDTO
     */
    public static EventInfoDTO toDTO(EventInfo eventInfo) {
        if (eventInfo == null) {
            return new EventInfoDTO();
        }
        
        return new EventInfoDTO(eventInfo.getName());
    }
    
    /**
     * Converts EventsStatus to EventsStatusDTO
     */
    public static EventsStatusDTO toDTO(EventsStatus eventsStatus) {
        if (eventsStatus == null) {
            return new EventsStatusDTO();
        }
        
        EventsStatusDTO dto = new EventsStatusDTO();
        
        if (eventsStatus.getRequested() != null) {
            Set<EventInfoDTO> requestedDTO = eventsStatus.getRequested().stream()
                .map(DebuggerStateConverter::toDTO)
                .collect(Collectors.toSet());
            dto.setRequested(requestedDTO);
        }
        
        if (eventsStatus.getBlocked() != null) {
            Set<EventInfoDTO> blockedDTO = eventsStatus.getBlocked().stream()
                .map(DebuggerStateConverter::toDTO)
                .collect(Collectors.toSet());
            dto.setBlocked(blockedDTO);
        }
        
        if (eventsStatus.getWait() != null) {
            Set<EventInfoDTO> waitDTO = eventsStatus.getWait().stream()
                .map(DebuggerStateConverter::toDTO)
                .collect(Collectors.toSet());
            dto.setWait(waitDTO);
        }
        
        return dto;
    }
    
    /**
     * Converts DebuggerConfigs to DebuggerConfigsDTO
     */
    public static DebuggerConfigsDTO toDTO(DebuggerConfigs configs) {
        if (configs == null) {
            return new DebuggerConfigsDTO();
        }
        
        DebuggerConfigsDTO dto = new DebuggerConfigsDTO();
        dto.setSkipBreakpoints(configs.isToggleMuteBreakPoint());
        dto.setSkipSyncPoints(configs.isToggleMuteSyncPoints());
        dto.setWaitForExternalEvents(configs.isToggleWaitForExternalEvents());
        
        return dto;
    }
}
