/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hazelcast.platform.demos.industry.iiot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.map.IMap;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>Idempotent initialization steps to be run on every node
 * before initialization on any node.
 * </p>
 * <p>Ie. member specific runs before cluster specific.
 * </p>
 * <ol>
 * <li>
 * <p>Logging</p>
 * <p>Activate <a href="http://www.slf4j.org/">Slf4j</a> with
 * <a href="https://logback.qos.ch/">Logback</a> backend. Logs
 * go to an {@link IMap} for later inspection/extraction.
 * </li>
 * </ol>
 */
public class InitializerAllNodes
    extends Tuple4Callable {
        private static final long serialVersionUID = 1L;
        private static final String MY_NAME = InitializerAllNodes.class.getSimpleName();


    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR",
            justification = "setHazelcastInstance() gets called by Hazelcast")
    private transient HazelcastInstance hazelcastInstance;
    private Level level;

    InitializerAllNodes(Level arg0) {
        this.level = arg0;
    }

    /**
     * <p>This to do to initialize every node. Each method invoked should
     * be idempotent.
     * </p>
     */
    @Override
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "sleep can throw exception")
    public List<Tuple4<String, String, List<String>, List<String>>> call() throws Exception {
        List<Tuple4<String, String, List<String>, List<String>>> result = new ArrayList<>();

        // Logging is via Slf4j. Logback is easier to customize than Log4j2.
        this.initialiseLogback(result);

        return result;
    }


    /**
     * <p>Activate Logback. This will have a logger "{@code ROOT}" but we add our own
     * to log messages from this application into an IMap.
     * </p>
     *
     * @param result
     */
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION",
            justification = "SpotBugs 4.4.2.2 doesn't spot exception that can be thrown by Logger casting")
    private void initialiseLogback(List<Tuple4<String, String, List<String>, List<String>>> result) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        boolean existing = false;

        try {
            // assume SLF4J is bound to logback in the current environment
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

            for (Logger logger : loggerContext.getLoggerList()) {
                if (logger.getName().equals(MyConstants.SLF4J_LOGGER_NAME)) {
                    // Keep, but could drop/create with "logger.detachAndStopAllAppenders();"
                    existing = true;
                    Iterator<Appender<ILoggingEvent>> iterator = logger.iteratorForAppenders();
                    if (iterator.hasNext()) {
                        while (iterator.hasNext()) {
                            Appender<ILoggingEvent> appender = iterator.next();
                            warnings.add("Logger \"" + logger.getName()
                                + "\" exists with appender \"" + appender.getName() + "\".");
                        }
                    } else {
                        errors.add("Logger \"" + logger.getName() + "\" already exists but with no appenders.");
                    }
                }
            }

            if (!existing) {
                IMapAppender iMapAppender = new IMapAppender(this.hazelcastInstance);
                iMapAppender.setContext(loggerContext);
                iMapAppender.setName(MyConstants.SLF4J_APPENDER_NAME);
                iMapAppender.start();

                Logger iMapLogger = loggerContext.getLogger(MyConstants.SLF4J_LOGGER_NAME);
                iMapLogger.setAdditive(false);
                if (this.level == null) {
                    iMapLogger.setLevel(Level.DEBUG);
                } else {
                    iMapLogger.setLevel(this.level);
                }
                iMapLogger.addAppender(iMapAppender);

                org.slf4j.Logger slf4jLogger = org.slf4j.LoggerFactory.getLogger(InitializerAllNodes.class);
                slf4jLogger.info("Initialized Logback");
                slf4jLogger.debug("Runtime.getRuntime().availableProcessors()=="
                        + Runtime.getRuntime().availableProcessors());
            }

            result.add(Tuple4.tuple4(MY_NAME + ".initialiseLogback()", "", errors, warnings));
        } catch (Exception e) {
            Utils.addExceptionToError(errors, null, e);
        }
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Hazelcast instance must be shared, not cloned")
    @Override
    public void setHazelcastInstance(HazelcastInstance arg0) {
        this.hazelcastInstance = arg0;
    }

}
