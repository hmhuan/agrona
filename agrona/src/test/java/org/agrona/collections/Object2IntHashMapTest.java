/*
 * Copyright 2014-2022 Real Logic Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.agrona.collections;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.jupiter.api.Assertions.*;

class Object2IntHashMapTest
{
    static final int MISSING_VALUE = -1;
    final Object2IntHashMap<String> objectToIntMap;

    Object2IntHashMapTest()
    {
        objectToIntMap = newMap(Hashing.DEFAULT_LOAD_FACTOR, Object2IntHashMap.MIN_CAPACITY);
    }

    <T> Object2IntHashMap<T> newMap(final float loadFactor, final int initialCapacity)
    {
        return new Object2IntHashMap<>(initialCapacity, loadFactor, MISSING_VALUE);
    }

    @Test
    void shouldDoPutAndThenGet()
    {
        final String key = "Seven";
        objectToIntMap.put(key, 7);

        assertThat(objectToIntMap.get(key), is(7));
    }

    @Test
    void shouldReplaceExistingValueForTheSameKey()
    {
        final int value = 7;
        final String key = "Seven";
        objectToIntMap.put(key, value);

        final int newValue = 8;
        final int oldValue = objectToIntMap.put(key, newValue);

        assertThat(objectToIntMap.get(key), is(newValue));
        assertThat(oldValue, is(value));
        assertThat(objectToIntMap.size(), is(1));
    }

    @Test
    void shouldGrowWhenThresholdExceeded()
    {
        final float loadFactor = 0.5f;
        final int initialCapacity = 32;
        final Object2IntHashMap<String> map = newMap(loadFactor, initialCapacity);
        for (int i = 0; i < 16; i++)
        {
            map.put(Integer.toString(i), i);
        }

        assertThat(map.resizeThreshold(), is(16));
        assertThat(map.capacity(), is(initialCapacity));
        assertThat(map.size(), is(16));

        map.put("16", 16);

        assertThat(map.resizeThreshold(), is(initialCapacity));
        assertThat(map.capacity(), is(64));
        assertThat(map.size(), is(17));

        assertThat(map.getValue("16"), equalTo(16));
        assertThat((double)loadFactor, closeTo(map.loadFactor(), 0.0f));
    }


    @Test
    void shouldHandleCollisionAndThenLinearProbe()
    {
        final float loadFactor = 0.5f;
        final int initialCapacity = 32;
        final Object2IntHashMap<Integer> map = newMap(loadFactor, initialCapacity);
        final int value = 7;
        final Integer key = 7;
        map.put(key, value);

        final Integer collisionKey = key + map.capacity();
        final int collisionValue = collisionKey;
        map.put(collisionKey, collisionValue);

        assertThat(map.get(key), is(value));
        assertThat(map.get(collisionKey), is(collisionValue));
        assertThat((double)loadFactor, closeTo(map.loadFactor(), 0.0f));
    }

    @Test
    void shouldClearCollection()
    {
        for (int i = 0; i < 15; i++)
        {
            objectToIntMap.put(Integer.toString(i), i);
        }

        assertThat(objectToIntMap.size(), is(15));
        assertThat(objectToIntMap.getValue(Integer.toString(1)), is(1));

        objectToIntMap.clear();

        assertThat(objectToIntMap.size(), is(0));
        assertEquals(MISSING_VALUE, objectToIntMap.getValue("1"));
    }

    @Test
    void shouldCompactCollection()
    {
        final int totalItems = 50;
        for (int i = 0; i < totalItems; i++)
        {
            objectToIntMap.put(Integer.toString(i), i);
        }

        for (int i = 0, limit = totalItems - 4; i < limit; i++)
        {
            objectToIntMap.remove(Integer.toString(i));
        }

        final int capacityBeforeCompaction = objectToIntMap.capacity();
        objectToIntMap.compact();

        assertThat(objectToIntMap.capacity(), lessThan(capacityBeforeCompaction));
    }

    @Test
    void shouldContainValue()
    {
        final int value = 7;
        final String key = "Seven";

        objectToIntMap.put(key, value);

        assertTrue(objectToIntMap.containsValue(value));
        assertFalse(objectToIntMap.containsValue(8));
    }

    @Test
    void shouldContainKey()
    {
        final int value = 7;
        final String key = "Seven";

        objectToIntMap.put(key, value);

        assertTrue(objectToIntMap.containsKey(key));
        assertFalse(objectToIntMap.containsKey("Eight"));
    }

    @Test
    void shouldRemoveEntry()
    {
        final int value = 7;
        final String key = "Seven";

        objectToIntMap.put(key, value);

        assertTrue(objectToIntMap.containsKey(key));

        objectToIntMap.remove(key);

        assertFalse(objectToIntMap.containsKey(key));
    }

    @Test
    void shouldRemoveEntryAndCompactCollisionChain()
    {
        final float loadFactor = 0.5f;
        final Object2IntHashMap<Integer> objectToIntMap = new Object2IntHashMap<>(32, loadFactor, MISSING_VALUE);

        final int value = 12;
        final Integer key = 12;

        objectToIntMap.put(key, value);
        objectToIntMap.put(Integer.valueOf(13), 13);

        final int collisionKey = key + objectToIntMap.capacity();
        final int collisionValue = collisionKey;

        objectToIntMap.put(Integer.valueOf(collisionKey), collisionValue);
        objectToIntMap.put(Integer.valueOf(14), 14);

        assertThat(objectToIntMap.remove(key), is(value));
    }

    @Test
    void shouldIterateValuesGettingIntAsPrimitive()
    {
        final Collection<Integer> initialSet = new HashSet<>();

        for (int i = 0; i < 11; i++)
        {
            final String key = Integer.toString(i);
            objectToIntMap.put(key, i);
            initialSet.add(i);
        }

        final Collection<Integer> copyToSet = new HashSet<>();

        for (final Object2IntHashMap<String>.ValueIterator iter = objectToIntMap.values().iterator(); iter.hasNext(); )
        {
            copyToSet.add(iter.nextInt());
        }

        assertThat(copyToSet, is(initialSet));
    }

    @Test
    void shouldIterateValues()
    {
        final Collection<Integer> initialSet = new HashSet<>();

        for (int i = 0; i < 11; i++)
        {
            final String key = Integer.toString(i);
            objectToIntMap.put(key, i);
            initialSet.add(i);
        }

        final Collection<Integer> copyToSet = new HashSet<>();

        for (final Integer key : objectToIntMap.values())
        {
            //noinspection UseBulkOperation
            copyToSet.add(key);
        }

        assertThat(copyToSet, is(initialSet));
    }

    @Test
    void shouldIterateKeys()
    {
        final Collection<String> initialSet = new HashSet<>();

        for (int i = 0; i < 11; i++)
        {
            final String key = Integer.toString(i);
            objectToIntMap.put(key, i);
            initialSet.add(key);
        }

        assertIterateKeys(initialSet);
        assertIterateKeys(initialSet);
        assertIterateKeys(initialSet);
    }

    @Test
    void shouldIterateAndHandleRemove()
    {
        final Collection<String> initialSet = new HashSet<>();

        final int count = 11;
        for (int i = 0; i < count; i++)
        {
            final String key = Integer.toString(i);
            objectToIntMap.put(key, i);
            initialSet.add(key);
        }

        final Collection<String> copyOfSet = new HashSet<>();

        int i = 0;
        for (final Iterator<String> iter = objectToIntMap.keySet().iterator(); iter.hasNext(); )
        {
            final String item = iter.next();
            if (i++ == 7)
            {
                iter.remove();
            }
            else
            {
                copyOfSet.add(item);
            }
        }

        final int reducedSetSize = count - 1;
        assertThat(initialSet.size(), is(count));
        assertThat(objectToIntMap.size(), is(reducedSetSize));
        assertThat(copyOfSet.size(), is(reducedSetSize));
    }

    @Test
    void shouldIterateEntries()
    {
        final int count = 11;
        for (int i = 0; i < count; i++)
        {
            final String key = Integer.toString(i);
            objectToIntMap.put(key, i);
        }

        iterateEntries();
        iterateEntries();
        iterateEntries();

        final Integer testValue = 100;
        for (final Map.Entry<String, Integer> entry : objectToIntMap.entrySet())
        {
            assertThat(entry.getKey(), equalTo(String.valueOf(entry.getValue())));

            if (entry.getKey().equals("7"))
            {
                entry.setValue(testValue);
            }
        }

        assertThat(objectToIntMap.getValue("7"), equalTo(testValue));
    }


    @Test
    void shouldGenerateStringRepresentation()
    {
        final Object2IntHashMap<ControlledHash> objectToIntMap = new Object2IntHashMap<>(MISSING_VALUE);

        final ControlledHash[] testEntries = ControlledHash.create(3, 1, 19, 7, 11, 12, 7);

        for (final ControlledHash testEntry : testEntries)
        {
            objectToIntMap.put(testEntry, testEntry.value);
        }

        final String mapAsAString = "{7=7, 19=19, 11=11, 1=1, 12=12, 3=3}";
        assertThat(objectToIntMap.toString(), equalTo(mapAsAString));
    }

    @Test
    void shouldCopyConstructAndBeEqual()
    {
        final int[] testEntries = { 3, 1, 19, 7, 11, 12, 7 };

        for (final int testEntry : testEntries)
        {
            objectToIntMap.put(String.valueOf(testEntry), testEntry);
        }

        final Object2IntHashMap<String> mapCopy = new Object2IntHashMap<>(objectToIntMap);
        assertThat(mapCopy, is(objectToIntMap));
    }

    @Test
    void testToArray()
    {
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(-127);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        final Object[] array = map.entrySet().toArray();
        for (final Object entry : array)
        {
            map.remove(((Entry<?, ?>)entry).getKey());
        }

        assertTrue(map.isEmpty());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void testToArrayTyped()
    {
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(-127);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        final Entry<?, ?>[] type = new Entry[1];
        final Entry<?, ?>[] array = map.entrySet().toArray(type);
        for (final Entry<?, ?> entry : array)
        {
            map.remove(entry.getKey());
        }

        assertTrue(map.isEmpty());
    }

    @Test
    void testToArrayWithArrayListConstructor()
    {
        final Object2IntHashMap<String> map = new Object2IntHashMap<>(-127);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        final List<Map.Entry<String, Integer>> list = new ArrayList<>(map.entrySet());
        for (final Map.Entry<String, Integer> entry : list)
        {
            map.remove(entry.getKey());
        }

        assertTrue(map.isEmpty());
    }

    @Test
    void putReturnsMissingValueIfNewMappingIsAdded()
    {
        final Object2IntHashMap<TrickyKey> map = new Object2IntHashMap<>(MISSING_VALUE);

        assertEquals(MISSING_VALUE, map.put(new TrickyKey(42, 1), 10));
        assertEquals(MISSING_VALUE, map.put(new TrickyKey(42, 2), 20));
        assertEquals(MISSING_VALUE, map.put(new TrickyKey(42, 3), 30));
        assertEquals(3, map.size());
    }

    @Test
    void removeKeyReturnsMissingValueIfRemoveFails()
    {
        final int missingValue = -14;
        final Object2IntHashMap<TrickyKey> map = new Object2IntHashMap<>(missingValue);
        map.put(new TrickyKey(21, 1), 10);
        map.put(new TrickyKey(8, 2), 20);
        map.put(new TrickyKey(21, 3), 30);

        assertEquals(missingValue, map.removeKey(new TrickyKey(21, 21)));
        assertEquals(3, map.size());
        assertEquals(30, map.getValue(new TrickyKey(21, 3)));
    }

    @Test
    void removeReturnsNullIfRemoveFails()
    {
        final Object2IntHashMap<TrickyKey> map = new Object2IntHashMap<>(Integer.MIN_VALUE);
        map.put(new TrickyKey(21, 1), 10);

        assertNull(map.remove(new TrickyKey(21, 21)));
        assertEquals(1, map.size());
        assertEquals(10, map.getValue(new TrickyKey(21, 1)));
    }

    @Test
    void getValueReturnsMissingValueIfLookupFails()
    {
        final Object2IntHashMap<TrickyKey> map = new Object2IntHashMap<>(MISSING_VALUE);
        map.put(new TrickyKey(1, 1), 1);
        map.put(new TrickyKey(2, 2), 2);
        map.put(new TrickyKey(3, 3), 3);

        assertEquals(MISSING_VALUE, map.getValue(new TrickyKey(3, MISSING_VALUE)));
        assertEquals(2, map.getValue(new TrickyKey(2, 2)));
    }

    private void assertIterateKeys(final Collection<String> initialSet)
    {
        final Collection<String> copyToSet = new HashSet<>();
        for (final String aInteger : objectToIntMap.keySet())
        {
            //noinspection UseBulkOperation
            copyToSet.add(aInteger);
        }

        assertThat(copyToSet, is(initialSet));
    }

    private void iterateEntries()
    {
        for (final Map.Entry<String, Integer> entry : objectToIntMap.entrySet())
        {
            assertThat(entry.getKey(), equalTo(String.valueOf(entry.getValue())));
        }
    }

    private static class ControlledHash
    {
        private final int value;

        static ControlledHash[] create(final int... values)
        {
            final ControlledHash[] result = new ControlledHash[values.length];
            for (int i = 0; i < values.length; i++)
            {
                result[i] = new ControlledHash(values[i]);
            }

            return result;
        }

        ControlledHash(final int value)
        {
            super();
            this.value = value;
        }

        public String toString()
        {
            return Integer.toString(value);
        }

        public int hashCode()
        {
            return value * 31;
        }

        public boolean equals(final Object obj)
        {
            if (this == obj)
            {
                return true;
            }

            if (obj == null)
            {
                return false;
            }

            if (getClass() != obj.getClass())
            {
                return false;
            }

            final ControlledHash other = (ControlledHash)obj;

            return value == other.value;
        }
    }

    private static final class TrickyKey
    {
        private final int hash;
        private final int value;

        private TrickyKey(final int hash, final int value)
        {
            this.hash = hash;
            this.value = value;
        }

        public int hashCode()
        {
            return hash;
        }

        public boolean equals(final Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            return obj instanceof TrickyKey && value == ((TrickyKey)obj).value;
        }

        public String toString()
        {
            return "TrickyKey{" +
                "hash=" + hash +
                ", value=" + value +
                '}';
        }
    }
}
