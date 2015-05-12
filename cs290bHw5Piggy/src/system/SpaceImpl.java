/*
 * The MIT License
 *
 * Copyright 2015 peter.
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
package system;

import api.Computer;
import api.Shared;
import api.Space;
import api.Task;
import api.TaskCompose;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import static system.Configuration.SPACE_CALLABLE;

/**
 * SpaceImpl implements the space for coordinating sending/receiving Task and Result objects.
 * @author Peter Cappello
 */
public final class SpaceImpl extends UnicastRemoteObject implements Space, Computer2Space
{
    static final public int FINAL_RETURN_VALUE = -1;
    static final private AtomicInteger computerIds = new AtomicInteger();
    
    final private AtomicInteger taskIds = new AtomicInteger();
    final private BlockingQueue<Task>   readyTaskQ = new LinkedBlockingQueue<>();
    final private BlockingQueue<Return> resultQ    = new LinkedBlockingQueue<>();
    final private BlockingQueue<Task>   readySpaceCallableTaskQ = new LinkedBlockingQueue<>();
    final private Map<Computer, ComputerProxy> computerProxies = Collections.synchronizedMap( new HashMap<>() );
    final private Map<Integer, TaskCompose>   waitingTaskMap   = Collections.synchronizedMap( new HashMap<>() );
    final private AtomicInteger numTasks = new AtomicInteger();
          private Shared shared; // mutable but thread-safe: its state changes are synchronized on itself.
          private long t1   = 0;
          private long tInf = 0;
    
    public SpaceImpl() throws RemoteException 
    {
        Logger.getLogger(getClass().getName() ).log( Level.INFO, "Space started." );
        if ( SPACE_CALLABLE )
        {
            ComputerImpl computerInternal = new ComputerImpl( this );
            registerInternalComputer( computerInternal, computerInternal.workerList() );
        }
    }
    
    /**
     * Compute a Task and return its Return.
     * To ensure that the correct Return is returned, this must be the only
 computation that the Space is serving.
     * 
     * @param task
     * @return the Task's Return object.
     */
    @Override
    public Return compute( Task task )
    {
        initTimeMeasures();
        for ( ComputerProxy computerProxy : computerProxies.values() )
        {
            computerProxy.notifyWorkerProxies();
        }
        call( task );
        Return result = take();
        reportTimeMeasures( result );
        return result;
    }
    
    /**
     *
     * @param task
     * @param shared
     * @return
     */
    @Override
    public Return compute( Task task, Shared shared )
    {
        initTimeMeasures();
        initShared( shared );
        call( task );
        Return result = take();
        reportTimeMeasures( result );
        return result;
        
    }
    /**
     * Put a task into the Task queue.
     * @param task
     */
    @Override
    public void call( Task task ) 
    { 
        task.id( makeTaskId() );
        task.composeId( FINAL_RETURN_VALUE );
        readyTaskQ.add( task );
    }

    /**
     * Take a Return from the Return queue.
     * @return a Return object.
     */
    @Override
    public Return take() 
    {
        try { return resultQ.take(); } 
        catch ( InterruptedException exception ) 
        {
            Logger.getLogger(SpaceImpl.class.getName()).log(Level.INFO, null, exception);
        }
        assert false; // should never reach this point
        return null;
    }

    @Override
    public void exit() throws RemoteException 
    {
        computerProxies.values().forEach( proxy -> proxy.exit() );
        System.exit( 0 );
    }

    /**
     * Register Computer with Space.  
     * Will override existing key-value pair, if any.
     * @param computer
     * @param workerList
     * @throws RemoteException
     */
    @Override
    public void registerExternalComputer( Computer computer, List<Worker> workerList ) throws RemoteException
    {
        final ComputerProxy computerProxy = new ComputerProxy( computer, workerList, readyTaskQ );
        register( computer, computerProxy );
    }
    
    public void registerInternalComputer( Computer computer, List<Worker> workerList ) throws RemoteException
    {
        final ComputerProxy computerProxy = new ComputerProxy( computer, workerList, readySpaceCallableTaskQ );
        register( computer, computerProxy );
    }
    
    private void register( Computer computer, ComputerProxy computerProxy )
    {
        computerProxies.put( computer, computerProxy );
        computerProxy.start();
        computerProxy.startWorkerProxies();
        Logger.getLogger(SpaceImpl.class.getName()).log(Level.INFO, "Computer {0} started.", computerProxy.computerId);
    }
    
    public static void main( String[] args ) throws Exception
    {
        System.setSecurityManager( new SecurityManager() );
        LocateRegistry.createRegistry( Space.PORT )
                      .rebind(Space.SERVICE_NAME, new SpaceImpl() );
    }

    synchronized public void processResult( Task parentTask, Return result )
    { 
        numTasks.getAndIncrement();
        result.process( parentTask, this );
        t1 += result.taskRunTime();
    }
    
    public int makeTaskId() { return taskIds.incrementAndGet(); }
    
    public TaskCompose getCompose( int composeId ) { return waitingTaskMap.get( composeId ); }
            
    public void putCompose( TaskCompose compose )
    {
        assert waitingTaskMap.get( compose.id() ) == null; 
        waitingTaskMap.put( compose.id(), compose );
        assert waitingTaskMap.get( compose.id() ) != null;
    }
    
