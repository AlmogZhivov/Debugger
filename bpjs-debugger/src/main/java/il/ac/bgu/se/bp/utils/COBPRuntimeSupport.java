package il.ac.bgu.se.bp.utils;

import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.se.bp.utils.logger.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides COBP runtime support by injecting a 'ctx' object into the JavaScript runtime.
 * This allows COBP code to execute properly by providing ctx.bthread, ctx.Event, ctx.sync, etc.
 */
public class COBPRuntimeSupport {
    private static final Logger logger = new Logger(COBPRuntimeSupport.class);
    
    private final Map<String, String> bThreadContexts = new ConcurrentHashMap<>();
    
    public COBPRuntimeSupport(BProgram bProgram) {
        // BProgram parameter kept for future use
    }
    
    /**
     * Injects the COBP 'ctx' object into the JavaScript runtime
     */
    public void injectCOBPContext(Context jsContext, Scriptable scope) {
        try {
            // Create JavaScript code that defines the ctx object
            String cobpCode = 
                "var ctx = {\n" +
                "  bthread: function(name, context, func) {\n" +
                "    // Store context mapping for later extraction\n" +
                "    if (typeof ctx._contexts === 'undefined') {\n" +
                "      ctx._contexts = {};\n" +
                "    }\n" +
                "    ctx._contexts[name] = context;\n" +
                "    // Delegate to bp.registerBThread\n" +
                "    return bp.registerBThread(name, func);\n" +
                "  },\n" +
                "  Event: function(name, data) {\n" +
                "    return bp.Event(name, data);\n" +
                "  },\n" +
                "  sync: function(options) {\n" +
                "    return bp.sync(options);\n" +
                "  },\n" +
                "  Entity: function() {\n" +
                "    // Placeholder for COBP Entity\n" +
                "    return {};\n" +
                "  },\n" +
                "  populateContext: function() {\n" +
                "    // Placeholder for COBP populateContext\n" +
                "    return {};\n" +
                "  },\n" +
                "  registerQuery: function() {\n" +
                "    // Placeholder for COBP registerQuery\n" +
                "    return {};\n" +
                "  },\n" +
                "  registerEffect: function() {\n" +
                "    // Placeholder for COBP registerEffect\n" +
                "    return {};\n" +
                "  },\n" +
                "  runQuery: function() {\n" +
                "    // Placeholder for COBP runQuery\n" +
                "    return {};\n" +
                "  },\n" +
                "  getEntityById: function() {\n" +
                "    // Placeholder for COBP getEntityById\n" +
                "    return {};\n" +
                "  },\n" +
                "  removeEntity: function() {\n" +
                "    // Placeholder for COBP removeEntity\n" +
                "    return {};\n" +
                "  },\n" +
                "  insertEntity: function() {\n" +
                "    // Placeholder for COBP insertEntity\n" +
                "    return {};\n" +
                "  }\n" +
                "};\n";
            
            // Execute the JavaScript code to create the ctx object
            jsContext.evaluateString(scope, cobpCode, "COBPContext", 1, null);
            
            logger.info("Successfully injected COBP 'ctx' object into JavaScript runtime");
            
        } catch (Exception e) {
            logger.error("Failed to inject COBP context: {0}", e, e.getMessage());
        }
    }
    
    /**
     * Gets the context mapping for b-threads
     */
    public Map<String, String> getBThreadContexts() {
        return bThreadContexts;
    }
    
    /**
     * Extracts context mappings from the JavaScript ctx object
     */
    public void extractContextsFromJavaScript(Context jsContext, Scriptable scope) {
        try {
            // Get the ctx object from the JavaScript scope
            Object ctxObj = scope.get("ctx", scope);
            if (ctxObj instanceof Scriptable) {
                Scriptable ctx = (Scriptable) ctxObj;
                Object contextsObj = ctx.get("_contexts", ctx);
                if (contextsObj instanceof Scriptable) {
                    Scriptable contexts = (Scriptable) contextsObj;
                    Object[] ids = contexts.getIds();
                    for (Object id : ids) {
                        String bThreadName = id.toString();
                        String context = contexts.get(bThreadName, contexts).toString();
                        bThreadContexts.put(bThreadName, context);
                        logger.info("Extracted context from JavaScript: {0} -> {1}", bThreadName, context);
                    }
                } else {
                    logger.info("No _contexts object found in ctx scope");
                }
            } else {
                logger.info("No ctx object found in global scope");
            }
        } catch (Exception e) {
            logger.error("Failed to extract contexts from JavaScript: {0}", e, e.getMessage());
        }
    }
    
    /**
     * Extracts context mappings from the current JavaScript runtime
     */
    public void extractContextsFromCurrentRuntime() {
        try {
            Context jsContext = Context.getCurrentContext();
            if (jsContext != null) {
                Scriptable scope = jsContext.initStandardObjects();
                extractContextsFromJavaScript(jsContext, scope);
            }
        } catch (Exception e) {
            logger.error("Failed to extract contexts from current runtime: {0}", e, e.getMessage());
        }
    }
    
}
