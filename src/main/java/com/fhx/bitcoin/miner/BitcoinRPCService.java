package com.fhx.bitcoin.miner;

import com.diablominer.DiabloMiner.Utils;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Created by George on 1/2/14.
 */
public class BitcoinRPCService extends AbstractService implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(BitcoinRPCService.class);

    private URL queryUrl;
    private final String user;
    private final String pass;
    private String userPass;

    final ObjectMapper mapper = new ObjectMapper();
    String rejectReason = null;

    //private AtomicInteger scanCount = new AtomicInteger(0);
    protected int targetRoundTime = 6;
    /**
     * Number of hash per work (initial guess, will adjust according to
     * <code>targetRoundTime</code>
     */
    protected int scanCount = 0xffff;
    /** Number of second in each job. Never excess 10 minutes (GAE Limit) */
    protected int targetTotalTime = 60; // 540; in seconds

    private ExecutorService executor;

    public BitcoinRPCService(String user, String pass, String urlStr) {
        this.user = user;
        this.pass = pass;

        try {
            this.queryUrl = new URL(urlStr);
            this.userPass = "Basic " + Base64.encodeBase64String((user + ":" + pass).getBytes()).trim().replace("\r\n", "");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    protected void doStart() {
        log.info("Starting Bitcoin RPC service...");
        this.executor.submit(this);
        notifyStarted();
    }

    @Override
    protected void doStop() {
        log.info("Stopping Bitcoin RPC service...");
        notifyStopped();
    }

    protected static double timePassed(long start) {
        long now = System.currentTimeMillis();
        double tp = (now - start) / 1000.0;

        log.info(" timePassed: start: " + Utils.df.format(new Date(start)) + ", now: " + Utils.df.format(new Date(now)) + " diff (seconds): " + tp);
        if (tp < 0)
            tp += 86400; // midnight

        return tp;
    }

    private void adjustHashPerRound(long startRoundTime) {
        log.info("adjusthashPerRound: " + Utils.df.format(new Date(startRoundTime)));
        double time = timePassed(startRoundTime);

        log.info("$$$$ timePassed: {}, hashCount: {}, startRoundTime: " + Utils.df.format(new Date(startRoundTime)) , time, scanCount);
        scanCount = (int) ((9 * scanCount) + (scanCount / time * targetRoundTime)) / 10;
        log.info("$$$$ timePassed: {}, hashCount: {}, startRoundTime: " + Utils.df.format(new Date(startRoundTime)) , time, scanCount);
    }

    @Override
    public void run() {
        log.info("starting getWork thread...queryUrl: {} ", queryUrl);

        ScanHash sh = new ScanHash();
        int accepted = 0, rejected = 0;
        long startTime = System.currentTimeMillis();

        int requestCount = 0;

        try {
            do {
                long startRoundTime = System.currentTimeMillis();
                Work work = doGetWorkMessage(false);
                requestCount++;

                long hashStart = System.currentTimeMillis();
                log.info("scan hash begin: " + Utils.df.format(new Date(hashStart)) + " scanCount: " + scanCount);
                boolean found = sh.scan(work, 1, scanCount);
                long hashEnd = System.currentTimeMillis();
                log.info("scan hash end  : " + Utils.df.format(new Date(hashEnd)) + " scanCount: " + sh.getCount() + ", took (seconds): " + (hashEnd-hashStart)/1000.0 + ", found: " + found);

                if (found) {
                    log.warn("found: " + work.data);
                    boolean success = doSendWorkMessage(work);

                    if (success) {
                        log.warn("Yay! Accepted!");
                        accepted++;
                    }
                    else {
                        log.warn("Doh! Rejected!");
                        rejected++;
                    }
                } else {
                    adjustHashPerRound(startRoundTime);
                    log.info("... H=" + sh.getCount() + ", A=" + accepted + ", R=" + rejected);
                }

                log.info("HAHA: requestCount="+requestCount);

            } while (timePassed(startTime) < targetTotalTime);

            long endTime = System.currentTimeMillis();
            double diff = (endTime - startTime) / 1000.0;

            log.info("Times Up! startTime: {}, now: {}, diff: " + diff + ", targetTotalTime: " + targetTotalTime,
                    Utils.df.format(new Date(startTime)),
                    Utils.df.format(new Date(System.currentTimeMillis())));

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            log.info("Fin. H=" + sh.getCount() + ", A=" + accepted + ", R=" + rejected);
        }

        log.info("exiting...");
        System.exit(-1);
    }

    private void getWork() throws Exception {
        log.info("calling doGetWorkMessage...");

    }

    private Work doGetWorkMessage(boolean longPoll) throws IOException {
        ObjectNode getWorkMessage = mapper.createObjectNode();

        getWorkMessage.put("method", "getwork");
        getWorkMessage.putArray("params");
        getWorkMessage.put("id", 1);

        log.info("making jason rpc call: {} {} time: " + Utils.df.format(new Date()), longPoll, getWorkMessage);
        JsonNode responseMessage = doJSONRPCCall(longPoll, getWorkMessage);
        //log.info("XXXX got getwork responseMessage: " + responseMessage);

//        Iterator<String> it = responseMessage.getFieldNames();
//        while(it.hasNext()) {
//            String fn = it.next();
//            log.info(fn + ": " + responseMessage.get(fn));
//        }

        String datas;
        String midstates;
        String targets;
        String hash1;

        try {
            datas = responseMessage.get("data").asText();
            midstates = responseMessage.get("midstate").asText();
            targets = responseMessage.get("target").asText();
            hash1 = responseMessage.get("hash1").asText();

        } catch(Exception e) {
            throw new IOException("Bitcoin returned unparsable JSON");
        }

        log.info(String.format("got rpc response: time: {%s} data{%s}, midstate: {%s}, target: {%s}, hash1: {%s}",
                Utils.df.format(new Date()), datas, midstates, targets, hash1));

        Work work = new Work(datas, midstates, targets, hash1);

        String parse;

        for(int i = 0; i < 32; i++) {
            parse = datas.substring(i * 8, (i * 8) + 8);
            work.setData(i, Integer.reverseBytes((int) Long.parseLong(parse, 16)));
        }

        for(int i = 0; i < 8; i++) {
            parse = midstates.substring(i * 8, (i * 8) + 8);
            work.setMidstate(i, Integer.reverseBytes((int) Long.parseLong(parse, 16)));
        }

        for(int i = 0; i < 8; i++) {
            parse = targets.substring(i * 8, (i * 8) + 8);
            work.setTarget(i, (Long.reverseBytes(Long.parseLong(parse, 16) << 16)) >>> 16);
        }

        // publish to disruptor
        return work;
    }

    private JsonNode doJSONRPCCall(boolean longPoll, ObjectNode message) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) queryUrl.openConnection();

            if(longPoll) {
                connection.setConnectTimeout(10 * 60 * 1000);
                connection.setReadTimeout(10 * 60 * 1000);
            } else {
                connection.setConnectTimeout(15 * 1000);
                connection.setReadTimeout(15 * 1000);
            }

            connection.setRequestProperty("Authorization", userPass);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Accept-Encoding", "gzip,deflate");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("User-Agent", "DiabloMiner");
            connection.setRequestProperty("X-Mining-Extensions", "longpoll rollntime switchto");
            connection.setDoOutput(true);

            OutputStream requestStream = connection.getOutputStream();
            Writer request = new OutputStreamWriter(requestStream);
            request.write(message.toString());
            request.close();
            requestStream.close();

            ObjectNode responseMessage = null;
            InputStream responseStream = null;

            try {
                String xLongPolling = connection.getHeaderField("X-Long-Polling");

                if(xLongPolling != null && !"".equals(xLongPolling) ) {

                    log.info(": Enabling long poll support");
                }

                String xRollNTime = connection.getHeaderField("X-Roll-NTime");

                if(xRollNTime != null && !"".equals(xRollNTime)) {
                    log.info("Enabling/Disabling roll ntime support");
                }

                String xSwitchTo = connection.getHeaderField("X-Switch-To");

                if(xSwitchTo != null && !"".equals(xSwitchTo)) {
                    String oldHost = queryUrl.getHost();
                    JsonNode newHost = mapper.readTree(xSwitchTo);

                    queryUrl = new URL(queryUrl.getProtocol(), newHost.get("host").asText(), newHost.get("port").getIntValue(), queryUrl.getPath());

                    log.info(oldHost + ": Switched to " + queryUrl.getHost());
                }

                String xRejectReason = connection.getHeaderField("X-Reject-Reason");

                if(xRejectReason != null && !"".equals(xRejectReason)) {
                    rejectReason = xRejectReason;
                }

                String xIsP2Pool = connection.getHeaderField("X-Is-P2Pool");

                if(xIsP2Pool != null && !"".equals(xIsP2Pool)) {
                    log.info("P2Pool no delay mode enabled");
                }

                if(connection.getContentEncoding() != null) {
                    if(connection.getContentEncoding().equalsIgnoreCase("gzip"))
                        responseStream = new GZIPInputStream(connection.getInputStream());
                    else if(connection.getContentEncoding().equalsIgnoreCase("deflate"))
                        responseStream = new InflaterInputStream(connection.getInputStream());
                } else {
                    responseStream = connection.getInputStream();
                }

                if(responseStream == null)
                    throw new IOException("Drop to error handler");

                Object output = mapper.readTree(responseStream);

                if(NullNode.class.equals(output.getClass())) {
                    throw new IOException("Bitcoin returned unparsable JSON");
                } else {
                    try {
                        responseMessage = (ObjectNode) output;
                    } catch(ClassCastException e) {
                        throw new IOException("Bitcoin returned unparsable JSON");
                    }
                }

                responseStream.close();
            } catch(JsonProcessingException e) {
                throw new IOException("Bitcoin returned unparsable JSON");
            } catch(IOException e) {
                InputStream errorStream = null;
                IOException e2 = null;

                if(connection.getErrorStream() == null)
                    throw new IOException("Bitcoin disconnected during response: " + connection.getResponseCode() + " " + connection.getResponseMessage());

                if(connection.getContentEncoding() != null) {
                    if(connection.getContentEncoding().equalsIgnoreCase("gzip"))
                        errorStream = new GZIPInputStream(connection.getErrorStream());
                    else if(connection.getContentEncoding().equalsIgnoreCase("deflate"))
                        errorStream = new InflaterInputStream(connection.getErrorStream());
                } else {
                    errorStream = connection.getErrorStream();
                }

                if(errorStream == null)
                    throw new IOException("Bitcoin disconnected during response: " + connection.getResponseCode() + " " + connection.getResponseMessage());

                byte[] errorbuf = new byte[8192];

                if(errorStream.read(errorbuf) < 1)
                    throw new IOException("Bitcoin returned an error, but with no message");

                String error = new String(errorbuf).trim();

                if(error.startsWith("{")) {
                    try {
                        Object output = mapper.readTree(error);

                        if(NullNode.class.equals(output.getClass()))
                            throw new IOException("Bitcoin returned an error message: " + error);
                        else
                            try {
                                responseMessage = (ObjectNode) output;
                            } catch(ClassCastException f) {
                                throw new IOException("Bitcoin returned unparsable JSON");
                            }

                        if(responseMessage.get("error") != null) {
                            if(responseMessage.get("error").get("message") != null && responseMessage.get("error").get("message").asText() != null) {
                                error = responseMessage.get("error").get("message").asText().trim();
                                e2 = new IOException("Bitcoin returned error message: " + error);
                            } else if(responseMessage.get("error").asText() != null) {
                                error = responseMessage.get("error").asText().trim();

                                if(!"null".equals(error) && !"".equals(error))
                                    e2 = new IOException("Bitcoin returned an error message: " + error);
                            }
                        }
                    } catch(JsonProcessingException f) {
                        e2 = new IOException("Bitcoin returned unparsable JSON");
                    }
                } else {
                    e2 = new IOException("Bitcoin returned an error message: " + error);
                }

                errorStream.close();

                if(responseStream != null)
                    responseStream.close();

                if(e2 == null)
                    e2 = new IOException("Bitcoin returned an error, but with no message");

                throw e2;
            }

            if(responseMessage.get("error") != null) {
                if(responseMessage.get("error").get("message") != null && responseMessage.get("error").get("message").asText() != null) {
                    String error = responseMessage.get("error").get("message").asText().trim();
                    throw new IOException("Bitcoin returned error message: " + error);
                } else if(responseMessage.get("error").asText() != null) {
                    String error = responseMessage.get("error").asText().trim();

                    if(!"null".equals(error) && !"".equals(error))
                        throw new IOException("Bitcoin returned error message: " + error);
                }
            }

            JsonNode result;

            try {
                result = responseMessage.get("result");
            } catch(Exception e) {
                throw new IOException("Bitcoin returned unparsable JSON");
            }

            if(result == null)
                throw new IOException("Bitcoin did not return a result or an error");

            return result;
        } catch(IOException e) {
            if(connection != null)
                connection.disconnect();

            throw e;
        }
    }

    boolean doSendWorkMessage(Work work) throws IOException {
        StringBuilder dataOutput = new StringBuilder(8 * 32 + 1);
        Formatter dataFormatter = new Formatter(dataOutput);
        int[] data = work.getData();

        dataFormatter.format(
                "%08x%08x%08x%08x%08x%08x%08x%08x%08x%08x%08x%08x%08x%08x%08x%08x" +
                        "%08x%08x%08x%08x%08x%08x%08x%08x%08x%08x%08x%08x%08x%08x%08x%08x",
                Integer.reverseBytes(data[0]), Integer.reverseBytes(data[1]),
                Integer.reverseBytes(data[2]), Integer.reverseBytes(data[3]),
                Integer.reverseBytes(data[4]), Integer.reverseBytes(data[5]),
                Integer.reverseBytes(data[6]), Integer.reverseBytes(data[7]),
                Integer.reverseBytes(data[8]), Integer.reverseBytes(data[9]),
                Integer.reverseBytes(data[10]), Integer.reverseBytes(data[11]),
                Integer.reverseBytes(data[12]), Integer.reverseBytes(data[13]),
                Integer.reverseBytes(data[14]), Integer.reverseBytes(data[15]),
                Integer.reverseBytes(data[16]), Integer.reverseBytes(data[17]),
                Integer.reverseBytes(data[18]), Integer.reverseBytes(data[19]),
                Integer.reverseBytes(data[20]), Integer.reverseBytes(data[21]),
                Integer.reverseBytes(data[22]), Integer.reverseBytes(data[23]),
                Integer.reverseBytes(data[24]), Integer.reverseBytes(data[25]),
                Integer.reverseBytes(data[26]), Integer.reverseBytes(data[27]),
                Integer.reverseBytes(data[28]), Integer.reverseBytes(data[29]),
                Integer.reverseBytes(data[30]), Integer.reverseBytes(data[31]));

        ObjectNode sendWorkMessage = mapper.createObjectNode();
        sendWorkMessage.put("method", "getwork");
        ArrayNode params = sendWorkMessage.putArray("params");
        params.add(dataOutput.toString());
        sendWorkMessage.put("id", 1);

        JsonNode responseMessage = doJSONRPCCall(false, sendWorkMessage);

        log.info("$$$$ got submit responseMessage: " + responseMessage);
        Iterator<String> it = responseMessage.getFieldNames();
        while(it.hasNext()) {
            String fn = it.next();
            log.info(fn + ": " + responseMessage.get(fn));
        }

        boolean accepted;

        dataFormatter.close();

        try {
            accepted = responseMessage.getBooleanValue();
        } catch(Exception e) {
            throw new IOException("Bitcoin returned unparsable JSON");
        }

        return accepted;
    }

    private void submitWork() throws Exception {

    }

    public static void main(String[] args) {
        String path = BitcoinRPCService.class.getClassLoader().getResource("config.properties").getFile();
        System.out.println("setting config.properties=" + path);
        System.getProperties().setProperty("config.properties", path);
        System.getProperties().setProperty("instanceName", "node_1");

        final Properties properties = new Properties();
        String filename = System.getProperty("config.properties", "config.properties");
        final File file = new File(filename);
        if (file.exists()) {
            FileReader reader = null;
            try{
                reader = new FileReader(file);
                properties.load(reader);
            }
            catch (IOException io) {
                io.printStackTrace();
            }
            finally {
                if (reader != null) {
                    try {
                        reader.close();
                    }
                    catch (IOException ie) {
                        ie.printStackTrace();
                    }
                }
            }
        }
        else {
            URL inputStream = BitcoinRPCService.class.getResource("/" + filename);

            if (inputStream != null) {
                try {
                    properties.load(inputStream.openStream());
                }
                catch (IOException e) {
                    throw new RuntimeException("Error loading properties " + filename + " from classpath");
                }
            }
        }
        log.info("xxxx system properties: " + Arrays.toString(properties.keySet().toArray()));

        // http://gfeng:br001klyn@localhost:18332
        final String user = properties.getProperty("user");
        final String password = properties.getProperty("pass");
        final String urlStr = properties.getProperty("url");

        BitcoinRPCService rpcService = new BitcoinRPCService(user, password, urlStr);
        startSingletonServices(rpcService);

    }

    private static void startSingletonServices(Service... services) {
        ServiceManager serviceManager = new ServiceManager(ImmutableList.copyOf(services));
        serviceManager.startAsync();
        serviceManager.awaitHealthy();
    }
}
