/*
 * Copyright 2007 University of Waikato.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.  
 */

package com.github.javacliparser;

import java.text.DecimalFormat;

/**
 * Class implementing some string utility methods.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class StringUtils {

    public static final String newline = System.getProperty("line.separator");

    public static String doubleToString(double value, int fractionDigits) {
        return doubleToString(value, 0, fractionDigits);
    }

    public static String doubleToString(double value, int minFractionDigits,
            int maxFractionDigits) {
        DecimalFormat numberFormat = new DecimalFormat();
        numberFormat.setMinimumFractionDigits(minFractionDigits);
        numberFormat.setMaximumFractionDigits(maxFractionDigits);
        return numberFormat.format(value);
    }

    public static void appendNewline(StringBuilder out) {
        out.append(newline);
    }

    public static void appendIndent(StringBuilder out, int indent) {
        for (int i = 0; i < indent; i++) {
            out.append(' ');
        }
    }

    public static void appendIndented(StringBuilder out, int indent, String s) {
        appendIndent(out, indent);
        out.append(s);
    }

    public static void appendNewlineIndented(StringBuilder out, int indent,
            String s) {
        appendNewline(out);
        appendIndented(out, indent, s);
    }

}
