/**
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

/* rich Mar 25, 2006 4:28:27 PM */

package com.github.krukow.clj_lang;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.regex.Matcher;

public class RT {
//
//	static final public Boolean T = Boolean.TRUE;// Keyword.intern(Symbol.create(null, "t"));
//	static final public Boolean F = Boolean.FALSE;// Keyword.intern(Symbol.create(null, "t"));
//	// single instance of UTF-8 Charset, so as to avoid catching
//	// UnsupportedCharsetExceptions everywhere
//	static public Charset UTF8 = Charset.forName("UTF-8");
//
	static volatile boolean readably = true;
//
	static public final Object[] EMPTY_ARRAY = new Object[] {};
	static public final Comparator DEFAULT_COMPARATOR = new DefaultComparator();

	private static final class DefaultComparator implements Comparator<Comparable>, Serializable {
		public int compare(Comparable o1, Comparable o2) {
		    return o1.compareTo(o2);
		}

		private Object readResolve() throws ObjectStreamException {
			// ensures that we aren't hanging onto a new default comparator for every
			// sorted set, etc., we deserialize
			return DEFAULT_COMPARATOR;
		}
	}
//
//	// //////////// Collections support /////////////////////////////////
//
	static public ISeq seq(Object coll) {
		if (coll instanceof ASeq)
			return (ASeq) coll;
		else
			return seqFrom(coll);
	}

	static ISeq seqFrom(Object coll) {
		if (coll instanceof Seqable)
			return ((Seqable) coll).seq();
		else if (coll == null)
			return null;
		else if (coll instanceof Iterable)
			return IteratorSeq.create(((Iterable) coll).iterator());
		else if (coll instanceof Map)
			return seq(((Map) coll).entrySet());
		else {
			Class c = coll.getClass();
			Class sc = c.getSuperclass();
			throw new IllegalArgumentException("Don't know how to create ISeq from: " + c.getName());
		}
	}

	static public ISeq keys(Object coll) {
		return APersistentMap.KeySeq.create(seq(coll));
	}

	public static int count(Object o) {
		if (o instanceof Counted)
			return ((Counted) o).count();
		return countFrom(Util.ret1(o, o = null));
	}

	static int countFrom(Object o) {
		if (o == null)
			return 0;
		else if (o instanceof IPersistentCollection) {
			ISeq s = seq(o);
			o = null;
			int i = 0;
			for (; s != null; s = s.next()) {
				if (s instanceof Counted)
					return i + s.count();
				i++;
			}
			return i;
		} else if (o instanceof CharSequence)
			return ((CharSequence) o).length();
		else if (o instanceof Collection)
			return ((Collection) o).size();
		else if (o instanceof Map)
			return ((Map) o).size();
		else if (o.getClass().isArray())
			return Array.getLength(o);

		throw new UnsupportedOperationException("count not supported on this type: " + o.getClass().getSimpleName());
	}
//
//	static public IPersistentCollection conj(IPersistentCollection coll,
//			Object x) {
//		if (coll == null)
//			return new PersistentList(x);
//		return coll.cons(x);
//	}

	static public ISeq cons(Object x, Object coll) {
		// ISeq y = seq(coll);
		if (coll == null)
			return new PersistentList(x);
		else if (coll instanceof ISeq)
			return new Cons(x, (ISeq) coll);
		else
			return new Cons(x, seq(coll));
	}

	static public Object first(Object x) {
		if (x instanceof ISeq)
			return ((ISeq) x).first();
		ISeq seq = seq(x);
		if (seq == null)
			return null;
		return seq.first();
	}

	static public Object second(Object x) {
		return first(next(x));
	}

	static public ISeq next(Object x) {
		if (x instanceof ISeq)
			return ((ISeq) x).next();
		ISeq seq = seq(x);
		if (seq == null)
			return null;
		return seq.next();
	}

	static public Object nth(Object coll, int n) {
		if (coll instanceof Indexed)
			return ((Indexed) coll).nth(n);
		return nthFrom(Util.ret1(coll, coll = null), n);
	}

	static Object nthFrom(Object coll, int n) {
		if (coll == null)
			return null;
		else if (coll instanceof CharSequence)
			return Character.valueOf(((CharSequence) coll).charAt(n));
		else if (coll.getClass().isArray())
			return Reflector.prepRet(coll.getClass().getComponentType(),Array.get(coll, n));
		else if (coll instanceof RandomAccess)
			return ((List) coll).get(n);
		else if (coll instanceof Matcher)
			return ((Matcher) coll).group(n);

		else if (coll instanceof Map.Entry) {
			Map.Entry e = (Map.Entry) coll;
			if (n == 0)
				return e.getKey();
			else if (n == 1)
				return e.getValue();
			throw new IndexOutOfBoundsException();
		}

		else if (coll instanceof Sequential) {
			ISeq seq = RT.seq(coll);
			coll = null;
			for (int i = 0; i <= n && seq != null; ++i, seq = seq.next()) {
				if (i == n)
					return seq.first();
			}
			throw new IndexOutOfBoundsException();
		} else
			throw new UnsupportedOperationException("nth not supported on this type: " + coll.getClass().getSimpleName());
	}

