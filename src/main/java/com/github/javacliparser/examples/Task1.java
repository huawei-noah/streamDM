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


public class Task1 implements Configurable {

    public IntOption ensembleSizeOption = new IntOption("ensembleSize", 's',
            "The number of models in the bag.", 10, 1, Integer.MAX_VALUE);
    
    public ClassOption taskClassOption = new ClassOption("taskClass", 'l',
            "Task to execute.", Task2.class, "Task2 -s 100");

    public void init() {
        System.out.println("Task1:" + this.ensembleSizeOption.getValue());
        Task2 task = this.taskClassOption.getValue();
        task.init();
    }
}
