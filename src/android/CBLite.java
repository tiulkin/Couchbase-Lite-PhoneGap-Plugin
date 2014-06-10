package com.couchbase.cblite.phonegap;

import android.content.Context;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;

import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.Manager;

import com.couchbase.lite.Database;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Reducer;


import com.couchbase.lite.listener.LiteListener;
import com.couchbase.lite.listener.LiteServlet;
import com.couchbase.lite.listener.Credentials;
import com.couchbase.lite.router.URLStreamHandlerFactory;
import com.couchbase.lite.View;
import com.couchbase.lite.javascript.JavaScriptViewCompiler;

import java.io.IOException;
import java.io.File;

import java.util.Map;
import java.util.List;


public class CBLite extends CordovaPlugin {

	private static final int DEFAULT_LISTEN_PORT = 5984;
	private boolean initFailed = false;
	private int listenPort;
    private Credentials allowedCredentials;
    public static final String DATABASE_NAME = "stuffdj_client";
    public static final String DATABASE_NAME1 = "stuffdj_client_master";

    public static final String designDocName = "cr";
    private Database database;
    private Database database1;

	/**
	 * Constructor.
	 */
	public CBLite() {
		super();
		System.out.println("CBLite() constructor called");
	}

	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		System.out.println("initialize() called");

		super.initialize(cordova, webView);
		initCBLite();

	}

	private void initCBLite() {
		try {

		    allowedCredentials = new Credentials("admin","123456");

			URLStreamHandlerFactory.registerSelfIgnoreError();

			View.setCompiler(new JavaScriptViewCompiler());

			Manager server = startCBLite(this.cordova.getActivity());
			database = server.getDatabase(DATABASE_NAME);
            database1 = server.getDatabase(DATABASE_NAME1);
			com.couchbase.lite.View articlesByDate = database.getView(String.format("%s/%s", designDocName, "articlesByDate"));
			com.couchbase.lite.View articlesPartsByParent = database.getView(String.format("%s/%s", designDocName, "articlesPartsByParent"));
            com.couchbase.lite.View activeChannels = database1.getView(String.format("%s/%s", designDocName, "activeChannels"));
			
			articlesByDate.setMapReduce(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
				String type = (String)document.get("type");				
				if (type.equals("article")) {					
					String status = (String)document.get("status");				
					if (status.equals("Published")){
						emitter.emit(document.get("date"),document.get("title"));
					}
				}
            }},
            new Reducer() {
                @Override
                public Object reduce(List<Object> keys, List<Object> values, boolean rereduce) {
                   return new Integer(values.size());
                }
            }, "1.0");


            activeChannels.setMapReduce(new Mapper() {
                                            @Override
                                            public void map(Map<String, Object> document, Emitter emitter) {
                                               String status = (String) document.get("status");
						System.out.println("MapReduce called");
						System.out.println("status");
                                                if (status.equals("active")) {
//                                                    emitter.emit(document.get("_id"), document.get("name"));
                                                    emitter.emit(0,0);
                                                }
                                            }
                                        },
                    new Reducer() {
                        @Override
                        public Object reduce(List<Object> keys, List<Object> values, boolean rereduce) {
                            return new Integer(values.size());
                        }
                    }, "1.0");

//            activeChannels.setMap(new Mapper() {
//            @Override
//            public void map(Map<String, Object> document, Emitter emitter) {
//					emitter.emit(0,0);
//            }}, "1.0");

//            articlesPartsByParent.setMap(new Mapper() {
//            @Override
//            public void map(Map<String, Object> document, Emitter emitter) {
//				String type = (String)document.get("type");
//				if (type.equals("part")) {
//					emitter.emit(document.get("parentId"),document.get("parentId"));
//				}
//            }}, "1.0");


listenPort = startCBLListener(DEFAULT_LISTEN_PORT, server, allowedCredentials);
			System.out.println("initCBLite() completed successfully");


		} catch (final Exception e) {
			e.printStackTrace();
			initFailed = true;
		}

	}

	@Override
	public boolean execute(String action, JSONArray args,
			CallbackContext callback) {
		if (action.equals("getURL")) {
			try {

				if (initFailed == true) {
					callback.error("Failed to initialize couchbase lite.  See console logs");
					return false;
				} else {
					String callbackRespone = String.format(
							"http://%s:%s@localhost:%d/",
                            allowedCredentials.getLogin(),
                            allowedCredentials.getPassword(),
                            listenPort
                    );

					callback.success(callbackRespone);

					return true;
				}

			} catch (final Exception e) {
				e.printStackTrace();
				callback.error(e.getMessage());
			}
		}
		return false;
	}

	protected Manager startCBLite(Context context) {
		Manager server;
		try {
			server = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return server;
	}

	private int startCBLListener(int listenPort, Manager server, Credentials allowedCredentials) {

		LiteListener listener = new LiteListener(server, listenPort,null); // allowedCredentials);
		int boundPort = listener.getListenPort();
		Thread thread = new Thread(listener);
		thread.start();
		return boundPort;

	}

	public void onResume(boolean multitasking) {
		System.out.println("CBLite.onResume() called");
	}

	public void onPause(boolean multitasking) {
		System.out.println("CBLite.onPause() called");
	}


}
