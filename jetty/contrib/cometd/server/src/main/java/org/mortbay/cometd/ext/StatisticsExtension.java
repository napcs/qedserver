// ========================================================================
// Copyright 2009 Mort Bay Consulting Pty. Ltd.
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
//========================================================================

package org.mortbay.cometd.ext;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import org.cometd.Bayeux;
import org.cometd.Client;
import org.cometd.Extension;
import org.cometd.Message;
import org.mortbay.cometd.ClientImpl;
import org.mortbay.log.Log;





/**
 * StatisticsExtension
 *
 * This extension helps gather statistics about round-trip message times. It works by intercepting
 * a message sent from a client to request a statistics measurement be started, and then propagates
 * messages to each client, measuring how long it takes for the clients to receive and respond to
 * the message. A summary of the statistics collected is then sent back to the originating client.
 *
 * The server recognizes a request to start a statistics measurement looking in the received message
 * for a key with a particular value (_statsRequestSentinel). As clients can generate messages with
 * arbitrarily deep structures, you can specify an array of keys (_statsRequestKeys), where each
 * element in the array is one level deeper down a nested structure. NOTE that if your messages are
 * not just Map<String,Object> you will need to subclass this extension to do the message examination.
 * Optionally, the extension can only look for these messages on a specific channel (_statsRequestChannel).
 * The message can also specify the time to wait for all clients to respond to the probe message. The default is 10seconds.
 *
 * For example, if the _statsReqestKeys =  {"chat"} and the sentinel is "//stats" (these is the default)
 * a client would publish a message like so:
 *
 * <pre>chat: //stats,20</pre>
 *
 * The server will deliver a message back to the sending client:
 *
 * <pre>chat: //stats-confirm Statistic id=99 started. Results due in 20s.</pre>
 *
 * The inbound message is examined to see if it has the structure of a potential stats message.
 * If a stats request is recognized, a new stats collection started, and the id of the stats
 * collection and  timestamp on the server is appended. Then, the server passes the message back to
 * Bayeux so that it can be processed, and published out.
 *
 * At this point, the extension's send() method is called. If a stats request with an id and
 * timestamp is recognized, the outbound message is modifed to be a probe message (_probeSentinel)
 * and the sending time appended to the message.
 *
 * A client receives the probe message, and responds by publishing a reply (_probeReplySentinel).
 *
 * Continuing the example above, assuming we set _probeSentinel = "stats-probe",
 * then each client will receive a message like:
 * <pre>chat: //stats-probe,20::--99::--1243945849978::--12345667464</pre>
 *
 * Where ::-- are field delimiters, 99 is the id of the statistic collection underway and 1243945849978
 * is the time on the server the stats request was received, and 12345667464 is the server time at
 * which the stats probe was published to all clients.
 *
 * The client responds with the same message, but prefixed by "//stats-reply":
 * <pre>chat: //stats-reply::--99::--1243945849978::--12345667464</pre>
 *
 * The server receives the replies from each client, and updates the min,max and average
 * statistics for:
 *
 * <ul>
 * <li>RoundTrip: time since the stats request was received on the server to stats-reply
 * <li>Application: time spent in the application between time stats request received to probe being published
 * <li>Infrastructure: time since probe published to reply received
 * <li>Cometd: portion of Infrastructure time that was due to cometd processing (estimate based on TimesyncExtension network lag calculation)
 * </ul>
 *
 * In other words, if T0 is the time at which the original stats request is received on the server,
 * and T1 is the time at which the stats probe is sent, and T2 is the time the probe reply is
 * received:
 * <ul>
 * <li>RoundTrip = T2-T0
 * <li>Application = T1-T0
 * <li>Infrastructure = T2-T1
 * <li>Cometd = Infrastructure - Lag
 * </ul>
 *
 * After waiting the prescribed interval, eg 20 secs,  to collect as many of the clients'
 * messages as possible, it sends a message to the client who originated the statistics request
 * with the results, eg:
 *
 * <pre>chat://stats-results Id 2: samples=1, avgR=43, minR=43, maxR=43, avgI=42, minI=42, maxI=42, avgA=1, minA=1, maxA=1, avgC=42, minC=42, maxC=42</pre>
 *
 * Note that there can be many stats collections in progress at once.
 */
