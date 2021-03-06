package com.ning.arecibo.aggregator.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.json.JSONObject;
import com.google.inject.Inject;
import com.ning.arecibo.aggregator.impl.EventProcessorImpl;
import com.ning.arecibo.aggregator.listeners.EventProcessorListener;
import com.ning.arecibo.aggregator.rest.EventStreamEndPoint.StreamQuery;
import com.ning.arecibo.util.Logger;

@Provider
@Produces("text/plain+rawEvt")
public class RawEventRenderer implements MessageBodyWriter<EventStreamEndPoint.StreamQuery>
{
	private static final Logger log = Logger.getLogger(EsperStatementRenderer.class);
    private final EventProcessorImpl eventProcessor;
    private static final long MAX_QUIET_TIME = 60000L;
    private static final long MAX_POLL_TIME = 5000L;

    @Inject
    public RawEventRenderer(EventProcessorImpl eventProcessor)
    {
        this.eventProcessor = eventProcessor;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return EventStreamEndPoint.StreamQuery.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(StreamQuery query, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return -1;
    }

    @Override
    public void writeTo(StreamQuery query,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException
    {
        EventProcessorListener listener = null;

        try {
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(entityStream));
    
    		final LinkedBlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<Map<String, Object>>();
            listener = new EventProcessorListener(){
                public void processEvent(Map<String, Object> evt)
                {
                    queue.add(evt);
                }
            };
    		eventProcessor.addEventProcessorListener(query.getEventName(), listener);
    
            log.info("polling event beans for %s", query.getEventName());
    
            long lastOutputTime = System.currentTimeMillis();
    		while (!pw.checkError()) {
                long now = System.currentTimeMillis();
    			Map<String, Object> bean = queue.poll(MAX_POLL_TIME, TimeUnit.MILLISECONDS) ;
                if (bean != null ) {
                    if ( query.getWhere() != null )
                    {
                        int match = 0 ;
                        for ( Map.Entry<String, List<String>> entry : query.getWhere().entrySet() ) {
                            for ( String v : entry.getValue() ) {
                                if ( v.equals(bean.get(entry.getKey()))) {
                                    match ++ ;
                                    break;
                                }
                            }
                        }
                        if ( match >= query.getWhere().size() ) {
                            printEvent(pw, bean);
                            entityStream.flush();
                            lastOutputTime = now;
                        }
                        else if(now - lastOutputTime > MAX_QUIET_TIME) {
                            // this should force a failure when browser disconnects
                            printEllipsis(pw);
                            entityStream.flush();
                            lastOutputTime = now;
                        }
                    }
                    else {
                        printEvent(pw, bean);
                        entityStream.flush();
                        lastOutputTime = now;
                    }
    			}
                else {
                    if(now - lastOutputTime > MAX_QUIET_TIME) {
                        // this should force a failure when browser disconnects
                        printEllipsis(pw);
                        entityStream.flush();
                        lastOutputTime = now;
                    }
                }
    		}
		}
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
		finally {
			log.info("Commiting response and cleaning up listener for %s", query.getEventName());
            if ( listener != null && query != null) {
				eventProcessor.removeEventProcessorListener(query.getEventName(), listener);
			}
		}
	}

    private void printEvent(PrintWriter pw, Map<String, Object> map)
    {
	    pw.println(new JSONObject(map));
	    pw.flush();
    }

    private void printEllipsis(PrintWriter pw) {
        pw.println("...");
        pw.flush();
    }
}
