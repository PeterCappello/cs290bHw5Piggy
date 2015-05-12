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
import api.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import static system.Configuration.MULTI_COMPUTERS;

/**
 * An implementation of the Remote Computer interface.
 * @author Peter Cappello
 */
public class ComputerImpl extends UnicastRemoteObject implements Computer
{
    static final private int FACTOR = 2;
           final private SpaceProxy spaceProxy;
           final private List<Worker> workerList = new ArrayList<>();
           final private AtomicInteger numTasks = new AtomicInteger();
           //!! delete sharedLock & test to ensure its OK.
           final private Boolean sharedLock = true;
                 private Shared shared;
           
    public ComputerImpl( Computer2Space space ) throws RemoteException
    {
        final int numWorkers = MULTI_COMPUTERS ? FACTOR * Runtime.getRuntime().availableProcessors() : 1;
        for ( int workerNum = 0; workerNum < numWorkers; workerNum++ )
        {
            workerList.add( new WorkerImpl() );
        }
        Logger.getLogger( this.getClass().getCanonicalName() ).log( Level.INFO, "Workers: {0}", numWorkers );
        spaceProxy = new SpaceProxy( space );
        spaceProxy.start();
    }
         
    /**
     * Execute a Task.
     * @param task to be executed.
     * @return the return value of the Task call method.
     * @throws RemoteException
     */
    @Override
    public Return execute( Task task ) throws RemoteException 
    { 
        numTasks.getAndIncrement();
        final long startTime = System.nanoTime();
        task.computer( this );
        final Return returnValue = task.call();
        final long runTime = ( System.nanoTime() - startTime ); // milliseconds
        returnValue.taskRunTime( runTime );
//        System.out.println("ComputerImpl.exeute: Task id: " + task.id() + " elapsed time: " + runTime);
       
        return returnValue;
    }
    
    public static void main( String[] args ) throws Exception
    {
        System.setSecurityManager( new SecurityManager() );
        /**
         * Its main method gets the domain name of its Space's machine from the command line. 
         */
        final String domainName = "localhost";
        final String url = "rmi://" + domainName + ":" + Space.PORT + "/" + Space.SERVICE_NAME;
        final Computer2Space space = (Computer2Space) Naming.lookup( url );
        ComputerImpl computer = new ComputerImpl( space );
        space.registerExternalComputer( computer, computer.workerList() );
        Logger.getLogger( ComputerImpl.class.getCanonicalName() ).log( Level.WARNING, "Computer running." );
    }

    /**
     * Terminate the JVM.
     * @throws RemoteException - always!
     */
    @Override
    public void exit() throws RemoteException 
    { 
        System.out.println("Computer # tasks complete:" + numTasks ); /*System.exit( 0 ); */ 
    }
        
    public Shared shared() { synchronized ( sharedLock ) { return shared; } }
    
    public void upShared( Shared that )
    {
        if ( shared.shared( that ) )
        {
            spaceProxy.upShared();
        }
    }
    
    public List<Worker> workerList() { return workerList; }

    @Override
    public void downShared( Shared that ) 
    { 
        if ( shared == null )
        {
            shared = that;
        }
        else
        {
            shared.shared( that ); 
        }
    }
    
    private class WorkerImpl implements Worker
    {
        @Override
        public Return execute( Task task ) throws RemoteException 
        {
            return ComputerImpl.this.execute( task );
        }
    }
    
    private class SpaceProxy extends Thread
    {
        final private Computer2Space space;
        final private BlockingQueue<Boolean> upSharedQ = new LinkedBlockingQueue<>(); 
        
        SpaceProxy( Computer2Space space ) { this.space = space; }
        
        @Override
        public void run()
        {
            try { upSharedQ.take(); } 
            catch (InterruptedException ex) 
            {
                Logger.getLogger(ComputerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
            try { space.upShared( shared.duplicate() ); } 
            catch ( RemoteException ex ) 
            {
                Logger.getLogger( ComputerImpl.class.getName() ).log( Level.SEVERE, null, ex );
            }
        }
        
        synchronized private void upShared() { upSharedQ.add( Boolean.TRUE ); }
    }
}