public class StatisticsExtension implements Extension
{
    protected static AtomicLong COUNTER = new AtomicLong();

    /**
     * Channel on which to examine inbound messages for a stats request. Can be null, in which case
     * all channels are examined.
     */
    protected String _statsRequestChannel = null;

    /**
     * Structure of keys to look for on inbound message that is a request to start a stats run.
     */
    protected String[] _statsRequestKeys = new String[]{"chat"};

    /**
     * Sentinel received on inbound that indicates request to start stats run.
     */
    protected String _statsRequestSentinel = "//stats";


    /**
     * Sentinel sent to user to indicate confirmation of start of a stats run.
     */
    protected String _statsConfirmSentinel = "//stats-confirm";


    /**
     * Sentinel prefix sent do user to indicate the results of the stats run.
     */
    protected String _statsResultSentinel = "//stats-results";

    /**
     * Channel on which outbound stats probes are sent to clients.
     */
    protected String _probeChannel = null;

    /**
     * Sentinel prefix that is put on an outbound stats probe message to indicate a probe to all clients.
     */
    protected String _probeSentinel = "//stats-probe";

    /**
     * Structure of keys to look for on outbound messages that indicate a probe being sent.
     */
    protected String[] _probeKeys = null;



    /**
     * Channel on which to expect stats probe replies from clients. Can be null, in which case,
     * all channels are examined.
     */
    protected String _probeReplyChannel = null;

    /**
     * Structure of keys to look for on inbound message with probe replies from clients.
     */
    protected String[] _probeReplyKeys = null;

    /**
     * Sentinel prefix on inbound message with probe results from a client.
     */
    protected String _probeReplySentinel = "//stats-reply";



    protected String _delim = "::--";
    protected Map<String,Statistic> _statistics = new HashMap<String, Statistic>();
    protected Timer _timer;
    protected long _timeout = 10000; //wait 10 sec for all samples for a statistic to complete


    /**
     * Statistic
     *
     * One measurement of min,max and average roundtrip time of a cometd message to all clients.
     */
    public class Statistic extends TimerTask
    {
        public long _id;
        public Client _client;
        public Message _probeMessage;
        public long _timeout = 0;
        public long _samples = 0;

        public long _minRoundTrip = Long.MAX_VALUE;
        public long _maxRoundTrip = 0;
        public long _avgRoundTrip = 0;
        public long _totalRoundTrip;

        public long _minApp = Long.MAX_VALUE;
        public long _maxApp = 0;
        public long _avgApp = 0;
        public long _totalApp = 0;

        public long _minInfra = Long.MAX_VALUE;
        public long _maxInfra = 0;
        public long _avgInfra = 0;
        public long _totalInfra = 0;

        public long _minCometd = Long.MAX_VALUE;
        public long _maxCometd = 0;
        public long _avgCometd = 0;
        public long _totalCometd = 0;

        public Statistic (Client client,  long timeout)
        {
            _id = COUNTER.incrementAndGet();
            _client = client;
            _timeout = timeout;
            _statistics.put(String.valueOf(_id), this); //add it to the statistics
            _timer.schedule(this, timeout);
        }


        public void setProbe (Message probe)
        {
            _probeMessage = (Message)probe.clone();
        }

        public void sample (long reqTime, long sentTime, long rcvTime, long lag)
        {
            // Synchronize sampling with TimerTask execution
            synchronized (this)
            {
                _samples++;

                long roundTrip = rcvTime - reqTime;
                if (roundTrip < _minRoundTrip)
                    _minRoundTrip = roundTrip;
                if (roundTrip > _maxRoundTrip)
                    _maxRoundTrip = roundTrip;
                _totalRoundTrip+=roundTrip;
                _avgRoundTrip = _totalRoundTrip/_samples;

                long appTime = sentTime - reqTime;
                if (appTime < _minApp)
                    _minApp = appTime;
                if (appTime > _maxApp)
                    _maxApp = appTime;
                _totalApp += appTime;
                _avgApp = _totalApp/_samples;

                long infraTime = rcvTime - sentTime;
                if (infraTime < _minInfra)
                    _minInfra = infraTime;
                if (infraTime > _maxInfra)
                    _maxInfra = infraTime;
                _totalInfra += infraTime;
                _avgInfra = _totalInfra/_samples;

                long cometdTime = infraTime - (2*lag);
                if (cometdTime < _minCometd)
                    _minCometd = cometdTime;
                if (cometdTime > _maxCometd)
                    _maxCometd = cometdTime;
                _totalCometd += cometdTime;
                _avgCometd = _totalCometd/_samples;
            }
        }

