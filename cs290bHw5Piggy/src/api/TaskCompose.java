/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Peter Cappello
 * @param <I> input type.
 */
public abstract class TaskCompose<I> extends Task
{
    private AtomicInteger numUnsetArgs;
    private List<I> args;
    private long decomposeTaskRunTime;
    private long sumChildT1;
    private long maxChildTinf;
    
    @Override
    abstract public ReturnValue call();
    
    public List<I> args() { return args; }
    
    public void arg( final int argNum, final I argValue ) 
    { 
        assert numUnsetArgs.get() > 0 && ! isReady() && args.get( argNum ) == null; 
        args.set( argNum, argValue );
        numUnsetArgs.getAndDecrement();
        assert args.get( argNum ) == argValue;
    }
    
    public void numArgs( int numArgs )
    {
        assert numArgs >= 0;
        numUnsetArgs = new AtomicInteger( numArgs );
        args = Collections.synchronizedList( new ArrayList<>( numArgs ) ) ;
        for ( int i = 0; i < numArgs; i++ )
        {
            args.add( null );
            assert args.get( i ) == null;
        }
        assert args.size() == numArgs;
    }
    
    public boolean isReady() { return numUnsetArgs.get() == 0; }
    
    public void decomposeTaskRunTime( long time ) { decomposeTaskRunTime = time; }
    public long decomposeTaskRunTime() { return decomposeTaskRunTime; }
    
    public long sumChildT1() { return sumChildT1; }
    public void sumChildT1( long time ) { sumChildT1 +=  time; }
    
    public long maxChildTInf() { return maxChildTinf; }
    public void maxChildTInf( long time ) { maxChildTinf = maxChildTinf < time ? time : maxChildTinf; }
}
