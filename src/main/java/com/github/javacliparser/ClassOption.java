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

import java.io.File;

/** 
 * Class option.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class ClassOption extends AbstractClassOption {

    private static final long serialVersionUID = 1L;

    public ClassOption(String name, char cliChar, String purpose,
            Class<?> requiredType, String defaultCLIString) {
        super(name, cliChar, purpose, requiredType, defaultCLIString);
    }

    public ClassOption(String name, char cliChar, String purpose,
            Class<?> requiredType, String defaultCLIString, String nullString) {
        super(name, cliChar, purpose, requiredType, defaultCLIString, nullString);
    }

    @Override
    public String getValueAsCLIString() {
        if ((this.currentValue == null) && (this.nullString != null)) {
            return this.nullString;
        }
        return objectToCLIString(this.currentValue, this.requiredType);
    }

    @Override
    public void setValueViaCLIString(String s) {
        if ((this.nullString != null)
                && ((s == null) || (s.length() == 0) || s.equals(this.nullString))) {
            this.currentValue = null;
        } else {
            try {
                this.currentValue = cliStringToObject(s, this.requiredType,
                        null);
            } catch (Exception e) {
                throw new IllegalArgumentException("Problems with option: " + getName(), e);
            }
        }
    }

    public static String objectToCLIString(Object obj, Class<?> requiredType) {
        if (obj == null) {
            return "";
        }
        if (obj instanceof File) {
            return (FILE_PREFIX_STRING + ((File) obj).getPath());
        }
        if (obj instanceof String) {
            return (INMEM_PREFIX_STRING + obj);
        }
        String className = classToCLIString(obj.getClass(), requiredType);
        if (obj instanceof Configurable) {
            //Add cli parser
            JavaCLIParser config = new JavaCLIParser(obj, "");
            String subOptions = config.getOptions().getAsCLIString();
            //String subOptions = ((Configurable) obj).getOptions().getAsCLIString();
            if (subOptions.length() > 0) {
                return (className + " " + subOptions);
            }
        }
        return className;
    }

    public static <T> T createObject(String cliString,
            Class<T> requiredType) throws Exception {
        return cliStringToObject(cliString, requiredType, null);
    }
    
    public static <T> T createObject(String[] args,
            Class<T> requiredType) throws Exception {
            // build a single string by concatenating cli options
            StringBuilder cliString = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                cliString.append(" ").append(args[i]);
            }
            return cliStringToObject(cliString.toString(), requiredType, null);
    }
    
    public static <T> T cliStringToObject(String cliString,
            Class<T> requiredType, Option[] externalOptions) throws Exception {
       /* if (cliString.startsWith(FILE_PREFIX_STRING)) {
            return new File(cliString.substring(FILE_PREFIX_STRING.length()));
        }
        if (cliString.startsWith(INMEM_PREFIX_STRING)) {
            return cliString.substring(INMEM_PREFIX_STRING.length());
        }*/
        cliString = cliString.trim();
        int firstSpaceIndex = cliString.indexOf(' ', 0);
        String className;
        String classOptions;
        if (firstSpaceIndex > 0) {
            className = cliString.substring(0, firstSpaceIndex);
            classOptions = cliString.substring(firstSpaceIndex + 1, cliString.length());
            classOptions = classOptions.trim();
        } else {
            className = cliString;
            classOptions = "";
        }
        Class<?> classObject;
        try {
            classObject = Class.forName(className);
        } catch (Throwable t1) {
            try {
                // try prepending default package
                classObject = Class.forName(requiredType.getPackage().getName()
                        + "." + className);
            } catch (Throwable t3) {
                    throw new Exception("Class not found: " + className);
            }
        }
        Object classInstance;
        try {
            classInstance = classObject.newInstance();
        } catch (Exception ex) {
            throw new Exception("Problem creating instance of class: "
                    + className, ex);
        }
        if (requiredType.isInstance(classInstance)
                 ) {
            Options options = new Options();
            JavaCLIParser config = null;
            if (externalOptions != null) {
                for (Option option : externalOptions) {
                    options.addOption(option);
                }
            }
            if (classInstance instanceof Configurable) {
                 config = new JavaCLIParser(classInstance, "");
                 Option[] objectOptions = config.getOptions().getOptionArray();
                for (Option option : objectOptions) {
                    options.addOption(option);
                }
            }
            try {
                options.setViaCLIString(classOptions);
            } catch (Exception ex) {
                throw new Exception("Problem with options to '"
                        + className
                        + "'."
                        + "\n\nValid options for "
                        + className
                        + ":\n"
                        + config == null ? "": config.getOptions().getHelpString(), ex);
            } finally {
                options.removeAllOptions(); // clean up listener refs
            }
        } else {
            throw new Exception("Class named '" + className
                    + "' is not an instance of " + requiredType.getName() + ".");
        }
        return requiredType.cast(classInstance);
    }

}
