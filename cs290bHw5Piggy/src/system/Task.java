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
import api.Shared;
import api.Space;
import api.TaskCompose;
import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 *
 * @author Peter Cappello
 */
abstract public class Task implements Serializable, Callable<Return> 
{ 
    private int id;
    private int composeId;
    private int composeArgNum;
    private ComputerImpl computerImpl;
    protected Space space;
    
    @Override
    abstract public Return call(); 
        
    public int  id() { return id; }
    public void id( int id ) { this.id = id; }
    
    public int  composeArgNum() { return composeArgNum; }
    public void composeArgNum( int composeArgNum ) { this.composeArgNum = composeArgNum; }
    
    public int  composeId() { return composeId; }
    public void composeId( int composeId ) { this.composeId = composeId; }
    
    public void computer( ComputerImpl computerImpl ) { this.computerImpl = computerImpl; }
    
    public Shared shared() { return computerImpl.shared(); }
    
    public void shared( Shared shared ) { computerImpl.upShared( shared ); }
    
    public boolean isSpaceCallable() { return this instanceof TaskCompose; }
}
