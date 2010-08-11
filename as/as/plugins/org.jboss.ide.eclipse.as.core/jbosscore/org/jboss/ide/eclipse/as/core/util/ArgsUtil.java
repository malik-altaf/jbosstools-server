/******************************************************************************* 
 * Copyright (c) 2007 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.ide.eclipse.as.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ArgsUtil {

	public static final Integer NO_VALUE = new Integer(-1); 
	public static final String EQ = "="; //$NON-NLS-1$
	public static final String SPACE=" "; //$NON-NLS-1$
	public static final String VMO = "-D"; //$NON-NLS-1$
	public static final String EMPTY=""; //$NON-NLS-1$
	public static final String QUOTE="\""; //$NON-NLS-1$
	
	public static Map<String, Object> getSystemProperties(String argString) {
		String[] args = parse(argString);
		HashMap<String, Object> map = new HashMap<String, Object>();
		
		for( int i = 0; i < args.length; i++ ) {
			if( args[i].startsWith(VMO)) {
				int eq = args[i].indexOf(EQ);
				if( eq != -1 ) {
					map.put(args[i].substring(2, eq), 
							args[i].substring(eq+1));
				} else {
					map.put(args[i], NO_VALUE);
				}
			}
		}
		return map;
	}

	public static String getValue(String allArgs, String shortOpt, String longOpt) {
		return getValue(parse(allArgs), shortOpt, longOpt);
	}
	
	public static String getValue(String[] args, String shortOpt, String longOpt ) {
		for( int i = 0; i < args.length; i++ ) {
			if( args[i].equals(shortOpt) && i+1 < args.length)
				return args[i+1];
			if( longOpt != null && args[i].startsWith(longOpt + EQ)) 
				return args[i].substring(args[i].indexOf(EQ) + 1);
		}
		return null;
	}
	
	public static String[] parse(String s) {
		try {
			ArrayList<String> l = new ArrayList<String>();
			int length = s.length();
			
			int current = 0;
			boolean inQuotes = false;
			boolean done = false;
			String tmp = EMPTY;
			StringBuffer buf = new StringBuffer();
	
			while( !done ) {
				switch(s.charAt(current)) {
				case '"':
					inQuotes = !inQuotes;
					buf.append(s.charAt(current));
					break;
				case '\n':
				case ' ':
					if( !inQuotes ) {
						tmp = buf.toString();
						l.add(tmp);
						buf = new StringBuffer();
					} else {
						buf.append(' ');
					}
					break;
				default:
					buf.append(s.charAt(current));
					break;
				}
				current++;
				if( current == length ) done = true;
			}
			
			l.add(buf.toString());
			
			Object[] lArr = l.toArray();
			String[] retVal = new String[lArr.length];
			for( int i = 0; i < lArr.length; i++ ) {
				retVal[i] = (String)lArr[i];
			}
			return retVal;
		} catch( Exception e ) {
			return new String[] { };
		}
	}
	
	public static String setArg(String allArgs, String shortOpt, String longOpt, String value ) {
		if( value.contains(SPACE))
			value = QUOTE + value + QUOTE;
		return setArg(allArgs, shortOpt, longOpt, value, false);
	}
	
	public static String setArg(String allArgs, String shortOpt, String longOpt, String value, boolean addQuotes ) {
		if( addQuotes ) 
			value = QUOTE + value + QUOTE;
		boolean found = false;
		String[] args = parse(allArgs);
		String retVal = EMPTY;
		for( int i = 0; i < args.length; i++ ) {
			if( args[i].equals(shortOpt)) {
				args[i+1] = value;
				retVal += args[i] + SPACE + args[++i] + SPACE;
				found = true;
			} else if( longOpt != null && args[i].startsWith(longOpt + EQ)) { 
				args[i] = longOpt + EQ + value;
				retVal += args[i] + SPACE;
				found = true;
			} else {
				retVal += args[i] + SPACE;
			}
		}
		
		// turn this to a retval;
		if( !found )
			retVal = retVal + longOpt + EQ + value;
		return retVal;
	}
	
}