        public String toString()
        {
            return "Id "+_id+": samples="+_samples+
            ", avgR="+_avgRoundTrip+", minR="+_minRoundTrip+", maxR="+_maxRoundTrip+
            ", avgI="+_avgInfra+", minI="+_minInfra+", maxI="+_maxInfra+
            ", avgA="+_avgApp+", minA="+_minApp+", maxA="+_maxApp+
            ", avgC="+_avgCometd+", minC="+_minCometd+", maxC="+_maxCometd;
        }

        /**
         * Timer expiry: send off the samples we have gathered for this instance
         * @see java.util.TimerTask#run()
         */
        public void run()
        {
            // No exception must escape, otherwise the Timer dies
            try
            {
                // Synchronize sampling with TimerTask execution
                synchronized (this)
                {
                    //remove myself from the list
                    _statistics.remove(String.valueOf(_id));

                    //send the originator of the sample request the result
                    notifyEnd(_client, this);
                }
            }
            catch (Exception x)
            {
                Log.warn("Unexpected exception", x);
            }
        }


        /**
         * Send a message to the originator of the statistics request to advise
         * that it is underway.
         *
         * The message will be sent on the same channel as the probe message.
         */
        public void notifyStart ()
        {
            HashMap<String,Object> msg = new HashMap<String,Object>();
            msg.putAll((Map<String,Object>)_probeMessage.get(Bayeux.DATA_FIELD));

            Map<String,Object> map = matchKeys(msg,  _statsRequestKeys);
            map.put(_statsRequestKeys[_statsRequestKeys.length-1], _statsConfirmSentinel+getStartText());
            _client.deliver(_client, _probeMessage.getChannel(), msg, null); //tell client statistic has started
        }

        public String getStartText ()
        {
           return " Statistic id="+_id+" started. Results due in "+(_timeout/1000)+"s.";
        }


        /**
         * Send a message to the originator of the statistics request to advise of the
         * results.
         *
         * The message will include all data from the original message from the originator,
         * just with the _markerKey field modified.
         */
        public void notifyEnd (Client client, Statistic stat)
        {
           HashMap<String,Object> msg = new HashMap<String,Object>();
           msg.putAll((Map<String,Object>)_probeMessage.get(Bayeux.DATA_FIELD));

            Map<String,Object> map = matchKeys(msg,  _statsRequestKeys);
            msg.put(_statsRequestKeys[_statsRequestKeys.length-1], _statsResultSentinel+" "+getEndText());
            _client.deliver(_client, _probeMessage.getChannel(), msg, null);
        }

        public String getEndText()
        {
            return this.toString();
        }
    }


    public StatisticsExtension ()
    {
        _timer = new Timer(true);
    }

    public void setStatsRequestKeys (String[] keys)
    {
        _statsRequestKeys = keys;
    }

    public String[] getStatsRequestKeys ()
    {
        return _statsRequestKeys;
    }

    public void setStatsRequestSentinel (String val)
    {
        _statsRequestSentinel = val;
    }

    public String getStatsRequestSentinel ()
    {
        return _statsRequestSentinel;
    }

    public void setStatsRequestChannel(String channel)
    {
        _statsRequestChannel = channel;
    }

    public void setStatsConfirmSentinel (String val)
    {
        _statsConfirmSentinel = val;
    }


    public String getStatsConfirmSentinel ()
    {
        return _statsConfirmSentinel;
    }


    public void setStatsResultSentinel (String val)
    {
        _statsResultSentinel = val;
    }

    public String getStatsResultSentinel ()
    {
        return _statsResultSentinel;
    }


    public void setProbeChannel(String channel)
    {
        _probeChannel = channel;
    }


    public void setProbeSentinel (String val)
    {
        _probeSentinel = val;
    }

    public String getProbeSentinel ()
    {
        return _probeSentinel;
    }


    public void setProbeKeys (String[] keys)
    {
        _probeKeys = keys;
    }

