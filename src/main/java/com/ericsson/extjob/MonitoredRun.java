/******************************************************************************
 * External Job Monitor
 * Copyright Ericsson AB 2011. All Rights Reserved.
 *
 * Software is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND,
 * either express or implied.
 *
 ******************************************************************************/
package com.ericsson.extjob;

import hudson.Proc;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.util.DecodingStream;
import hudson.util.DualOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class MonitoredRun extends Run<MonitoredJob, MonitoredRun> {

    /**
     * Loads a run from a log file.
     */
    MonitoredRun(MonitoredJob owner, File runDir) throws IOException {
        super(owner, runDir);
    }

    /**
     * Creates a new run.
     */
    MonitoredRun(MonitoredJob project) throws IOException {
        super(project);
    }

    /**
     * Instead of performing a build, run the specified command,
     * record the log and its exit code, then call it a build.
     */
    public void run(final String[] cmd) {
        run(new Runner() {

            public Result run(BuildListener listener) throws Exception {
                Proc proc = new Proc.LocalProc(cmd, getEnvironment(listener), System.in, new DualOutputStream(System.out, listener.getLogger()));
                return proc.join() == 0 ? Result.SUCCESS : Result.FAILURE;
            }

            public void post(BuildListener listener) {
                // do nothing
            }

            public void cleanUp(BuildListener listener) {
                // do nothing
            }
        });
    }

    /**
     * Instead of performing a build, accept the log and the return code
     * from a remote machine.
     *
     * <p>
     * The format of the XML is:
     *
     * <pre><xmp>
     * <run>
     *  <log>...console output...</log>
     *  <result>exit code</result>
     * </run>
     * </xmp></pre>
     */
    @SuppressWarnings({"Since15"})
    //@IgnoreJRERequirement
    public void acceptRemoteSubmission(final Reader in) throws IOException {
        final long[] duration = new long[1];
        run(new Runner() {

            private String elementText(XMLStreamReader r) throws XMLStreamException {
                StringBuilder buf = new StringBuilder();
                while (true) {
                    int type = r.next();
                    if (type == XMLStreamReader.CHARACTERS || type == XMLStreamReader.CDATA) {
                        buf.append(r.getTextCharacters(), r.getTextStart(), r.getTextLength());
                    } else {
                        return buf.toString();
                    }
                }
            }

            public Result run(BuildListener listener) throws Exception {

                // Retreive the address string of the job, in order to fetch the XML (or JSON)
                PrintStream logger = new PrintStream(new DecodingStream(listener.getLogger()));

                XMLInputFactory xif = XMLInputFactory.newInstance();
                XMLStreamReader p = xif.createXMLStreamReader(in);

                p.nextTag();    // get to the <run>
                p.nextTag();    // get to the <log>

                charset = p.getAttributeValue(null, "content-encoding");
                while (p.next() != XMLStreamReader.END_ELEMENT) {
                    int type = p.getEventType();
                    if (type == XMLStreamReader.CHARACTERS || type == XMLStreamReader.CDATA) {
                        logger.print(p.getText());
                    }
                }
                p.nextTag(); // get to <result>

                Result r = Integer.parseInt(elementText(p)) == 0 ? Result.SUCCESS : Result.FAILURE;

                p.nextTag();  // get to <duration> (optional)
                if (p.getEventType() == XMLStreamReader.START_ELEMENT
                        && p.getLocalName().equals("duration")) {
                    duration[0] = Long.parseLong(elementText(p));
                }


                if (p.nextTag() == XMLStreamReader.START_ELEMENT) {
                    //if (p.getLocalName().equals("culprits"))
                    p.nextTag();
                    while (true) {

                        if (p.getEventType() == XMLStreamReader.END_ELEMENT) {
                            if (p.getLocalName().equals("culprits")) {
                                break;
                            }
                        }
                        System.out.println("Tag: " + p.getLocalName() + " - " + p.getElementText());

                        // Add culprits to run


                        p.nextTag();
                    }
                }
                return r;
            }

            public void post(BuildListener listener) {
                // do nothing
            }

            public void cleanUp(BuildListener listener) {
                // do nothing
            }
        });

        if (duration[0] != 0) {
            super.duration = duration[0];
            // save the updated duration
            save();
        }
    }
}