    public void putReadyTask( Task task ) 
    { 
        assert waitingTaskMap.get( task.composeId() ) != null || task.composeId() == FINAL_RETURN_VALUE : task.composeId();
        if ( SPACE_CALLABLE && task.isSpaceCallable() )
        {
            try { readySpaceCallableTaskQ.put( task ); } catch ( InterruptedException ignore ){} 
        }
        else
        {
            try { readyTaskQ.put( task ); } catch ( InterruptedException ignore ){} 
        }
    }
    
    public void removeWaitingTask( int composeId )
    { 
        assert waitingTaskMap.get( composeId ) != null; 
        waitingTaskMap.remove( composeId ); 
    }
    
    public void putResult( Return result )
    {
        try { resultQ.put( result ); } catch( InterruptedException ignore ){}
    }
    
    @Override
    public void upShared( Shared that )
    {
        if ( this.shared.shared( that ) )
        {
            for ( ComputerProxy computerProxy : computerProxies.values() )
            {
                computerProxy.downShared( this.shared );
            }
        }
    }
    
    public void tInf( long tInf ) { this.tInf = tInf; }
    
    private void initTimeMeasures()
    {
        numTasks.getAndSet( 0 );
        t1 = 0;
        tInf = 0;
    }
    
    private void initShared( Shared shared )
    {
        this.shared = shared;
        for ( ComputerProxy computerProxy : computerProxies.values() )
        {
            computerProxy.initShared( this.shared );
        }
    }
    
    private void reportTimeMeasures( Return result )
    {
        Logger.getLogger( this.getClass().getCanonicalName() )
                .log(Level.INFO, "\n\tTotal tasks: {0} \n\tT_1: {1}ms.\n\tT_inf: {2}ms.", new Object[]{numTasks, result.t1() / 1000000, result.tInf() / 1000000});
    }
    
    private class ComputerProxy extends Thread implements Computer 
    {
        final private Computer computer;
        final private int computerId = computerIds.getAndIncrement();
        final private Map<Worker, WorkerProxy> workerMap = new HashMap<>();
        final private BlockingQueue<Boolean> downSharedQ = new LinkedBlockingQueue<>();
        final private BlockingQueue<Task> readyTaskQ;

        ComputerProxy( Computer computer, List<Worker> workerList, BlockingQueue<Task> readyTaskQ )
        { 
            this.computer = computer;
            this.readyTaskQ = readyTaskQ;
            for ( Worker worker : workerList )
            {
                WorkerProxy workerProxy = new WorkerProxy( worker );
                workerMap.put( worker, workerProxy );
            }
        }
        
        private void startWorkerProxies()
        {
            for ( WorkerProxy workerProxy : workerMap.values() )
            {
                workerProxy.start();
            }
        }

        @Override
        public Return execute( Task task ) throws RemoteException
        { 
            return computer.execute( task );
        }
        
        private void unregister( Task task, Computer computer, Worker worker )
        {
            readyTaskQ.add( task );
            workerMap.remove( worker );
            Logger.getLogger( this.getClass().getName() )
                  .log( Level.WARNING, "Computer {0}: Worker failed.", computerId );
            if ( workerMap.isEmpty() )
            {
                computerProxies.remove( computer );
                Logger.getLogger( ComputerProxy.class.getCanonicalName() )
                      .log( Level.WARNING, "Computer {0} failed.", computerId );
                interrupt();
            }
        }
        
        @Override
        public void exit() 
        { 
            try { computer.exit(); } 
            catch ( RemoteException ignore ) {} 
        }
        
        @Override
        public void run()
        {
            while ( true )
            {
                try { downSharedQ.take(); } 
                catch ( InterruptedException ex ) 
                {
                    Logger.getLogger( ComputerProxy.class.getName() )
                          .log( Level.SEVERE, null, ex );
                }
                try { computer.downShared( shared.duplicate() ); } 
                catch ( RemoteException ex ) 
                {
                    Logger.getLogger( ComputerProxy.class.getName() )
                          .log( Level.SEVERE, null, ex );
                }
            }
        }
        
        @Override
        public void downShared( Shared shared ) { downSharedQ.add( Boolean.TRUE ); }
        
        public void initShared( Shared shared )
        {            
            try { computer.downShared( shared.duplicate() ); } 
            catch ( RemoteException ex ) 
            {
                Logger.getLogger( SpaceImpl.class.getName() )
                      .log( Level.SEVERE, null, ex );
            }
            notifyWorkerProxies();
        }
        
        private void notifyWorkerProxies()
        {
            for ( WorkerProxy workerProxy : workerMap.values() )
            {
               synchronized ( workerProxy ) { workerProxy.notify(); } 
            }
        }
     
        private class WorkerProxy extends Thread implements Worker
        {
            final private Worker worker;
            
            private WorkerProxy( Worker worker ) { this.worker = worker; }
            
            @Override
            public void run()
            {
                try { synchronized( this ) { wait(); } }
                catch ( InterruptedException ex ) 
                {
                    Logger.getLogger( WorkerProxy.class.getName() )
                          .log( Level.SEVERE, null, ex );
                }
                while ( true )
                {
                    Task task = null;
                    try 
                    { 
                        task = ComputerProxy.this.readyTaskQ.take();
                        processResult( task, execute( task ) );
                    }
                    catch ( RemoteException ignore )
                    {
                        unregister( task, computer, worker );
                        break;
                    } 
                    catch ( InterruptedException ex ) 
                    { 
                        Logger.getLogger( this.getClass().getName() )
                              .log( Level.INFO, null, ex ); 
                    }
                }
            }
            
            @Override
            public Return execute( Task task ) throws RemoteException 
            {
                return worker.execute( task );
            }     
        }
    }
}
