/*
 * Copyright (C) 2015 Holmes Team at HUAWEI Noah's Ark Lab.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.spark.streamdm.tasks

import java.io.Serializable

import com.github.javacliparser.Configurable
import org.apache.spark.streaming.StreamingContext

/**
 * Abstract Task. All runnable tasks in streamDM extend this class.
 *
 */
abstract class Task extends Configurable with Serializable{

  /**
   * Run the task.
   * @param ssc The Spark Streaming context in which the task is run.
   */
  def run(ssc:StreamingContext): Unit
}
