package uk.ac.open.ouanywhere;

import android.content.SharedPreferences;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Analytics class, records events and uploads them to the server when online
 *
 * @author Nigel Clarke <nigel.clarke@pentahedra.com>
 */
public class Analytics extends CordovaPlugin {

    JSONObject serverParams;
    JSONObject eventParams;
    String serverUri = null;
    String logfile = "";
    String oucu = null;
    String key = null;
    public static final String PREFS_NAME = "OUA_Prefs";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        serverParams = args.getJSONObject(0);
        eventParams = args.getJSONObject(1);

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                // code running in background to app
                String server = null;
                String uri = null;
                Boolean online = false;
                try {
                    server = serverParams.has("server") ? serverParams.getString("server") : null;
                    uri = serverParams.has("uri") ? serverParams.getString("uri") : null;
                    logfile = serverParams.has("logfile") ? serverParams.getString("logfile") : "";
                    online = serverParams.has("online") ? serverParams.getBoolean("online") : false;
                    oucu = serverParams.has("oucu") ? serverParams.getString("oucu") : null;
                    key = serverParams.has("key") ? serverParams.getString("key") : null;
                } catch (JSONException ex) {
                    Logger.getLogger(Analytics.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (server != null && uri != null) {
                    serverUri = server + uri;
                    String eventStr = json2XML(eventParams);

                    addToLog(eventStr);

                    if (online) { // online, so check if we have a log to upload, plus upload the new event
                        // Only upload once every 23 hours
                        long now = System.currentTimeMillis() / 1000; // now in seconds
                        SharedPreferences settings = cordova.getActivity().getSharedPreferences(PREFS_NAME, 0);
                        long lastTime = settings.getLong("lastAnalyticsUpload", 0);
                        boolean uploaded = false;
                        if (now - lastTime > 60 * 60 * 23) {
//                            Logger.getLogger(Analytics.class.getName()).log(Level.INFO, "Uploading analytics...");
                            // upload log if present
                            try {
                                BufferedReader br = new BufferedReader(new FileReader(logfile));
                                String line;
                                String block = "";
                                int lineCount = 0;
                                while ((line = br.readLine()) != null) {
                                    block = block + line;
                                    lineCount++;
                                    if (lineCount > 999) { // upload up to 1000 log items at once
                                        if (upload(block)) {
                                            uploaded = true;
                                        }
                                        block = "";
                                        lineCount = 0;
                                    }
                                }
                                br.close();
                                if (upload(block) || uploaded) {
//                                    Logger.getLogger(Analytics.class.getName()).log(Level.INFO, "Uploaded analytics - Woohoo!");

                                    // Save upload time
                                    SharedPreferences.Editor editor = settings.edit();
                                    editor.putLong("lastAnalyticsUpload", now);
                                    editor.commit();
                                }

                            } catch (FileNotFoundException ex) {
                                // file not found - no need to panic, it just means we have no log to upload
                                Logger.getLogger(Analytics.class.getName()).log(Level.SEVERE, null, ex);

                            } catch (IOException ex) {
                                Logger.getLogger(Analytics.class.getName()).log(Level.SEVERE, null, ex);
                                // Logger.getLogger(Analytics.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        } else {
//                            Logger.getLogger(Analytics.class.getName()).log(Level.SEVERE, "NOT uploading analytics - too soon!");
                        }
                    }
                }
            }

            /**
             * Generates an XML string of the format:
             * <event oucu="sm449" website="A373-12K"
             * time="2013-03-21T15:07:33Z" action="viewlist" info=""
             * resourceid="" filetype="" fileurl="" />
             *
             * @param {JSONObject} data - a json object of the event data
             * @returns {String} event - xml version of the event
             */
            String json2XML(JSONObject data) {
                String event = "<event ";
                try {
                    if (data.has("oucu")) {
                        event = event + "oucu=\"" + data.getString("oucu") + "\" ";
                    }
                    if (data.has("website")) {
                        event = event + "website=\"" + data.getString("website") + "\" ";
                    }
                    if (data.has("time")) {
                        event = event + "time=\"" + data.getString("time") + "\" ";
                    }
                    if (data.has("action")) {
                        event = event + "action=\"" + data.getString("action") + "\" ";
                    }
                    if (data.has("info")) {
                        event = event + "info=\"" + data.getString("info") + "\" ";
                    }
                    if (data.has("resourceid")) {
                        event = event + "resourceid=\"" + data.getString("resourceid") + "\" ";
                    }
                    if (data.has("filetype")) {
                        event = event + "filetype=\"" + data.getString("filetype") + "\" ";
                    }
                    if (data.has("fileurl")) {
                        event = event + "fileurl=\"" + data.getString("fileurl") + "\" ";
                    }
                } catch (JSONException ex) {
                    Logger.getLogger(Analytics.class.getName()).log(Level.SEVERE, null, ex);
                }
                event = event + "/>";
                return event;
            }

            /**
             * Adds an event string in XML format to the local log file.
             */
            void addToLog(String eventStr) {
                try {
                    File log = new File(logfile);
                    if (!log.exists()) {
                        log.createNewFile();
                    }
                    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logfile, true)));
                    out.println(eventStr);
                    out.close();
                } catch (IOException ex) {
                    Logger.getLogger(Analytics.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            /**
             * Sends a block of event entries to the server
             */
            boolean upload(String data) {

                if (!"".equals(data)) {
                    Logger.getLogger(Analytics.class.getName()).log(Level.INFO, "Uploading data...");

                    try {
                        data = String.format("oucu=%s&key=%s&log=%s",
                                URLEncoder.encode(oucu, "utf-8"),
                                URLEncoder.encode(key, "utf-8"),
                                "<log>" + data + "</log>", "utf-8");
                        // URLEncoder.encode("<log>" + data + "</log>", "utf-8"));
//                    data = "<oucu>" + oucu + "</oucu><key>" + key + "</key><log>" + data + "</log>";
                        byte[] dataBytes = data.getBytes();
                        URL url = new URL(serverUri);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoOutput(true);
                        connection.setDoInput(true);
                        connection.setInstanceFollowRedirects(false);
                        connection.setRequestMethod("POST");
                        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        connection.setRequestProperty("charset", "utf-8");
                        connection.setRequestProperty("Content-Length", "" + Integer.toString(dataBytes.length));
                        connection.setUseCaches(false);

                        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                        wr.write(dataBytes, 0, dataBytes.length);
//                        wr.writeBytes(data);
                        wr.flush();
                        InputStream response = connection.getInputStream();
                        int status = connection.getResponseCode();

                        String contentType = connection.getHeaderField("Content-Type");
                        String charset = null;
                        String replyInfo = "Sent:" + data + "\n";
                        for (String param : contentType.replace(" ", "").split(";")) {
                            if (param.startsWith("charset=")) {
                                charset = param.split("=", 2)[1];
                                break;
                            }
                        }

                        if (charset != null) {
                            BufferedReader reader = null;
                            try {
                                reader = new BufferedReader(new InputStreamReader(response, charset));
                                for (String line; (line = reader.readLine()) != null;) {
                                    replyInfo = replyInfo + line;
                                }
                            } finally {
                                if (reader != null) {
                                    try {
                                        reader.close();
                                    } catch (IOException logOrIgnore) {
                                    }
                                }
                            }
                        } else {
                            replyInfo = "*Binary response*";
                            // It's likely binary content, so //TODO use InputStream/OutputStream to read it
                        }
                        wr.close();
                        connection.disconnect();

                        String statusText = "OK - ";
                        if (status == HttpURLConnection.HTTP_OK) {
                            // Delete file to empty uploaded log
                            File sentLog = new File(logfile + ".sent");
                            sentLog.delete();
                            File log = new File(logfile);
                            log.renameTo(sentLog);
                            //log.delete();
                        } else {
                            statusText = "ERROR:" + status + " - ";
                        }

                        // Let's log the reply from the server for debug purposes.
                        File logreply = new File(logfile + ".reply");
                        if (!logreply.exists()) {
                            logreply.createNewFile();
                        }

                        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logfile + ".reply", true)));
                        out.println(statusText + replyInfo);
                        out.close();
                        return true;
                    } catch (Exception ex) {
                        // Connection failed - most likely we don't have a connection
                        // Don't panic, the event will have been logged to file, and will upload later
                        Logger.getLogger(Analytics.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                return false;
            }
        });

        return true;
    }
}
