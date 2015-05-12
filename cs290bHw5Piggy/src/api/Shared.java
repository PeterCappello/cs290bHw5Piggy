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
package api;

import java.io.Serializable;

/**
 * This mutable class and its extensions must synchronize all methods.
 * @author Peter Cappello
 * @param <T> the shared object's type.
 */
abstract public class Shared<T extends Shared> implements Serializable
{        
    private T shared;
    
    synchronized public T shared() { return shared; }
    
    /**
     * Is this shared object older that that shared object?
     * Implementation must synchronize on this.
     * @param that should not be null.
     * @return true if and only if this is older than that.
     */
    abstract public boolean isOlderThan( final T that );
    
    synchronized public boolean shared( final T that )
    {
        if ( this.isOlderThan( that ) )
        {
            copy( that );
            return true;
        }
        return false;
    }
    
    /**
     * Give this shared the state of that shared.
     * @param that
     */
    abstract public void copy( final T that );
    
    /**
     * Duplicate this Shared object.
     * @return the duplicate.
     */
    abstract public Shared duplicate();
}
