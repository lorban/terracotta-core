/*
 * Copyright (c) 2011-2018 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.tc.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ConcurrentIdentityHashMapTest {
  private HashMap<Integer, Weird> interned;

  @Before
  public void setUp() throws Exception {
    this.interned = new HashMap<Integer, Weird>();
  }

  @After
  public void tearDown() throws Exception {
    this.interned = null;
  }

  private Weird weird(int i) {
    Weird p = interned.get(i);
    if (p == null) {
      p = new Weird(i);
      interned.put(i, p);
    }
    return p;
  }

  @Test
  public void testSimple() {
    HashMap<Integer, Weird> interned = new HashMap<>();
    ConcurrentIdentityHashMap<Weird, Weird> map = new ConcurrentIdentityHashMap<Weird, Weird>();
    for (int i = 0; i < 100; i++) {
      Weird w = weird(i * 2);
      map.put(w, w);
    }
    assertThat(map.size(), is(100));
    for (int i = 0; i < 100; i++) {
      assertThat(map.containsKey(weird(i * 2)), is(true));
      assertThat(map.containsKey(weird(i * 2 + 1)), is(false));
      assertThat(map.containsKey(new Weird(i * 2 + 1)), is(false));
    }

  }

  static class Weird {
    private final Integer val;

    public Weird(int i) {
      this.val = i;
    }

    public int getValue() {
      return val.intValue();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(val);
    }
  }
}