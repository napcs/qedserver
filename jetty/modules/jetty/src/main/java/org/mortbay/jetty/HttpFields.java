// ========================================================================
// Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.jetty;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.servlet.http.Cookie;

import org.mortbay.io.Buffer;
import org.mortbay.io.BufferCache;
import org.mortbay.io.BufferDateCache;
import org.mortbay.io.BufferUtil;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.View;
import org.mortbay.io.BufferCache.CachedBuffer;
import org.mortbay.util.LazyList;
import org.mortbay.util.QuotedStringTokenizer;
import org.mortbay.util.StringMap;
import org.mortbay.util.StringUtil;
import org.mortbay.util.URIUtil;

/* ------------------------------------------------------------ */
/**
 * HTTP Fields. A collection of HTTP header and or Trailer fields. This class is not synchronized
 * and needs to be protected from concurrent access.
 * 
 * This class is not synchronized as it is expected that modifications will only be performed by a
 * single thread.
 * 
 * @author Greg Wilkins (gregw)
 */
public class HttpFields
{
    /* ------------------------------------------------------------ */
    public final static String __separators = ", \t";

    /* ------------------------------------------------------------ */
    private static String[] DAYS =
    { "Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    private static String[] MONTHS =
    { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan"};

    /* ------------------------------------------------------------ */
    /**
     * Format HTTP date "EEE, dd MMM yyyy HH:mm:ss 'GMT'" or "EEE, dd-MMM-yy HH:mm:ss 'GMT'"for
     * cookies
     */
    public static String formatDate(long date, boolean cookie)
    {
        StringBuffer buf = new StringBuffer(32);
        GregorianCalendar gc = new GregorianCalendar(__GMT);
        gc.setTimeInMillis(date);
        formatDate(buf, gc, cookie);
        return buf.toString();
    }

    /* ------------------------------------------------------------ */
    /**
     * Format HTTP date "EEE, dd MMM yyyy HH:mm:ss 'GMT'" or "EEE, dd-MMM-yy HH:mm:ss 'GMT'"for
     * cookies
     */
    public static String formatDate(Calendar calendar, boolean cookie)
    {
        StringBuffer buf = new StringBuffer(32);
        formatDate(buf, calendar, cookie);
        return buf.toString();
    }

    /* ------------------------------------------------------------ */
    /**
     * Format HTTP date "EEE, dd MMM yyyy HH:mm:ss 'GMT'" or "EEE, dd-MMM-yy HH:mm:ss 'GMT'"for
     * cookies
     */
    public static String formatDate(StringBuffer buf, long date, boolean cookie)
    {
        GregorianCalendar gc = new GregorianCalendar(__GMT);
        gc.setTimeInMillis(date);
        formatDate(buf, gc, cookie);
        return buf.toString();
    }

    /* ------------------------------------------------------------ */
    /**
     * Format HTTP date "EEE, dd MMM yyyy HH:mm:ss 'GMT'" or "EEE, dd-MMM-yy HH:mm:ss 'GMT'"for
     * cookies
     */
    public static void formatDate(StringBuffer buf, Calendar calendar, boolean cookie)
    {
        // "EEE, dd MMM yyyy HH:mm:ss 'GMT'"
        // "EEE, dd-MMM-yy HH:mm:ss 'GMT'", cookie

        int day_of_week = calendar.get(Calendar.DAY_OF_WEEK);
        int day_of_month = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        int century = year / 100;
        year = year % 100;

        int epoch = (int) ((calendar.getTimeInMillis() / 1000) % (60 * 60 * 24));
        int seconds = epoch % 60;
        epoch = epoch / 60;
        int minutes = epoch % 60;
        int hours = epoch / 60;

        buf.append(DAYS[day_of_week]);
        buf.append(',');
        buf.append(' ');
        StringUtil.append2digits(buf, day_of_month);

        if (cookie)
        {
            buf.append('-');
            buf.append(MONTHS[month]);
            buf.append('-');
            StringUtil.append2digits(buf, century);
            StringUtil.append2digits(buf, year);
        }
        else
        {
            buf.append(' ');
            buf.append(MONTHS[month]);
            buf.append(' ');
            StringUtil.append2digits(buf, century);
            StringUtil.append2digits(buf, year);
        }
        buf.append(' ');
        StringUtil.append2digits(buf, hours);
        buf.append(':');
        StringUtil.append2digits(buf, minutes);
        buf.append(':');
        StringUtil.append2digits(buf, seconds);
        buf.append(" GMT");
    }

    /* -------------------------------------------------------------- */
    private static TimeZone __GMT = TimeZone.getTimeZone("GMT");
    public final static BufferDateCache __dateCache = new BufferDateCache("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);

    /* ------------------------------------------------------------ */
    private final static String __dateReceiveFmt[] =
    {   "EEE, dd MMM yyyy HH:mm:ss zzz", 
        "EEE, dd-MMM-yy HH:mm:ss",
        "EEE MMM dd HH:mm:ss yyyy",
        
        "EEE, dd MMM yyyy HH:mm:ss", "EEE dd MMM yyyy HH:mm:ss zzz", 
        "EEE dd MMM yyyy HH:mm:ss", "EEE MMM dd yyyy HH:mm:ss zzz", "EEE MMM dd yyyy HH:mm:ss", 
        "EEE MMM-dd-yyyy HH:mm:ss zzz", "EEE MMM-dd-yyyy HH:mm:ss", "dd MMM yyyy HH:mm:ss zzz", 
        "dd MMM yyyy HH:mm:ss", "dd-MMM-yy HH:mm:ss zzz", "dd-MMM-yy HH:mm:ss", "MMM dd HH:mm:ss yyyy zzz", 
        "MMM dd HH:mm:ss yyyy", "EEE MMM dd HH:mm:ss yyyy zzz",  
        "EEE, MMM dd HH:mm:ss yyyy zzz", "EEE, MMM dd HH:mm:ss yyyy", "EEE, dd-MMM-yy HH:mm:ss zzz", 
        "EEE dd-MMM-yy HH:mm:ss zzz", "EEE dd-MMM-yy HH:mm:ss",
      };
    private  static int __dateReceiveInit=3;
    private  static SimpleDateFormat __dateReceive[];
    static
    {
        __GMT.setID("GMT");
        __dateCache.setTimeZone(__GMT);
        __dateReceive = new SimpleDateFormat[__dateReceiveFmt.length];
        // Initialize only the standard formats here.
        for (int i = 0; i < __dateReceiveInit; i++)
        {
            __dateReceive[i] = new SimpleDateFormat(__dateReceiveFmt[i], Locale.US);
            __dateReceive[i].setTimeZone(__GMT);
        }
    }
    public final static String __01Jan1970 = formatDate(0, true).trim();
    public final static Buffer __01Jan1970_BUFFER = new ByteArrayBuffer(__01Jan1970);

    /* -------------------------------------------------------------- */
    protected ArrayList _fields = new ArrayList(20);
    protected int _revision;
    protected HashMap _bufferMap = new HashMap(32);
    protected SimpleDateFormat _dateReceive[] = new SimpleDateFormat[__dateReceive.length];
    private StringBuffer _dateBuffer;
    private Calendar _calendar;

    /* ------------------------------------------------------------ */
    /**
     * Constructor.
     */
    public HttpFields()
    {
    }

    /* -------------------------------------------------------------- */
    /**
     * Get enumeration of header _names. Returns an enumeration of strings representing the header
     * _names for this request.
     */
    public Enumeration getFieldNames()
    {
        final int revision=_revision;
        return new Enumeration()
        {
            int i = 0;
            Field field = null;

            public boolean hasMoreElements()
            {
                if (field != null) return true;
                while (i < _fields.size())
                {
                    Field f = (Field) _fields.get(i++);
                    if (f != null && f._prev == null && f._revision == revision)
                    {
                        field = f;
                        return true;
                    }
                }
                return false;
            }

            public Object nextElement() throws NoSuchElementException
            {
                if (field != null || hasMoreElements())
                {
                    String n = BufferUtil.to8859_1_String(field._name);
                    field = null;
                    return n;
                }
                throw new NoSuchElementException();
            }
        };
    }

    /* -------------------------------------------------------------- */
    /**
     * Get enumeration of Fields Returns an enumeration of Fields for this request.
     */
    public Iterator getFields()
    {
        final int revision=_revision;
        return new Iterator()
        {
            int i = 0;
            Field field = null;

            public boolean hasNext()
            {
                if (field != null) return true;
                while (i < _fields.size())
                {
                    Field f = (Field) _fields.get(i++);
                    if (f != null && f._revision == revision)
                    {
                        field = f;
                        return true;
                    }
                }
                return false;
            }

            public Object next()
            {
                if (field != null || hasNext())
                {
                    final Field f = field;
                    field = null;
                    return f;
                }
                throw new NoSuchElementException();
            }

            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    /* ------------------------------------------------------------ */
    private Field getField(String name)
    {
        return (Field) _bufferMap.get(HttpHeaders.CACHE.lookup(name));
    }

    /* ------------------------------------------------------------ */
    private Field getField(Buffer name)
    {
        return (Field) _bufferMap.get(name);
    }

    /* ------------------------------------------------------------ */
    public boolean containsKey(Buffer name)
    {
        Field f = getField(name);
        return (f != null && f._revision == _revision); 
    }

    /* ------------------------------------------------------------ */
    public boolean containsKey(String name)
    {
        Field f = getField(name);
        return (f != null && f._revision == _revision); 
    }

    /* -------------------------------------------------------------- */
    /**
     * @return the value of a field, or null if not found. For multiple fields of the same name,
     *         only the first is returned.
     * @param name the case-insensitive field name
     */
    public String getStringField(String name)
    {
        // TODO - really reuse strings from previous requests!
        Field field = getField(name);
        if (field != null && field._revision == _revision) return field.getValue();
        return null;
    }

    /* -------------------------------------------------------------- */
    /**
     * @return the value of a field, or null if not found. For multiple fields of the same name,
     *         only the first is returned.
     * @param name the case-insensitive field name
     */
    public String getStringField(Buffer name)
    {
        // TODO - really reuse strings from previous requests!
        Field field = getField(name);
        if (field != null && field._revision == _revision) 
            return BufferUtil.to8859_1_String(field._value);
        return null;
    }

    /* -------------------------------------------------------------- */
    /**
     * @return the value of a field, or null if not found. For multiple fields of the same name,
     *         only the first is returned.
     * @param name the case-insensitive field name
     */
    public Buffer get(Buffer name)
    {
        Field field = getField(name);
        if (field != null && field._revision == _revision) 
            return field._value;
        return null;
    }

    /* -------------------------------------------------------------- */
    /**
     * Get multi headers
     * 
     * @return Enumeration of the values, or null if no such header.
     * @param name the case-insensitive field name
     */
    public Enumeration getValues(String name)
    {
        final Field field = getField(name);
        if (field == null) 
            return null;
        final int revision=_revision;

        return new Enumeration()
        {
            Field f = field;

            public boolean hasMoreElements()
            {
                while (f != null && f._revision != revision)
                    f = f._next;
                return f != null;
            }

            public Object nextElement() throws NoSuchElementException
            {
                if (f == null) throw new NoSuchElementException();
                Field n = f;
                do
                    f = f._next;
                while (f != null && f._revision != revision);
                return n.getValue();
            }
        };
    }

    /* -------------------------------------------------------------- */
    /**
     * Get multi headers
     * 
     * @return Enumeration of the value Strings, or null if no such header.
     * @param name the case-insensitive field name
     */
    public Enumeration getValues(Buffer name)
    {
        final Field field = getField(name);
        if (field == null) 
            return null;
        final int revision=_revision;

        return new Enumeration()
        {
            Field f = field;

            public boolean hasMoreElements()
            {
                while (f != null && f._revision != revision)
                    f = f._next;
                return f != null;
            }

            public Object nextElement() throws NoSuchElementException
            {
                if (f == null) throw new NoSuchElementException();
                Field n = f;
                f = f._next;
                while (f != null && f._revision != revision)
                    f = f._next;
                return n.getValue();
            }
        };
    }

    /* -------------------------------------------------------------- */
    /**
     * Get multi field values with separator. The multiple values can be represented as separate
     * headers of the same name, or by a single header using the separator(s), or a combination of
     * both. Separators may be quoted.
     * 
     * @param name the case-insensitive field name
     * @param separators String of separators.
     * @return Enumeration of the values, or null if no such header.
     */
    public Enumeration getValues(String name, final String separators)
    {
        final Enumeration e = getValues(name);
        if (e == null) 
            return null;
        return new Enumeration()
        {
            QuotedStringTokenizer tok = null;

            public boolean hasMoreElements()
            {
                if (tok != null && tok.hasMoreElements()) return true;
                while (e.hasMoreElements())
                {
                    String value = (String) e.nextElement();
                    tok = new QuotedStringTokenizer(value, separators, false, false);
                    if (tok.hasMoreElements()) return true;
                }
                tok = null;
                return false;
            }

            public Object nextElement() throws NoSuchElementException
            {
                if (!hasMoreElements()) throw new NoSuchElementException();
                String next = (String) tok.nextElement();
                if (next != null) next = next.trim();
                return next;
            }
        };
    }

    /* -------------------------------------------------------------- */
    /**
     * Set a field.
     * 
     * @param name the name of the field
     * @param value the value of the field. If null the field is cleared.
     */
    public void put(String name, String value)
    {
        Buffer n = HttpHeaders.CACHE.lookup(name);
        Buffer v = null;
        if (value != null)
            v = HttpHeaderValues.CACHE.lookup(value);
        put(n, v, -1);
    }

    /* -------------------------------------------------------------- */
    /**
     * Set a field.
     * 
     * @param name the name of the field
     * @param value the value of the field. If null the field is cleared.
     */
    public void put(Buffer name, String value)
    {
        Buffer v = HttpHeaderValues.CACHE.lookup(value);
        put(name, v, -1);
    }

    /* -------------------------------------------------------------- */
    /**
     * Set a field.
     * 
     * @param name the name of the field
     * @param value the value of the field. If null the field is cleared.
     */
    public void put(Buffer name, Buffer value)
    {
        put(name, value, -1);
    }

    /* -------------------------------------------------------------- */
    /**
     * Set a field.
     * 
     * @param name the name of the field
     * @param value the value of the field. If null the field is cleared.
     * @param numValue the numeric value of the field (must match value) or -1
     */
    public void put(Buffer name, Buffer value, long numValue)
    {
        if (value == null)
        {
            remove(name);
            return;
        }

        if (!(name instanceof BufferCache.CachedBuffer)) name = HttpHeaders.CACHE.lookup(name);

        Field field = (Field) _bufferMap.get(name);

        // Look for value to replace.
        if (field != null)
        {
            field.reset(value, numValue, _revision);
            field = field._next;
            while (field != null)
            {
                field.clear();
                field = field._next;
            }
            return;
        }
        else
        {
            // new value;
            field = new Field(name, value, numValue, _revision);
            _fields.add(field);
            _bufferMap.put(field.getNameBuffer(), field);
        }
    }

    /* -------------------------------------------------------------- */
    /**
     * Set a field.
     * 
     * @param name the name of the field
     * @param list the List value of the field. If null the field is cleared.
     */
    public void put(String name, List list)
    {
        if (list == null || list.size() == 0)
        {
            remove(name);
            return;
        }
        Buffer n = HttpHeaders.CACHE.lookup(name);

        Object v = list.get(0);
        if (v != null)
            put(n, HttpHeaderValues.CACHE.lookup(v.toString()));
        else
            remove(n);

        if (list.size() > 1)
        {
            java.util.Iterator iter = list.iterator();
            iter.next();
            while (iter.hasNext())
            {
                v = iter.next();
                if (v != null) put(n, HttpHeaderValues.CACHE.lookup(v.toString()));
            }
        }
    }

    /* -------------------------------------------------------------- */
    /**
     * Add to or set a field. If the field is allowed to have multiple values, add will add multiple
     * headers of the same name.
     * 
     * @param name the name of the field
     * @param value the value of the field.
     * @exception IllegalArgumentException If the name is a single valued field and already has a
     *                value.
     */
    public void add(String name, String value) throws IllegalArgumentException
    {
        Buffer n = HttpHeaders.CACHE.lookup(name);
        Buffer v = HttpHeaderValues.CACHE.lookup(value);
        add(n, v, -1);
    }

    /* -------------------------------------------------------------- */
    /**
     * Add to or set a field. If the field is allowed to have multiple values, add will add multiple
     * headers of the same name.
     * 
     * @param name the name of the field
     * @param value the value of the field.
     * @exception IllegalArgumentException If the name is a single valued field and already has a
     *                value.
     */
    public void add(Buffer name, Buffer value) throws IllegalArgumentException
    {
        add(name, value, -1);
    }

    /* -------------------------------------------------------------- */
    /**
     * Add to or set a field. If the field is allowed to have multiple values, add will add multiple
     * headers of the same name.
     * 
     * @param name the name of the field
     * @param value the value of the field.
     * @exception IllegalArgumentException If the name is a single valued field and already has a
     *                value.
     */
    private void add(Buffer name, Buffer value, long numValue) throws IllegalArgumentException
    {
        if (value == null) throw new IllegalArgumentException("null value");

        if (!(name instanceof BufferCache.CachedBuffer)) name = HttpHeaders.CACHE.lookup(name);
        
        Field field = (Field) _bufferMap.get(name);
        Field last = null;
        if (field != null)
        {
            while (field != null && field._revision == _revision)
            {
                last = field;
                field = field._next;
            }
        }

        if (field != null)
            field.reset(value, numValue, _revision);
        else
        {
            // create the field
            field = new Field(name, value, numValue, _revision);

            // look for chain to add too
            if (last != null)
            {
                field._prev = last;
                last._next = field;
            }
            else
                _bufferMap.put(field.getNameBuffer(), field);

            _fields.add(field);
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * Remove a field.
     * 
     * @param name
     */
    public void remove(String name)
    {
        remove(HttpHeaders.CACHE.lookup(name));
    }

    /* ------------------------------------------------------------ */
    /**
     * Remove a field.
     * 
     * @param name
     */
    public void remove(Buffer name)
    {
        Field field = (Field) _bufferMap.get(name);

        if (field != null)
        {
            while (field != null)
            {
                field.clear();
                field = field._next;
            }
        }
    }

    /* -------------------------------------------------------------- */
    /**
     * Get a header as an long value. Returns the value of an integer field or -1 if not found. The
     * case of the field name is ignored.
     * 
     * @param name the case-insensitive field name
     * @exception NumberFormatException If bad long found
     */
    public long getLongField(String name) throws NumberFormatException
    {
        Field field = getField(name);
        if (field != null && field._revision == _revision) return field.getLongValue();

        return -1L;
    }

    /* -------------------------------------------------------------- */
    /**
     * Get a header as an long value. Returns the value of an integer field or -1 if not found. The
     * case of the field name is ignored.
     * 
     * @param name the case-insensitive field name
     * @exception NumberFormatException If bad long found
     */
    public long getLongField(Buffer name) throws NumberFormatException
    {
        Field field = getField(name);
        if (field != null && field._revision == _revision) return field.getLongValue();
        return -1L;
    }

    /* -------------------------------------------------------------- */
    /**
     * Get a header as a date value. Returns the value of a date field, or -1 if not found. The case
     * of the field name is ignored.
     * 
     * @param name the case-insensitive field name
     */
    public long getDateField(String name)
    {
        Field field = getField(name);
        if (field == null || field._revision != _revision) return -1;

        if (field._numValue != -1) return field._numValue;

        String val = valueParameters(BufferUtil.to8859_1_String(field._value), null);
        if (val == null) return -1;

        
        
        for (int i = 0; i < __dateReceiveInit; i++)
        {
            if (_dateReceive[i] == null) _dateReceive[i] = (SimpleDateFormat) __dateReceive[i].clone();

            try
            {
                Date date = (Date) _dateReceive[i].parseObject(val);
                return field._numValue = date.getTime();
            }
            catch (java.lang.Exception e)
            {
            }
        }
        if (val.endsWith(" GMT"))
        {
            val = val.substring(0, val.length() - 4);
            for (int i = 0; i < __dateReceiveInit; i++)
            {
                try
                {
                    Date date = (Date) _dateReceive[i].parseObject(val);
                    return field._numValue = date.getTime();
                }
                catch (java.lang.Exception e)
                {
                }
            }
        }
        
        // The standard formats did not work.  So we will lock the common format array
        // and look at lazily creating the non-standard formats
        synchronized (__dateReceive)
        {
            for (int i = __dateReceiveInit; i < _dateReceive.length; i++)
            {
                if (_dateReceive[i] == null) 
                {
                    if (__dateReceive[i]==null)
                    {
                        __dateReceive[i] = new SimpleDateFormat(__dateReceiveFmt[i], Locale.US);
                        __dateReceive[i].setTimeZone(__GMT);
                    }
                    _dateReceive[i] = (SimpleDateFormat) __dateReceive[i].clone();
                }
                
                try
                {
                    Date date = (Date) _dateReceive[i].parseObject(val);
                    return field._numValue = date.getTime();
                }
                catch (java.lang.Exception e)
                {
                }
            }
            if (val.endsWith(" GMT"))
            {
                val = val.substring(0, val.length() - 4);
                for (int i = 0; i < _dateReceive.length; i++)
                {
                    try
                    {
                        Date date = (Date) _dateReceive[i].parseObject(val);
                        return field._numValue = date.getTime();
                    }
                    catch (java.lang.Exception e)
                    {
                    }
                }
            }
        }
        

        throw new IllegalArgumentException("Cannot convert date: " + val);
    }

    /* -------------------------------------------------------------- */
    /**
     * Sets the value of an long field.
     * 
     * @param name the field name
     * @param value the field long value
     */
    public void putLongField(Buffer name, long value)
    {
        Buffer v = BufferUtil.toBuffer(value);
        put(name, v, value);
    }

    /* -------------------------------------------------------------- */
    /**
     * Sets the value of an long field.
     * 
     * @param name the field name
     * @param value the field long value
     */
    public void putLongField(String name, long value)
    {
        Buffer n = HttpHeaders.CACHE.lookup(name);
        Buffer v = BufferUtil.toBuffer(value);
        put(n, v, value);
    }

    /* -------------------------------------------------------------- */
    /**
     * Sets the value of an long field.
     * 
     * @param name the field name
     * @param value the field long value
     */
    public void addLongField(String name, long value)
    {
        Buffer n = HttpHeaders.CACHE.lookup(name);
        Buffer v = BufferUtil.toBuffer(value);
        add(n, v, value);
    }

    /* -------------------------------------------------------------- */
    /**
     * Sets the value of an long field.
     * 
     * @param name the field name
     * @param value the field long value
     */
    public void addLongField(Buffer name, long value)
    {
        Buffer v = BufferUtil.toBuffer(value);
        add(name, v, value);
    }

    /* -------------------------------------------------------------- */
    /**
     * Sets the value of a date field.
     * 
     * @param name the field name
     * @param date the field date value
     */
    public void putDateField(Buffer name, long date)
    {
        if (_dateBuffer == null)
        {
            _dateBuffer = new StringBuffer(32);
            _calendar = new GregorianCalendar(__GMT);
        }
        _dateBuffer.setLength(0);
        _calendar.setTimeInMillis(date);
        formatDate(_dateBuffer, _calendar, false);
        Buffer v = new ByteArrayBuffer(_dateBuffer.toString());
        put(name, v, date);
    }

    /* -------------------------------------------------------------- */
    /**
     * Sets the value of a date field.
     * 
     * @param name the field name
     * @param date the field date value
     */
    public void putDateField(String name, long date)
    {
        Buffer n = HttpHeaders.CACHE.lookup(name);
        putDateField(n,date);
    }

    /* -------------------------------------------------------------- */
    /**
     * Sets the value of a date field.
     * 
     * @param name the field name
     * @param date the field date value
     */
    public void addDateField(String name, long date)
    {
        if (_dateBuffer == null)
        {
            _dateBuffer = new StringBuffer(32);
            _calendar = new GregorianCalendar(__GMT);
        }
        _dateBuffer.setLength(0);
        _calendar.setTimeInMillis(date);
        formatDate(_dateBuffer, _calendar, false);
        Buffer n = HttpHeaders.CACHE.lookup(name);
        Buffer v = new ByteArrayBuffer(_dateBuffer.toString());
        add(n, v, date);
    }

    /* ------------------------------------------------------------ */
    /**
     * Format a set cookie value
     * 
     * @param cookie The cookie.
     * @param cookie2 If true, use the alternate cookie 2 header
     */
    public void addSetCookie(Cookie cookie)
    {
        String name = cookie.getName();
        String value = cookie.getValue();
        int version = cookie.getVersion();

        // Check arguments
        if (name == null || name.length() == 0) throw new IllegalArgumentException("Bad cookie name");

        // Format value and params
        StringBuffer buf = new StringBuffer(128);
        String name_value_params = null;
        synchronized (buf)
        {
            QuotedStringTokenizer.quoteIfNeeded(buf, name);
            buf.append('=');
            if (value != null && value.length() > 0)
                QuotedStringTokenizer.quoteIfNeeded(buf, value);

            if (version > 0)
            {
                buf.append(";Version=");
                buf.append(version);
                String comment = cookie.getComment();
                if (comment != null && comment.length() > 0)
                {
                    buf.append(";Comment=");
                    QuotedStringTokenizer.quoteIfNeeded(buf, comment);
                }
            }
            String path = cookie.getPath();
            if (path != null && path.length() > 0)
            {
                buf.append(";Path=");
                if (path.startsWith("\""))
                    buf.append(path);
                else
                    QuotedStringTokenizer.quoteIfNeeded(buf,path);
            }
            String domain = cookie.getDomain();
            if (domain != null && domain.length() > 0)
            {
                buf.append(";Domain=");
                QuotedStringTokenizer.quoteIfNeeded(buf,domain.toLowerCase());
            }

            long maxAge = cookie.getMaxAge();
            if (maxAge >= 0)
            {
                if (version == 0)
                {
                    buf.append(";Expires=");
                    if (maxAge == 0)
                        buf.append(__01Jan1970);
                    else
                        formatDate(buf, System.currentTimeMillis() + 1000L * maxAge, true);
                }
                else
                {
                    buf.append(";Max-Age=");
                    buf.append(maxAge);
                }
            }
            else if (version > 0)
            {
                buf.append(";Discard");
            }

            if (cookie.getSecure())
            {
                buf.append(";Secure");
            }
            if (cookie instanceof HttpOnlyCookie)
                buf.append(";HttpOnly");

            // TODO - straight to Buffer?
            name_value_params = buf.toString();
        }
        put(HttpHeaders.EXPIRES_BUFFER, __01Jan1970_BUFFER);
        add(HttpHeaders.SET_COOKIE_BUFFER, new ByteArrayBuffer(name_value_params));
    }

    /* -------------------------------------------------------------- */
    public void put(Buffer buffer) throws IOException
    {
        for (int i = 0; i < _fields.size(); i++)
        {
            Field field = (Field) _fields.get(i);
            if (field != null && field._revision == _revision) field.put(buffer);
        }
        BufferUtil.putCRLF(buffer);
    }

    /* -------------------------------------------------------------- */
    public String toString()
    {
        try
        {
            StringBuffer buffer = new StringBuffer();
          
            for (int i = 0; i < _fields.size(); i++)
            {
                Field field = (Field) _fields.get(i);
                if (field != null && field._revision == _revision)
                {
                    String tmp = field.getName();
                    if (tmp != null) buffer.append(tmp);
                    buffer.append(": ");
                    tmp = field.getValue();
                    if (tmp != null) buffer.append(tmp);
                    buffer.append("\r\n");
                }
            } 
            buffer.append("\r\n");
            return buffer.toString();
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    /* ------------------------------------------------------------ */
    /**
     * Clear the header.
     */
    public void clear()
    {
        _revision++;
        if (_revision > 1000000)
        {
            _revision = 0;
            for (int i = _fields.size(); i-- > 0;)
            {
                Field field = (Field) _fields.get(i);
                if (field != null) field.clear();
            }
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * Destroy the header. Help the garbage collector by null everything that we can.
     */
    public void destroy()
    {
        if (_fields != null)
        {
            for (int i = _fields.size(); i-- > 0;)
            {
                Field field = (Field) _fields.get(i);
                if (field != null) {
                    _bufferMap.remove(field.getNameBuffer());
                    field.destroy();
                }
            }
        }
        _fields = null;
        _dateBuffer = null;
        _calendar = null;
        _dateReceive = null;
    }

    /* ------------------------------------------------------------ */
    /**
     * Add fields from another HttpFields instance. Single valued fields are replaced, while all
     * others are added.
     * 
     * @param fields
     */
    public void add(HttpFields fields)
    {
        if (fields == null) return;

        Enumeration e = fields.getFieldNames();
        while (e.hasMoreElements())
        {
            String name = (String) e.nextElement();
            Enumeration values = fields.getValues(name);
            while (values.hasMoreElements())
                add(name, (String) values.nextElement());
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * Get field value parameters. Some field values can have parameters. This method separates the
     * value from the parameters and optionally populates a map with the paramters. For example:
     * 
     * <PRE>
     * 
     * FieldName : Value ; param1=val1 ; param2=val2
     * 
     * </PRE>
     * 
     * @param value The Field value, possibly with parameteres.
     * @param parameters A map to populate with the parameters, or null
     * @return The value.
     */
    public static String valueParameters(String value, Map parameters)
    {
        if (value == null) return null;

        int i = value.indexOf(';');
        if (i < 0) return value;
        if (parameters == null) return value.substring(0, i).trim();

        StringTokenizer tok1 = new QuotedStringTokenizer(value.substring(i), ";", false, true);
        while (tok1.hasMoreTokens())
        {
            String token = tok1.nextToken();
            StringTokenizer tok2 = new QuotedStringTokenizer(token, "= ");
            if (tok2.hasMoreTokens())
            {
                String paramName = tok2.nextToken();
                String paramVal = null;
                if (tok2.hasMoreTokens()) paramVal = tok2.nextToken();
                parameters.put(paramName, paramVal);
            }
        }

        return value.substring(0, i).trim();
    }

    /* ------------------------------------------------------------ */
    private static Float __one = new Float("1.0");
    private static Float __zero = new Float("0.0");
    private static StringMap __qualities = new StringMap();
    static
    {
        __qualities.put(null, __one);
        __qualities.put("1.0", __one);
        __qualities.put("1", __one);
        __qualities.put("0.9", new Float("0.9"));
        __qualities.put("0.8", new Float("0.8"));
        __qualities.put("0.7", new Float("0.7"));
        __qualities.put("0.66", new Float("0.66"));
        __qualities.put("0.6", new Float("0.6"));
        __qualities.put("0.5", new Float("0.5"));
        __qualities.put("0.4", new Float("0.4"));
        __qualities.put("0.33", new Float("0.33"));
        __qualities.put("0.3", new Float("0.3"));
        __qualities.put("0.2", new Float("0.2"));
        __qualities.put("0.1", new Float("0.1"));
        __qualities.put("0", __zero);
        __qualities.put("0.0", __zero);
    }

    /* ------------------------------------------------------------ */
    public static Float getQuality(String value)
    {
        if (value == null) return __zero;

        int qe = value.indexOf(";");
        if (qe++ < 0 || qe == value.length()) return __one;

        if (value.charAt(qe++) == 'q')
        {
            qe++;
            Map.Entry entry = __qualities.getEntry(value, qe, value.length() - qe);
            if (entry != null) return (Float) entry.getValue();
        }

        HashMap params = new HashMap(3);
        valueParameters(value, params);
        String qs = (String) params.get("q");
        Float q = (Float) __qualities.get(qs);
        if (q == null)
        {
            try
            {
                q = new Float(qs);
            }
            catch (Exception e)
            {
                q = __one;
            }
        }
        return q;
    }

    /* ------------------------------------------------------------ */
    /**
     * List values in quality order.
     * 
     * @param enum Enumeration of values with quality parameters
     * @return values in quality order.
     */
    public static List qualityList(Enumeration e)
    {
        if (e == null || !e.hasMoreElements()) return Collections.EMPTY_LIST;

        Object list = null;
        Object qual = null;

        // Assume list will be well ordered and just add nonzero
        while (e.hasMoreElements())
        {
            String v = e.nextElement().toString();
            Float q = getQuality(v);

            if (q.floatValue() >= 0.001)
            {
                list = LazyList.add(list, v);
                qual = LazyList.add(qual, q);
            }
        }

        List vl = LazyList.getList(list, false);
        if (vl.size() < 2) return vl;

        List ql = LazyList.getList(qual, false);

        // sort list with swaps
        Float last = __zero;
        for (int i = vl.size(); i-- > 0;)
        {
            Float q = (Float) ql.get(i);
            if (last.compareTo(q) > 0)
            {
                Object tmp = vl.get(i);
                vl.set(i, vl.get(i + 1));
                vl.set(i + 1, tmp);
                ql.set(i, ql.get(i + 1));
                ql.set(i + 1, q);
                last = __zero;
                i = vl.size();
                continue;
            }
            last = q;
        }
        ql.clear();
        return vl;
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public static final class Field
    {
        private Buffer _name;
        private Buffer _value;
        private String _stringValue;
        private long _numValue;
        private Field _next;
        private Field _prev;
        private int _revision;

        /* ------------------------------------------------------------ */
        private Field(Buffer name, Buffer value, long numValue, int revision)
        {
            _name = name.asImmutableBuffer();
            _value = value.isImmutable() ? value : new View(value);
            _next = null;
            _prev = null;
            _revision = revision;
            _numValue = numValue;
            _stringValue=null;
        }

        /* ------------------------------------------------------------ */
        private void clear()
        {
            _revision = -1;
        }

        /* ------------------------------------------------------------ */
        private void destroy()
        {
            _name = null;
            _value = null;
            _next = null;
            _prev = null;
            _stringValue=null;
        }

        /* ------------------------------------------------------------ */
        /**
         * Reassign a value to this field. Checks if the string value is the same as that in the char
         * array, if so then just reuse existing value.
         */
        private void reset(Buffer value, long numValue, int revision)
        {
            _revision = revision;
            if (_value == null)
            {
                _value = value.isImmutable() ? value : new View(value);
                _numValue = numValue;
                _stringValue=null;
            }
            else if (value.isImmutable())
            {
                _value = value;
                _numValue = numValue;
                _stringValue=null;
            }
            else
            {
                if (_value instanceof View)
                    ((View) _value).update(value);
                else
                    _value = new View(value);
                _numValue = numValue;
                
                // check to see if string value is still valid.
                if (_stringValue!=null)
                {
                    if (_stringValue.length()!=value.length())
                        _stringValue=null;
                    else
                    {
                        for (int i=value.length();i-->0;)
                        {
                            if (value.peek(value.getIndex()+i)!=_stringValue.charAt(i))
                            {
                                _stringValue=null;
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        

        /* ------------------------------------------------------------ */
        public void put(Buffer buffer) throws IOException
        {
            int o=(_name instanceof CachedBuffer)?((CachedBuffer)_name).getOrdinal():-1;
            if (o>=0)
                buffer.put(_name);
            else
            {
                int s=_name.getIndex();
                int e=_name.putIndex();
                while (s<e)
                {
                    byte b=_name.peek(s++);
                    switch(b)
                    {
                        case '\r':
                        case '\n':
                        case ':' :
                            continue;
                        default:
                            buffer.put(b);
                    }
                }
            }
            
            buffer.put((byte) ':');
            buffer.put((byte) ' ');
            
            o=(_value instanceof CachedBuffer)?((CachedBuffer)_value).getOrdinal():-1;
            if (o>=0 || _numValue>=0)
                buffer.put(_value);
            else
            {
                int s=_value.getIndex();
                int e=_value.putIndex();
                while (s<e)
                {
                    byte b=_value.peek(s++);
                    switch(b)
                    {
                        case '\r':
                        case '\n':
                            continue;
                        default:
                            buffer.put(b);
                    }
                }
            }

            BufferUtil.putCRLF(buffer);
        }

        /* ------------------------------------------------------------ */
        public String getName()
        {
            return BufferUtil.to8859_1_String(_name);
        }

        /* ------------------------------------------------------------ */
        Buffer getNameBuffer()
        {
            return _name;
        }

        /* ------------------------------------------------------------ */
        public int getNameOrdinal()
        {
            return HttpHeaders.CACHE.getOrdinal(_name);
        }

        /* ------------------------------------------------------------ */
        public String getValue()
        {
            if (_stringValue==null)
                _stringValue=BufferUtil.to8859_1_String(_value);
            return _stringValue;
        }

        /* ------------------------------------------------------------ */
        public Buffer getValueBuffer()
        {
            return _value;
        }

        /* ------------------------------------------------------------ */
        public int getValueOrdinal()
        {
            return HttpHeaderValues.CACHE.getOrdinal(_value);
        }

        /* ------------------------------------------------------------ */
        public int getIntValue()
        {
            return (int) getLongValue();
        }

        /* ------------------------------------------------------------ */
        public long getLongValue()
        {
            if (_numValue == -1) _numValue = BufferUtil.toLong(_value);
            return _numValue;
        }

        /* ------------------------------------------------------------ */
        public String toString()
        {
            return ("[" + (_prev == null ? "" : "<-") + getName() + "="+_revision+"=" + _value + (_next == null ? "" : "->") + "]");
        }
    }

}
