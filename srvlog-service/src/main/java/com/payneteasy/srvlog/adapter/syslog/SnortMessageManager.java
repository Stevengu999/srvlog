package com.payneteasy.srvlog.adapter.syslog;

import com.nesscomputing.syslog4j.server.SyslogServerEventIF;
import static com.payneteasy.srvlog.adapter.syslog.OssecSnortMessage.createOssecSnortMessage;
import static com.payneteasy.srvlog.adapter.syslog.SnortMessage.createSnortMessage;
import com.payneteasy.srvlog.data.LogData;
import com.payneteasy.srvlog.data.SnortLogData;
import com.payneteasy.srvlog.service.ILogCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for snort messages from different sources.
 * Manages messages from barnyard2 and ossec and associates them togeser.
 * Properly handles situations, when messages comes in different order
 * (firstly from ossec or firstly from barnyard2).
 *
 * @author imenem
 */
public class SnortMessageManager {

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SnortMessageManager.class);

    /**
     * Object to manipulate logs data in storage.
     */
    private final ILogCollector logCollector;

    /**
     * @param       logCollector        Object to manipulate logs data in storage.
     */
    public SnortMessageManager(ILogCollector logCollector) {
        this.logCollector = logCollector;
    }

    /**
     * Return raw message string from syslog server event.
     *
     * @see OssecSnortMessage#getRawMessage(SyslogServerEventIF)
     *
     * @param       event       Syslog server event.
     *
     * @return      Raw message from event.
     */
    public String getRawMessage(SyslogServerEventIF event) {
        return OssecSnortMessage.getRawMessage(event);
    }

    /**
     * Return true, if message generated by ossec and contains message from snort.
     *
     * @see OssecSnortMessage#isSnortMessageFromOssec(String)
     *
     * @param       rawMessage      Message to ckeck.
     *
     * @return      True, if message generated by ossec and contains message from snort.
     */
    public boolean isSnortMessageFromOssec(String rawMessage) {
        return OssecSnortMessage.isSnortMessageFromOssec(rawMessage);
    }

    /**
     * Returns true, if message generated by snort and received from barnyard2.
     *
     * @see SnortMessage.isMessageFromSnort(String)
     *
     * @param       rawMessage      Message to check.
     *
     * @return      True, if message generated by snort.
     */
    public boolean isMessageFromSnort(String rawMessage) {
        return SnortMessage.isMessageFromSnort(rawMessage);
    }

    /**
     * Saves message that generated by ossec and contains message from snort.
     * Also associates ossec message with messages from  barnyard2, if they came earlier.
     *
     * @param       rawMessage      Raw message that generated by ossec and contains message from snort.
     * @param       logData         Object with common log data.
     */
    public void processOssecSnortMessage(String rawMessage, LogData logData) {
        OssecSnortMessage ossecSnortMessage = createOssecSnortMessage(rawMessage);
        logData.setHash(ossecSnortMessage.getHash());

        saveLog(logData);

        ossecSnortMessage.setLogData(logData);
        logCollector.saveOssecLog(ossecSnortMessage.toOssecLogData());
    }

    /**
     * Saves message that generated by snort and received from barnyard2.
     * Also associates this message with ossec messages, if they comes earlier.
     *
     * @param       rawMessage      Raw message that generated by snort and received from barnyard2.
     */
    public void processRawSnortMessage(String rawMessage) {
        SnortLogData snortLog = createSnortMessage(rawMessage).toSnortLogData();
        logCollector.saveSnortLog(snortLog);
    }

    /**
     * Saves general log data.
     *
     * @param       log         Object with common log data.
     */
    private void saveLog(LogData log) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Saving log: " + log);
        }

        logCollector.saveLog(log);
    }
}
