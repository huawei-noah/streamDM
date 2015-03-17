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

package com.github.javacliparser.examples;


import com.github.javacliparser.ClassOption;
import com.github.javacliparser.Configurable;
import com.github.javacliparser.IntOption;


public class DoTask implements Configurable { 

   public IntOption sizeOption = new IntOption("size", 's',
            "Size", 10, 1, Integer.MAX_VALUE);

   public ClassOption taskClassOption = new ClassOption("taskClass", 'l',
            "Task to execute.", Task1.class, "Task1 -s 4");
      
   public void init() {
        System.out.println(this.sizeOption.getValue());
        Task1 task = this.taskClassOption.getValue();
        task.init();
   }
    public static void main(String[] args) throws Exception {
        //Main main = ClassOption.createObject(args, Main.class);
        DoTask main =  ClassOption.createObject("DoTask -s 5 -l (Task1 -s 3 -l (Task2 -s 1))", DoTask.class);
        main.init();
    }
    
    
}