    public String[] getProbeKeys ()
    {
        return _probeKeys;
    }

    public void setProbeReplyKeys (String[] keys)
    {
        _probeReplyKeys = keys;
    }

    public String[] getProbeReplyKeys ()
    {
        return _probeReplyKeys;
    }

    public void setProbeReplySentinel (String val)
    {
        _probeReplySentinel = val;
    }

    public String getProbeReplySentinel ()
    {
        return _probeReplySentinel;
    }

    public void setProbeReplyChannel(String channel)
    {
        _probeReplyChannel = channel;
    }


    public void setTimeout (long timeout)
    {
        _timeout = timeout;
    }

    public long getTimeout()
    {
        return _timeout;
    }

    public void setDelim (String delim)
    {
        _delim = delim;
    }

    public String getDelim ()
    {
        return _delim;
    }


    /**
     * Called whenever a message is received from a client.
     *
     * A client will initiate a stats collection run by sending a message containing the
     * _statsRequestKeys structure with a _statsRequestSentinel as the value of the final key in the structure.
     * This message is then passed on to the application, who should pass it back unmodified so that the
     * send () method on this Extension can be called, where we can take timestamps and start measuring the stats.
     *
     * Filter the inbound messages to determine if we have received a reply to a stats probe.
     * @see org.cometd.Extension#rcv(org.cometd.Client, org.cometd.Message)
     */
    public Message rcv(Client from, Message message)
    {
        //Check to see if we have received a reply to a statistics probe from a client
        Map<String,Object> map = matchMessage(message, _probeReplyChannel, _probeReplyKeys);
        if (map != null)
        {
            String marker = matchSentinel(map, _probeReplyKeys[_probeReplyKeys.length-1], _probeReplySentinel);
            if (marker != null)
            {
                //This is a round trip reply from a client, so update the stats with this result
                updateStatistic(from,message,marker);
                //But don't propagate it to the application
                return null;
            }
        }

        //Check to see if we have received a request to start gathering statistics
        map = matchMessage(message, _statsRequestChannel, _statsRequestKeys);
        if (map != null)
        {
            String match = matchSentinel(map, _statsRequestKeys[_statsRequestKeys.length-1], _statsRequestSentinel);
            if (match != null && !match.startsWith(_probeReplySentinel))
            {
                //This is a request to start a stats collection
                Statistic stat = createStatistic(from,match);

                //Modify the message to include the id of the stat and the time at which the server received the stats start request
                match = match+_delim+stat._id+_delim+System.currentTimeMillis();
                map.put(_statsRequestKeys[_statsRequestKeys.length-1], match);
                onStatisticsRequest(message);
            }
        }

        return message;
    }

    /**
     * Override to be able to modify the message that has been identified as a statistics request
     * @param message the bayeux message identified as statistics request
     */
    protected void onStatisticsRequest(Message message)
    {
    }

    public Message rcvMeta(Client from, Message message)
    {
       return message;
    }

    /**
     * Called before an outbound message is sent. Note this is called once per message only, not once per client.
     *
     * A request to start statistics collection should be passed on unmodified from the application.
     * We will intercept this on the way to clients and start a stats run. We modify the outbound message to
     * be a stats-probe message.
     *
     * If your application is likey to modify the message structure significantly from the _statsRequestKeys
     * structure, then configure the _probeKeys so we know how to filter the outbound message.
     *
     * @see org.cometd.Extension#send(org.cometd.Client, org.cometd.Message)
     */
    public Message send(Client from, Message message)
    {
        //Check the outgoing message to see if it is an outgoing probe. If it is, we start the stats collection.
        //An outgoing probe should look exactly the same as the triggering inbound _statsRequestKeys, but if the
        //application changes the message structure, use the _probeKeys to describe the structure.
        String[] keys = (_probeKeys==null?_statsRequestKeys:_probeKeys);
        Map<String,Object> map = matchMessage(message, _probeChannel, keys);
        if (map != null)
        {
            //Potential outbound probe message, which will be the stats request message. We need to tweak it to make it into a probe
            String match = matchSentinel(map, keys[keys.length-1], _statsRequestSentinel);

            //Ignore any outbound messages that are confirmations or results of stats runs
            if (match != null && !match.startsWith(_statsConfirmSentinel) && !match.startsWith(_statsResultSentinel))
            {
                //Tell the stats requestor that the stats run has started
                String[] parts = match.split(_delim);
                if (parts != null && parts.length > 1)
                {
                    Statistic stat = _statistics.get(parts[1].trim());
                    if (stat != null)
                    {
                        stat.setProbe(message);
                        stat.notifyStart();

                        //Change the sentinel on the outbound message to be the stats-probe and
                        //put the current server timestamp that we were ready to send the messages
                        //so we can retrieve it on the replies
                        match = match.substring(_statsRequestSentinel.length());
                        map.put(keys[keys.length-1], _probeSentinel+match+_delim+System.currentTimeMillis());
                    }
                }
            }
        }

        //pass on the message
        return message;
    }

