/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *        
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.filter;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import java.awt.RenderingHints.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geotools.feature.NameImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.feature.type.Name;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.parameter.Parameter;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Abstract class for a function expression implementation
 *
 * @author James Macgill, PSU
 *
 *
 * @source $URL$
 */
public abstract class FunctionExpressionImpl
    extends org.geotools.filter.DefaultExpression implements FunctionExpression {
	
	/** function name **/
	protected String name;

	/** function params **/
	protected List<org.opengis.filter.expression.Expression> params;
	
    protected Literal fallback;

    /** FunctionName provided by subclass; or lazely created */
    protected FunctionName functionName;
    
    /**
     * Preferred constructor to ensure name and functionName match.
     * <p>
     * Recommended use:<pre>
     * import static org.geotools.filter.capability.FunctionNameImpl.*;
     * public class AreaFunction extends FunctionExpressionImpl { 
     *     
     *   public static FunctionName NAME = new FunctionNameImpl("Area",
     *        parameter("area",Double.class),
     *        parameter("geometry",Geometry.class));
     * 
     * public AreaFunction() {
     *     super(NAME);
     * }
     * </pre>
     * 
     * @param functionName FunctionName describing subclass
     */
    protected FunctionExpressionImpl(FunctionName functionName){
        this( functionName.getName(), null );
        this.functionName = functionName;
    }
    protected FunctionExpressionImpl(String name ){
        this(new NameImpl(name));
    }
    protected FunctionExpressionImpl(Name name){
        this( name, null );
    }
    /**
     * Creates a new instance of FunctionExpression
     */
    protected FunctionExpressionImpl(String name, Literal fallback) {
        this(new NameImpl(name), fallback);
    }
    /**
     * Creates a new instance of FunctionExpression
     */
    protected FunctionExpressionImpl(Name name, Literal fallback) {
        this.functionName = new FunctionNameImpl(name, (Class)null);
        this.name = name.getLocalPart();
        this.fallback = fallback;
        params = new ArrayList<org.opengis.filter.expression.Expression>();
    }

     /**
     * Gets the type of this expression.
     *
     * @return the short representation of a function expression.
     */
    public short getType() {
        return FUNCTION;
    }

    /**
     * Gets the name of this function.
     *
     * @return the name of the function.
     * 
     */
    public String getName() {
    	return name;
    }
    
    public synchronized FunctionName getFunctionName() {
        if( functionName == null ){
            functionName = new FunctionNameImpl( getName(), getArgCount() );
        }
        return functionName;
    }

    /**
     * Sets the name of the function.
     */
    public void setName(String name) {
    	this.name = name;
    }
    
    public Literal getFallbackValue() {
        return fallback;
    }    
    public void setFallbackValue(Literal fallback) {
        this.fallback = fallback;
    }
    /**
     * Returns the function parameters.
     */
    public List<org.opengis.filter.expression.Expression> getParameters() {
    	return params;
    }
    
    /**
     * Sets the function parameters.
     */
    public void setParameters(List params) {
        if(params == null){
            throw new NullPointerException("Function parameters required");
        }
        final int argCount = getArgCount();
        final int paramsSize = params.size();
        if(argCount > 0 && argCount != paramsSize){
            throw new IllegalArgumentException("Function "+name+" expected " + argCount + 
                    " arguments, got " + paramsSize);
        }
    	this.params = new ArrayList(params);
    }
    
    /**
     * Since this class is heavily subclasses within the geotools toolkit 
     * itself we relax the 'final' restriction of this deprecated method.
     * 
     * @deprecated use {@link #getParameters()}.
     * 
     */
    public Expression[] getArgs() {
    	List params = getParameters();
    	return (Expression[])params.toArray(new Expression[params.size()]);
    }
    
    /**
     * Since this class is heavily subclassed within the geotools toolkit 
     * itself we relax the 'final' restriction of this deprecated method.
     * 
     * @deprecated use {@link #setParameters(List)}
     */
    public void setArgs(Expression[] args) {
    	setParameters(Arrays.asList(args));
    }

    /**
     * Gets the number of arguments that are set.
     *
     * @return the number of args.
     */
    public int getArgCount() {
        if (functionName != null && functionName.getArguments() != null) {
            int count = 0;
            for (Parameter<?> argument : functionName.getArguments()) {
                if (argument.getMinOccurs() != argument.getMaxOccurs()) {
                    return -1;
                } else {
                    count += argument.getMinOccurs();
                }
            }
            return count;
        } else {
            return 0;
        }
    }

    /**
     * @see org.opengis.filter.expression.Expression#accept(ExpressionVisitor, Object)
     */
    public Object accept(ExpressionVisitor visitor, Object extraData) {
    	return visitor.visit(this, extraData);
    }
    
    /**
     * Returns the implementation hints. The default implementation returns an empty map.
     */
    public  Map<Key, ?> getImplementationHints() {
        return Collections.emptyMap();
    }
    
    /**
     * Convenience method for creating a function name.
     * @see FunctionImpl#functionName(String, String, String...)
     */
    protected static FunctionName functionName(String name, String ret, String... args) {
        return FunctionImpl.functionName(name, ret, args);
    }

    /**
     * Creates a String representation of this Function with
     * the function name and the arguments. The String created
     * should be good for most subclasses
     */
    public String toString(){
        StringBuffer sb = new StringBuffer();
        sb.append(getName());
        sb.append("(");
        List<org.opengis.filter.expression.Expression> params = getParameters();
        if(params != null){
            org.opengis.filter.expression.Expression exp;
            for(Iterator<org.opengis.filter.expression.Expression> it = params.iterator(); it.hasNext();){
                exp = it.next();
                sb.append("[");
                sb.append(exp);
                sb.append("]");
                if(it.hasNext()){
                    sb.append(", ");
                }
            }
        }
        sb.append(")");
        return sb.toString();
    }
    
    /**
     * Utility method for subclasses to ask for an argument at a given index
     * 
     * @param index
     * @return
     */
    protected org.opengis.filter.expression.Expression getExpression(int index){
        org.opengis.filter.expression.Expression exp;
        exp = (org.opengis.filter.expression.Expression) getParameters().get(index);
        return exp;
    }
    
    public boolean equals(Object obj) {
    	if( obj == null || !(obj instanceof Function)){
    		return false;
    	}
    	Function other = (Function) obj;
    	if( ( getName() == null && other.getName() != null ) ||
    	    (getName() != null && !getName().equalsIgnoreCase( other.getName() ))){
    		return false;
    	}
    	if( getParameters() == null && other.getClass() != null ){
    		return false;
    	}
    	return getParameters() != null && getParameters().equals( other.getParameters() );
    }
}
