package il.ac.bgu.se.bp.utils;

import il.ac.bgu.se.bp.socket.state.COBPContext;
import il.ac.bgu.se.bp.utils.logger.Logger;
import org.mozilla.javascript.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class COBPContextHelper {
    private static final Logger logger = new Logger(COBPContextHelper.class);

    public static COBPContext extractCOBPContext(Context jsContext, String currentBThreadName) {
        try {
            // For now, we'll use the simple source code analysis approach
            // This avoids the complexity of trying to access COBP objects from the Rhino context
            
            logger.info("Using simple COBP context extraction for b-thread: {0}", currentBThreadName);
            
            // Create a simple context with just the b-thread name as context
            // This will be enhanced later when we have access to the source code
            return new COBPContext(
                new ArrayList<>(), // No entities for now
                new HashMap<>(),   // No query results for now
                currentBThreadName, // Use b-thread name as context for now
                null              // No b-thread bound context for now
            );

        } catch (Exception e) {
            logger.error("Failed to extract COBP context: {0}", e, e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract COBP context from source code (static method for use without Rhino context)
     */
    public static COBPContext extractCOBPContextFromSource(String sourceCode, String currentBThreadName) {
        return SimpleCOBPContextExtractor.extractContext(sourceCode, currentBThreadName);
    }
    
    /**
     * Extract COBP context using runtime support (when available)
     */
    public static COBPContext extractCOBPContextFromRuntime(COBPRuntimeSupport runtimeSupport, String currentBThreadName) {
        if (runtimeSupport == null) {
            return null;
        }
        
        if (currentBThreadName == null) {
            logger.info("Current b-thread name is null, will try to extract any available COBP context from runtime");
        }
        
        try {
            Map<String, String> bThreadContexts = runtimeSupport.getBThreadContexts();
            String currentContext = null;
            
            if (currentBThreadName != null) {
                currentContext = bThreadContexts.get(currentBThreadName);
                logger.info("Extracted COBP context from runtime: b-thread={0}, context={1}", currentBThreadName, currentContext);
            }
            
            logger.info("Available b-thread contexts: {0}", bThreadContexts);
            
            // If we don't have context for the current b-thread, try to get any available context
            if (currentContext == null && !bThreadContexts.isEmpty()) {
                // Get the first available context
                String firstBThreadName = bThreadContexts.keySet().iterator().next();
                currentContext = bThreadContexts.get(firstBThreadName);
                logger.info("Using context from first available b-thread: {0} -> {1}", firstBThreadName, currentContext);
            }
            
            return new COBPContext(
                new ArrayList<>(), // No entities for now
                new HashMap<>(),   // No query results for now
                currentContext,    // Current b-thread context from runtime
                null              // No b-thread bound context for now
            );
            
        } catch (Exception e) {
            logger.error("Failed to extract COBP context from runtime: {0}", e, e.getMessage());
            return null;
        }
    }


}
