package com.payneteasy.srvlog.adapter.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.payneteasy.srvlog.adapter.udp.Datagram;
import com.payneteasy.srvlog.adapter.udp.IDatagramProcessor;
import com.payneteasy.srvlog.data.LogData;
import com.payneteasy.srvlog.service.ILogCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

@Service
public class LogbackUDPAdapter extends AbstractUDPLoggerAdapter {

    @Autowired
    public LogbackUDPAdapter(
            ILogCollector logCollector,
            @Value("${logbackProgram}") String program,
            @Value("${logbackUDPPort}") int serverPort) {
        super(serverPort, program, logCollector);
    }

    @Override
    protected String getLoggerTypeName() {
        return "logback";
    }

    void processEvent(ServerLogbackEvent logEvent) {
        LogData logData = LogbackAdapterUtils.buildLogData(logEvent, getProgram());

        logger.info("New " + getLoggerTypeName() + " event caught");
        getLogCollector().saveLog(logData);
    }

    @Override
    protected IDatagramProcessor createProcessor() {
        return new LogbackProcessor(this);
    }

    private static class LogbackProcessor implements IDatagramProcessor {

        private final LogbackUDPAdapter logbackAdapter;

        private LogbackProcessor(LogbackUDPAdapter logbackAdapter) {
            this.logbackAdapter = logbackAdapter;
        }

        @Override
        public void processDatagram(Datagram datagram) {
            ILoggingEvent event;
            try {
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(datagram.getData()));
                event = (ILoggingEvent) ois.readObject();
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read event data", e);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Cannot read event data", e);
            }
            logbackAdapter.processEvent(new ServerLogbackEvent(event, datagram.getSourceAddress()));
        }
    }
}