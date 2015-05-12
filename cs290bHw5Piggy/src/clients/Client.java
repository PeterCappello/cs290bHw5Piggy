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
package clients;

import api.ReturnValue;
import api.Shared;
import api.Space;
import system.Task;
import java.awt.BorderLayout;
import java.awt.Container;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import system.ComputerImpl;
import system.SpaceImpl;

/**
 *
 * @author Peter Cappello
 * @param <T> return type the Task that this Client executes.
 */
abstract public class Client<T> extends JFrame
{    
    private long startTime; 
    
    public Client( final String title ) throws RemoteException
    {     
        setTitle( title );
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
    }
    
    public void begin() { startTime = System.nanoTime(); }
    
    public void end() 
    { 
        Logger.getLogger( Client.class.getCanonicalName() ).log(Level.INFO, "Client time: {0} ms.", (System.nanoTime() - startTime) / 1000000 );
    }
    
    public void add( final JLabel jLabel )
    {
        final Container container = getContentPane();
        container.setLayout( new BorderLayout() );
        container.add( new JScrollPane( jLabel ), BorderLayout.CENTER );
        pack();
        setVisible( true );
    }
    
    public Space getSpace( String domainName ) throws RemoteException, NotBoundException, MalformedURLException
    {
        final String url = "rmi://" + domainName + ":" + Space.PORT + "/" + Space.SERVICE_NAME;
        return ( Space ) Naming.lookup( url );
    }
    
    public Space getSpace( int numComputers ) throws RemoteException
    {
        SpaceImpl space = new SpaceImpl();
        for ( int i = 0; i < numComputers; i++ )
        {
            ComputerImpl computer = new ComputerImpl( space );
            space.registerExternalComputer( computer, computer.workerList() );
        }
        return space;
    }
    
    abstract JLabel getLabel( T returnValue );
    
    static public void runClient( Client client, int numComputers, Task task ) throws RemoteException
    {
        System.setSecurityManager( new SecurityManager() );
        client.begin();
        Space space = client.getSpace( numComputers );
        ReturnValue<Integer> result = ( ReturnValue<Integer> ) space.compute( task );
        client.add( client.getLabel( result.value() ) );
        client.end();
    }
    
    static public void runClient( Client client, int numComputers, Task task, Shared shared ) throws RemoteException
    {
        System.setSecurityManager( new SecurityManager() );
        client.begin();
        Space space = client.getSpace( numComputers );
        ReturnValue<Integer> result = ( ReturnValue<Integer> ) space.compute( task, shared );
        client.add( client.getLabel( result.value() ) );
        client.end();
    }
}
