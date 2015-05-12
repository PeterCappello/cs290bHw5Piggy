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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of the Remote Computer interface.
 * @author Peter Cappello
 */
public class ComputerImpl extends UnicastRemoteObject implements Computer
{
    final private SpaceProxy spaceProxy;
    //!! delete sharedLock & test to ensure its OK.
    final private Boolean sharedLock = true;
          private Shared shared;
           
    public ComputerImpl( Space space ) throws RemoteException
    {
        Logger.getLogger( this.getClass().getCanonicalName() )
              .log(Level.INFO, "Computer: started with {0} available processors.", Runtime.getRuntime().availableProcessors() );
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
        final long startTime = System.nanoTime();
        task.computer( this );
        final Return returnValue = task.call();
        final long runTime = ( System.nanoTime() - startTime ); // milliseconds
        returnValue.taskRunTime( runTime );       
        return returnValue;
    }
    
    public static void main( String[] args ) throws Exception
    {
        System.setSecurityManager( new SecurityManager() );
        final String domainName = args.length == 0 ? "localhost" : args[ 0 ];
        final String url = "rmi://" + domainName + ":" + Space.PORT + "/" + Space.SERVICE_NAME;
        final Space space = (Space) Naming.lookup( url );
        space.register( new ComputerImpl( space ), Runtime.getRuntime().availableProcessors() );
    }
        
    public Shared shared() { synchronized ( sharedLock ) { return shared; } }
    
    public void upShared( Shared that )
    {
        if ( shared.shared( that ) )
        {
            spaceProxy.upShared();
        }
    }
    
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
    
    private class SpaceProxy extends Thread
    {
        final private Space space;
        final private BlockingQueue<Boolean> upSharedQ = new LinkedBlockingQueue<>(); 
        
        SpaceProxy( Space space ) { this.space = space; }
        
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
