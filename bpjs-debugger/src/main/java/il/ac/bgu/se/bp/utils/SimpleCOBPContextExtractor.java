package il.ac.bgu.se.bp.utils;

import il.ac.bgu.se.bp.socket.state.COBPContext;
import il.ac.bgu.se.bp.socket.state.COBPEntity;
import il.ac.bgu.se.bp.utils.logger.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple COBP context extractor that analyzes source code to extract b-thread context information
 * without trying to execute COBP code directly.
 */
public class SimpleCOBPContextExtractor {
    private static final Logger logger = new Logger(SimpleCOBPContextExtractor.class);
    
    // Pattern to match ctx.bthread('name', 'context', function)
    private static final Pattern BTHREAD_PATTERN = Pattern.compile(
        "ctx\\.bthread\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*,\\s*['\"]([^'\"]+)['\"]\\s*,\\s*function"
    );
    
    // Pattern to match bthread('name', function) - regular bthreads
    private static final Pattern REGULAR_BTHREAD_PATTERN = Pattern.compile(
        "bthread\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*,\\s*function"
    );
    
    /**
     * Extract COBP context information from source code
     * @param sourceCode the COBP source code
     * @param currentBThreadName the currently running b-thread name
     * @return COBPContext with b-thread context information
     */
    public static COBPContext extractContext(String sourceCode, String currentBThreadName) {
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Check if this looks like COBP code
            if (!isCOBPCode(sourceCode)) {
                return null;
            }
            
            logger.info("Extracting COBP context from source code");
            
            // Extract b-thread context mappings
            Map<String, String> bThreadContexts = extractBThreadContexts(sourceCode);
            
            // Get the context for the current b-thread
            String currentContext = bThreadContexts.get(currentBThreadName);
            
            // If we don't have context for the current b-thread, try to get any available context
            if (currentContext == null && !bThreadContexts.isEmpty()) {
                // Get the first available context
                String firstBThreadName = bThreadContexts.keySet().iterator().next();
                currentContext = bThreadContexts.get(firstBThreadName);
                logger.info("Using context from first available b-thread: {0} -> {1}", firstBThreadName, currentContext);
            }
            
            logger.info("Current b-thread: {0}, Context: {1}", currentBThreadName, currentContext);
            
            // Create a simple COBP context with just the b-thread context information
            return new COBPContext(
                new ArrayList<>(), // No entities for now
                new HashMap<>(),   // No query results for now
                currentContext,    // Current b-thread context
                null              // No b-thread bound context for now
            );
            
        } catch (Exception e) {
            logger.error("Failed to extract COBP context: {0}", e, e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if the source code contains COBP patterns
     */
    private static boolean isCOBPCode(String sourceCode) {
        return sourceCode.contains("ctx.bthread") || 
               sourceCode.contains("ctx.Entity") || 
               sourceCode.contains("ctx.populateContext") ||
               sourceCode.contains("ctx.registerQuery");
    }
    
    /**
     * Extract b-thread name to context mappings from source code
     */
    private static Map<String, String> extractBThreadContexts(String sourceCode) {
        Map<String, String> bThreadContexts = new HashMap<>();
        
        // Find ctx.bthread patterns
        Matcher matcher = BTHREAD_PATTERN.matcher(sourceCode);
        while (matcher.find()) {
            String bThreadName = matcher.group(1);
            String context = matcher.group(2);
            bThreadContexts.put(bThreadName, context);
            logger.info("Found COBP b-thread: {0} -> {1}", bThreadName, context);
        }
        
        // Find regular bthread patterns (no context)
        matcher = REGULAR_BTHREAD_PATTERN.matcher(sourceCode);
        while (matcher.find()) {
            String bThreadName = matcher.group(1);
            bThreadContexts.put(bThreadName, "GLOBAL"); // Regular bthreads have global context
            logger.info("Found regular b-thread: {0} -> GLOBAL", bThreadName);
        }
        
        return bThreadContexts;
    }
}
