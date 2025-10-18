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
    
    private COBPRuntimeSupport cobpRuntimeSupport;
    private String modifiedSourceCode;
    
    public COBPAwareBProgram(String resourceName, COBPRuntimeSupport cobpRuntimeSupport) {
        super(resourceName);
        this.cobpRuntimeSupport = cobpRuntimeSupport;
    }
    
    public COBPAwareBProgram(String resourceName, String modifiedSourceCode, COBPRuntimeSupport cobpRuntimeSupport) {
        super(resourceName);
        this.modifiedSourceCode = modifiedSourceCode;
        this.cobpRuntimeSupport = cobpRuntimeSupport;
    }
    
    public void setCOBPRuntimeSupport(COBPRuntimeSupport cobpRuntimeSupport) {
        this.cobpRuntimeSupport = cobpRuntimeSupport;
    }
    
    @Override
    protected void setupProgramScope(Scriptable scope) {
        try {
            // Get the current JavaScript context
            Context jsContext = Context.getCurrentContext();
            if (jsContext != null) {
                // Inject COBP runtime support before calling parent setup
                if (cobpRuntimeSupport != null) {
                    cobpRuntimeSupport.injectCOBPContext(jsContext, scope);
                }
            }
            
            // If we have modified source code, use it directly instead of reading from resource
            if (modifiedSourceCode != null) {
                logger.info("Using modified COBP source code directly");
                evaluate(modifiedSourceCode, "COBPSource", jsContext);
            } else {
                // Call parent setup to load the original source code
                super.setupProgramScope(scope);
            }
            
        } catch (Exception e) {
            logger.error("Failed to setup COBP program scope: {0}", e, e.getMessage());
            // Fall back to regular setup
            super.setupProgramScope(scope);
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
