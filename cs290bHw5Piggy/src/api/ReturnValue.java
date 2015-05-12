/*
 * The MIT License
 *
 * Copyright 2015 Peter Cappello.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package api;

import system.Return;
import system.SpaceImpl;

/**
 *
 * @author Peter Cappello
 * @param <T>
 */
public class ReturnValue<T> extends Return
{    
    final private int composeId;
    final private int composeArgNum;
    final private T value;
    
    public ReturnValue( final Task task, final T value ) 
    { 
        assert task != null;
        composeId = task.composeId();
        composeArgNum = task.composeArgNum();
        this.value = value; 
    }
    
    public T value() { return value; }
   
    /**
     *
     * @param associatedTask unused - the task whose Result is to be processed.
     * @param space
     */
    @Override
    public void process( Task associatedTask, SpaceImpl space )
    {
        if ( associatedTask instanceof TaskCompose )
        {
            TaskCompose task = (TaskCompose) associatedTask;
            long commonTime = task.decomposeTaskRunTime() + taskRunTime();
            t1(   commonTime + task.sumChildT1() );
            tInf( commonTime + task.maxChildTInf() );
        }
        else
        {
            t1(   taskRunTime() );
            tInf( taskRunTime() );
        }
        
        if ( composeId == SpaceImpl.FINAL_RETURN_VALUE )
        {
            space.tInf( tInf() );
            space.putResult( this );
            return;
        }
        TaskCompose compose = space.getCompose( composeId );
        assert compose != null && ! compose.isReady();
        compose.arg( composeArgNum, value );
        compose.sumChildT1( t1() );
        compose.maxChildTInf( tInf() );
        if ( compose.isReady() )
        {
            space.putReadyTask( compose );
            space.removeWaitingTask( composeId );
        }
    }
}
