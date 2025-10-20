package il.ac.bgu.se.bp.execution;

import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.se.bp.utils.COBPRuntimeSupport;
import il.ac.bgu.se.bp.utils.logger.Logger;
import il.ac.bgu.cs.bp.bpjs.model.ResourceBProgram;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * A COBP-aware BProgram that extends ResourceBProgram to inject COBP runtime support
 * into the JavaScript runtime during setup.
 */
public class COBPAwareBProgram extends ResourceBProgram {
    private static final Logger logger = new Logger(COBPAwareBProgram.class);
    
    private String modifiedSourceCode;
    
    public COBPAwareBProgram(String resourceName, String modifiedSourceCode, COBPRuntimeSupport cobpRuntimeSupport) {
        super(resourceName);
        this.modifiedSourceCode = modifiedSourceCode;
    }
    
    @Override
    protected void setupProgramScope(Scriptable scope) {
        try {
            // If we have modified source code, use it directly
            if (modifiedSourceCode != null) {
                logger.info("Using modified COBP source code for setup");
                
                // First call parent setup to ensure bp object is available
                super.setupProgramScope(scope);
                
                // Then evaluate the modified source code (which includes both COBP definitions and original code)
                Context jsContext = Context.getCurrentContext();
                if (jsContext != null) {
                    jsContext.evaluateString(scope, modifiedSourceCode, "COBPSource", 1, null);
                    logger.info("Successfully evaluated modified COBP source code");
                }
                
            } else {
                // Fall back to regular setup
                super.setupProgramScope(scope);
            }
            
        } catch (Exception e) {
            logger.error("Failed to setup COBP program scope: {0}", e, e.getMessage());
            // Fall back to regular setup
            super.setupProgramScope(scope);
        }
    }
    
    @Override
    protected Object evaluate(String sourceCode, String sourceName, Context jsContext) {
        try {
            // If we have modified source code, use it instead of the original
            if (modifiedSourceCode != null) {
                logger.info("Evaluating modified COBP source code instead of original");
                return super.evaluate(modifiedSourceCode, sourceName, jsContext);
            } else {
                // Fall back to original evaluation
                return super.evaluate(sourceCode, sourceName, jsContext);
            }
        } catch (Exception e) {
            logger.error("Failed to evaluate COBP source code: {0}", e, e.getMessage());
            // Fall back to original evaluation
            return super.evaluate(sourceCode, sourceName, jsContext);
        }
    }
    
    
    @Override
    public BProgramSyncSnapshot setup() {
        try {
            // Call the parent setup method
            BProgramSyncSnapshot result = super.setup();
            
            // Note: Context extraction is now handled in DebuggerStateHelper
            // after b-threads have been executed, not during setup
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to setup COBP-aware BProgram: {0}", e, e.getMessage());
            // Fall back to regular setup
            return super.setup();
        }
    }
    
}
