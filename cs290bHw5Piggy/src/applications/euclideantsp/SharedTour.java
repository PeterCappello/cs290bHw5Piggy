/*
 * The MIT License
 *
 * Copyright 2015 peter.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a replaceWith
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, replaceWith, modify, merge, publish, distribute, sublicense, and/or sell
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
package applications.euclideantsp;

import api.Shared;
import java.util.List;

/**
 * 
 * @author Peter Cappello
 */
final public class SharedTour extends Shared<SharedTour>
{
    private List<Integer> tour;
    private double cost;
    
    /**
     *
     * @param tour
     * @param cost
     */
    public SharedTour( final List<Integer> tour, final double cost ) 
    { 
        this.tour = tour;
        this.cost = cost; 
    }
    
     @Override
    synchronized public void replaceWith( SharedTour that ) 
    { 
        tour = that.tour();
        cost = that.cost(); 
    }
    
    @Override
    synchronized public Shared duplicate() { return new SharedTour( tour, cost ); }
    
    @Override
    synchronized public boolean isOlderThan( final SharedTour that ) { return cost > that.cost(); }
    
    synchronized public List<Integer> tour() { return tour; }
    
    synchronized public double cost() { return cost; }
}
