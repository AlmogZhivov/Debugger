package il.ac.bgu.se.bp.utils;

import il.ac.bgu.se.bp.utils.logger.Logger;

/**
 * Utility class to detect if source code contains COBP syntax
 */
public class COBPDetector {
    private static final Logger logger = new Logger(COBPDetector.class);
    
    // COBP-specific keywords and patterns
    private static final String[] COBP_KEYWORDS = {
        "ctx.bthread",
        "ctx.Entity",
        "ctx.populateContext",
        "ctx.registerQuery",
        "ctx.registerEffect",
        "ctx.runQuery",
        "ctx.getEntityById",
        "ctx.removeEntity",
        "ctx.insertEntity"
    };
    
    private static final String[] COBP_FUNCTIONS = {
        "sync(",
        "Event("
    };
    
    /**
     * Detects if the source code contains COBP syntax
     * @param sourceCode the JavaScript source code to analyze
     * @return true if COBP syntax is detected, false otherwise
     */
    public static boolean isCOBPCode(String sourceCode) {
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return false;
        }
        
        String code = sourceCode.trim();
        
        // Check for COBP keywords
        for (String keyword : COBP_KEYWORDS) {
            if (code.contains(keyword)) {
                logger.info("COBP code detected - found keyword: {0}", keyword);
                return true;
            }
        }
        
        // Check for COBP functions (but not BPjs equivalents)
        for (String function : COBP_FUNCTIONS) {
            if (code.contains(function) && !code.contains("bp." + function)) {
                logger.info("COBP code detected - found function: {0}", function);
                return true;
            }
        }
        
        logger.info("No COBP syntax detected - treating as regular BPjs code");
        return false;
    }
    
    /**
     * Gets a description of why the code was detected as COBP
     * @param sourceCode the JavaScript source code to analyze
     * @return description of detected COBP features
     */
    public static String getCOBPFeatures(String sourceCode) {
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return "No code provided";
        }
        
        String code = sourceCode.trim();
        StringBuilder features = new StringBuilder();
        
        for (String keyword : COBP_KEYWORDS) {
            if (code.contains(keyword)) {
                if (features.length() > 0) {
                    features.append(", ");
                }
                features.append(keyword);
            }
        }
        
        for (String function : COBP_FUNCTIONS) {
            if (code.contains(function) && !code.contains("bp." + function)) {
                if (features.length() > 0) {
                    features.append(", ");
                }
                features.append(function);
            }
        }
        
        return features.length() > 0 ? features.toString() : "No COBP features detected";
    }
}
