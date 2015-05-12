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

import system.Task;
import java.util.List;
import system.Return;
import system.SpaceImpl;

public class ReturnDecomposition extends Return
{    
    final private TaskCompose compose;
    final private List<Task> tasks;
    
    public ReturnDecomposition( TaskCompose compose, List<Task> tasks )
    {
        this.compose = compose;
        this.tasks = tasks;
    }
    
    public TaskCompose compose() { return compose; }
    
    public List<Task> tasks() { return tasks; }
    
    /**
     *
     * @param parentTask the task whose result is to be processed.
     * @param space the Space that holds the Task and Results.
     */
    @Override
    public void process( Task parentTask, SpaceImpl space ) 
    {
        final int composeId = space.makeTaskId();
        compose.id( composeId );
        compose.composeId( parentTask.composeId() );
        compose.composeArgNum( parentTask.composeArgNum() );
        compose.numArgs( tasks.size() );
        space.putCompose( compose );
        compose.decomposeTaskRunTime( taskRunTime() );
        for ( int i = 0; i < tasks.size(); i++  )
        {
            Task task = tasks.get( i );
            task.id( space.makeTaskId() );
            task.composeId( composeId );
            task.composeArgNum( i );
            space.putReadyTask( task ); 
        }
    }
}