    public Message sendMeta(Client from, Message message)
    {
        return message;
    }


    public Map<String,Object> matchMessage (Message message, String channel, String[] keys)
    {
        //No special key structure in message to look for, ignore it
        if (keys == null || keys.length == 0)
            return null;

        //is there a special channel to look for?
        if (channel != null && !message.getChannel().equals(channel))
            return null; //not the channel we're monitoring

        //Look for the key structure. The last key will be the leaf that holds the text value to check, but
        //we want to get a reference to the map that holds that key:value pair
        Object node = message.get(Bayeux.DATA_FIELD);
        if (node == null)
            return null;

        if (!Map.class.isAssignableFrom(node.getClass()))
            return null;

        return matchKeys((Map<String,Object>)node, keys);
    }

    public Map<String,Object> matchKeys(Map<String,Object> map, String[] keys)
    {

        Object node = map;

        for (int i=0; i<keys.length-1; i++)
        {
            if (node == null)
                break;

            if (!Map.class.isAssignableFrom(node.getClass()))
            {
                node = null;
                break;
            }
            node = ((Map<String,Object>)node).get(keys[i]);
        }

        if (node == null)
            return null;

        //Node we're looking at now should contain the final key:value pair
        if (!Map.class.isAssignableFrom(node.getClass()))
            return null;

        return (Map<String, Object>)node;
    }

    public String matchSentinel (Map<String,Object> map, String key, String sentinel)
    {

        if (map == null || key == null || sentinel == null)
            return null;

        Object value = map.get(key);

        if (value == null)
            return null;

        if (!String.class.isAssignableFrom(value.getClass()))
            return null;

        String text = (String)value;

        if (!text.startsWith(sentinel))
            return null;

        return text;
    }

    /**
     * Start a statistics collection run.
     *
     * @param from
     * @param message
     * @param marker
     */
    protected Statistic createStatistic(Client from,  String marker)
    {
        long timeout = _timeout;
        String tmp = (marker==null?"":marker);
        int idx = tmp.indexOf(",");
        if (idx > 0)
        {
            tmp = tmp.substring(idx+1);
            try
            {
                timeout = Long.parseLong(tmp.trim())*1000;
            }
            catch (NumberFormatException e)
            {
                Log.ignore(e);
            }
        }

        return newStatistic(from, timeout);
    }

    /**
     * Update a statistics collection with the result for the client.
     * @param from
     * @param message
     * @param marker
     */
    protected void updateStatistic (Client from, Message message, String marker)
    {
        String[] tokens = marker.split(_delim);
        if (tokens == null || tokens.length < 4)
            return; //nothing to update!


        //Message is: _probeReplySentinelt+_delim+id+_delim+rcvTimestamp+_delim+sendTimestamp
        //retrieve the id of the statistic this message is associated with from the message
        Statistic stat = _statistics.get(tokens[1].trim());
        if (stat == null)
            return;

        long reqTimestamp = Long.valueOf(tokens[2].trim()); //time the server received the start stats request
        long sentTimestamp = Long.valueOf(tokens[3].trim());//time the server send the stats probe to all clients

        //update the statistic with this client's result
        int lag = from instanceof ClientImpl ? ((ClientImpl)from).getLag() : 0;
        stat.sample(reqTimestamp, sentTimestamp, System.currentTimeMillis(), lag);
    }

    protected Statistic newStatistic (Client from, long timeout)
    {
        return new Statistic(from, timeout);
    }
}
