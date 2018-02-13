/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.util.concurrent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import com.tc.async.impl.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueFactory {

  private static final Logger logger = LoggerFactory.getLogger(QueueFactory.class);

  public static final int REASONABLE_MAX_QUEUE_SIZE = 10240;

  public <E> BlockingQueue<Event> createInstance(Class<E> type) {
    return createInstance(type, REASONABLE_MAX_QUEUE_SIZE + 1);
  }

  public <E> BlockingQueue<Event> createInstance(Class<E> type, int capacity) {
    if (capacity > REASONABLE_MAX_QUEUE_SIZE) {
      logger.info("Queue creation request for {} of max {} entries is not reasonable, clamping at {}", type, capacity, REASONABLE_MAX_QUEUE_SIZE);
      capacity = REASONABLE_MAX_QUEUE_SIZE;
    }
    return new ArrayBlockingQueue<>(capacity);
  }
}