	static public IPersistentVector subvec(IPersistentVector v, int start,
			int end) {
		if (end < start || start < 0 || end > v.count())
			throw new IndexOutOfBoundsException();
		if (start == end)
			return PersistentVector.EMPTY;
		return new APersistentVector.SubVector(v, start, end);
	}
//
//	/**
//	 * **************************************** list support
//	 * *******************************
//	 */

	static public ISeq list(Object arg1) {
		return new PersistentList(arg1);
	}

	static public Object[] seqToArray(ISeq seq) {
		int len = length(seq);
		Object[] ret = new Object[len];
		for (int i = 0; seq != null; ++i, seq = seq.next())
			ret[i] = seq.first();
		return ret;
	}

    // supports java Collection.toArray(T[])
    static public Object[] seqToPassedArray(ISeq seq, Object[] passed){
        Object[] dest = passed;
        int len = count(seq);
        if (len > dest.length) {
            dest = (Object[]) Array.newInstance(passed.getClass().getComponentType(), len);
        }
        for(int i = 0; seq != null; ++i, seq = seq.next())
            dest[i] = seq.first();
        if (len < passed.length) {
            dest[len] = null;
        }
        return dest;
    }

	static public int length(ISeq list) {
		int i = 0;
		for (ISeq c = list; c != null; c = c.next()) {
			i++;
		}
		return i;
	}

	static public boolean isReduced(Object r){
		return r instanceof Reduced;
	}

	static public String printString(Object x) {
		try {
			StringWriter sw = new StringWriter();
			print(x, sw);
			return sw.toString();
		}
		catch(Exception e) {
			throw Util.sneakyThrow(e);
		}
	}

	static public void print(Object x, Writer w) throws IOException {
		if (x == null)
			w.write("null");
		else if (x instanceof ISeq || x instanceof IPersistentList) {
			w.write('(');
			printInnerSeq(seq(x), w);
			w.write(')');
		} else if (x instanceof String) {
			String s = (String) x;
			if (!readably)
				w.write(s);
			else {
				w.write('"');
				for (int i = 0; i < s.length(); i++) {
					char c = s.charAt(i);
					switch (c) {
					case '\n':
						w.write("\\n");
						break;
					case '\t':
						w.write("\\t");
						break;
					case '\r':
						w.write("\\r");
						break;
					case '"':
						w.write("\\\"");
						break;
					case '\\':
						w.write("\\\\");
						break;
					case '\f':
						w.write("\\f");
						break;
					case '\b':
						w.write("\\b");
						break;
					default:
						w.write(c);
					}
				}
				w.write('"');
			}
		} else if (x instanceof IPersistentMap) {
			w.write('{');
			for (ISeq s = seq(x); s != null; s = s.next()) {
				IMapEntry e = (IMapEntry) s.first();
				print(e.key(), w);
				w.write(' ');
				print(e.val(), w);
				if (s.next() != null)
					w.write(", ");
			}
			w.write('}');
		} else if (x instanceof IPersistentVector) {
			IPersistentVector a = (IPersistentVector) x;
			w.write('[');
			for (int i = 0; i < a.count(); i++) {
				print(a.nth(i), w);
				if (i < a.count() - 1)
					w.write(' ');
			}
			w.write(']');
		} else if (x instanceof IPersistentSet) {
			w.write("#{");
			for (ISeq s = seq(x); s != null; s = s.next()) {
				print(s.first(), w);
				if (s.next() != null)
					w.write(" ");
			}
			w.write('}');
		} else if (x instanceof Character) {
			char c = ((Character) x).charValue();
			if (!readably)
				w.write(c);
			else {
				w.write('\\');
				switch (c) {
				case '\n':
					w.write("newline");
					break;
				case '\t':
					w.write("tab");
					break;
				case ' ':
					w.write("space");
					break;
				case '\b':
					w.write("backspace");
					break;
				case '\f':
					w.write("formfeed");
					break;
				case '\r':
					w.write("return");
					break;
				default:
					w.write(c);
				}
			}
		} else if (x instanceof Class) {
			w.write("#=");
			w.write(((Class) x).getName());
		} else if (x instanceof BigDecimal && readably) {
			w.write(x.toString());
			w.write('M');
		} else if(x instanceof BigInteger && readably) {
				w.write(x.toString());
				w.write("BIGINT");
		} else
			w.write(x.toString());
	}

	private static void printInnerSeq(ISeq x, Writer w) throws IOException {
		for (ISeq s = x; s != null; s = s.next()) {
			print(s.first(), w);
			if (s.next() != null)
				w.write(' ');
		}
	}
}
