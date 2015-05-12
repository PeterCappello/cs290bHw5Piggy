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
 * FITNESS FOR ONE PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package applications.euclideantsp;

import api.ReturnSubtasks;
import api.ReturnValue;
import api.Task;
import api.TaskRecursive;
import clients.ClientEuclideanTsp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * Find a tour of minimum minCost among those that start with city 0, 
 followed by city secondCity.
 * @author Peter Cappello
 */
public class TaskEuclideanTsp extends TaskRecursive<TaskEuclideanTsp>
//public class TaskEuclideanTsp extends TaskRecursive<Tour>
{ 
    static final private double[][] CITIES = ClientEuclideanTsp.CITIES;
    static final Integer ONE = 1;
    static final Integer TWO = 2;
    static final Integer MAX_UNVISITED_CITIES = 10;
    
    final private List<Integer> partialTour;
    final private List<Integer> unvisitedCities;
    final private LowerBound lowerBound;
            
    public TaskEuclideanTsp( List<Integer> partialTour, List<Integer> unvisitedCities )
    {
        this.partialTour = partialTour;
        this.unvisitedCities = unvisitedCities;
//        lowerBound = new LowerBoundNearestNeighbors();
        lowerBound = new LowerBoundPartialTour( partialTour );
    }
    
    TaskEuclideanTsp( TaskEuclideanTsp parentTask, Integer newCity )
    {
        partialTour = new ArrayList<>( parentTask.partialTour );
        lowerBound = parentTask.lowerBound.make( parentTask, newCity );
        unvisitedCities = new LinkedList<>( parentTask.unvisitedCities );     
        partialTour.add( newCity );
        unvisitedCities.remove( newCity );
    }
    
    @Override
    public boolean isAtomic() { return unvisitedCities.size() <= MAX_UNVISITED_CITIES; }
    
    /**
     * Produce a tour of minimum cost from the set of tours, having as its
     * elements each tour consisting of the sequence of cities in partial tour 
     * followed by a permutation of the unvisited cities.
     * @return a tour of minimum cost.
     */
     @Override
    public ReturnValue solve() 
    {
        Stack<TaskEuclideanTsp> stack = new Stack<>();
        stack.push( this );
        SharedMinDouble shared = ( SharedMinDouble ) shared();
        TaskEuclideanTsp minTour = null;
        while (  ! stack.isEmpty() ) 
        {
            TaskEuclideanTsp currentTask = stack.pop();
            
            // get children whose lower bound is less than current upper bound.
            List<TaskEuclideanTsp> children = currentTask.children( shared.minCost() );
            for ( TaskEuclideanTsp child : children )
            { 
                if ( child.isComplete() )
                { 
                    minTour = child;
                    shared( new SharedMinDouble( child.lowerBound().cost() ) );
                } 
                else 
                { 
                    stack.push( child );
                } 
            }  
        } 
        return new ReturnValue<>( this, minTour );
    }

    @Override
    public ReturnSubtasks decompose() 
    {
        final List<Task> children = new  LinkedList<>();
        for ( Integer city : unvisitedCities )
        {
            children.add( new TaskEuclideanTsp( this, city ) );
        }
        return new ReturnSubtasks( new MinTour(), children );
    }
    
    public LowerBound lowerBound() { return lowerBound; }
    
    /**
     * Get children whose lower bound is less than the current upper bound.
     * @param upperBound
     * @return 
     */
    private List<TaskEuclideanTsp> children( double upperBound )
    {
        List<TaskEuclideanTsp> children = new LinkedList<>();
        for ( Integer city : unvisitedCities )
        {
            TaskEuclideanTsp child = new TaskEuclideanTsp( this, city );
            if ( child.lowerBound().cost() < upperBound )
            {
                children.add( child );
            }
        }
        return children;
    }
    
    public double cost() { return lowerBound().cost(); }
    
    public List<Integer> tour() { return partialTour; }
    
    @Override
    public String toString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append( getClass() );
        stringBuilder.append( " Partial tour: \n" );
        partialTour.stream().forEach(( city ) -> 
        {
            stringBuilder.append( city ).append( ": " );
            stringBuilder.append( CITIES[ city ][ 0 ] ).append( " " ).append( CITIES[ city ][ 1 ] ).append( '\n' );
        } );
        stringBuilder.append( "\n\tUnvisited cities: " );
        unvisitedCities.stream().forEach(( city ) -> 
        {
            stringBuilder.append( city ).append( " " );
        } );
        return stringBuilder.toString();
    }
    
    public List<Integer> unvisitedCities() { return unvisitedCities; }
   
   private boolean isComplete() { return unvisitedCities.isEmpty(); }
}
