/**
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

/* rich Mar 3, 2008 */

package com.github.krukow.clj_lang;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.github.krukow.clj_ds.PersistentMap;
import com.github.krukow.clj_ds.PersistentSet;

public abstract class APersistentSet<T> implements PersistentSet<T>, Collection<T>, Set<T>, Serializable {
    int _hash = -1;

    final PersistentMap<T, Boolean> impl;

    protected APersistentSet(PersistentMap<T, Boolean> impl) {
        this.impl = impl;
    }

    public String toString() {
        return RT.printString(this);
    }

    public boolean contains(Object key) {
        return impl.containsKey(key);
    }

    public Boolean get(T key) {
        return (Boolean) impl.get(key);
    }

    public int count() {
        return impl.size();
    }

    public ISeq<T> seq() {
        return RT.keys(impl);
    }

    public Object invoke(Object arg1) {
        return get((T) arg1);
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Set))
            return false;
        Set m = (Set) obj;

        if (m.size() != count())
            return false;

        for (Object aM : m)
        {
            if (!contains(aM))
                return false;
        }

        return true;
    }

    public boolean equiv(Object o) {
        return equals(o);
    }

    public int hashCode() {
        if (_hash == -1)
        {
            // int hash = count();
            int hash = 0;
            for (Object e : this)
            {
                hash += Util.hash(e);
            }
            this._hash = hash;
        }
        return _hash;
    }

    public Object[] toArray() {
        return RT.seqToArray(this);
    }

    public boolean add(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public boolean containsAll(Collection c) {
        for (Object o : c)
        {
            if (!contains(o))
                return false;
        }
        return true;
    }

    public Object[] toArray(Object[] a) {
        return RT.seqToPassedArray(this, a);
    }

    public int size() {
        return count();
    }

    public boolean isEmpty() {
        return count() == 0;
    }

    public Iterator<T> iterator() {
        return new SeqIterator<T>(seq());
    }

}
