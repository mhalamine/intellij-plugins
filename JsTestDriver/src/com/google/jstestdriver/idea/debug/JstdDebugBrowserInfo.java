package com.google.jstestdriver.idea.debug;

import com.google.jstestdriver.CapturedBrowsers;
import com.google.jstestdriver.SlaveBrowser;
import com.google.jstestdriver.idea.execution.settings.JstdRunSettings;
import com.google.jstestdriver.idea.server.JstdServerState;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.ide.browsers.BrowserFamily;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.javascript.debugger.engine.JSDebugEngine;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Pair;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
* @author Sergey Simonchik
*/
public class JstdDebugBrowserInfo {

  private final Pair<JSDebugEngine, WebBrowser> myDebugEngine;
  private final String myCapturedBrowserUrl;
  private final SlaveBrowser mySlaveBrowser;

  private JstdDebugBrowserInfo(@NotNull Pair<JSDebugEngine, WebBrowser> debugEngine,
                               @NotNull String capturedBrowserUrl,
                               @NotNull SlaveBrowser slaveBrowser) {
    myCapturedBrowserUrl = capturedBrowserUrl;
    myDebugEngine = debugEngine;
    mySlaveBrowser = slaveBrowser;
  }

  @NotNull
  public JSDebugEngine getDebugEngine() {
    return myDebugEngine.first;
  }

  @NotNull
  public WebBrowser getBrowser() {
    return myDebugEngine.second;
  }

  @NotNull
  public String getCapturedBrowserUrl() {
    return myCapturedBrowserUrl;
  }

  public void fixIfChrome(@NotNull ProcessHandler processHandler) {
    if (!(myDebugEngine.second.getFamily().equals(BrowserFamily.CHROME))) {
      return;
    }
    final AtomicBoolean done = new AtomicBoolean(false);
    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
        while (!done.get()) {
          mySlaveBrowser.heartBeat();
          try {
            //noinspection BusyWait
            Thread.sleep(5000);
          }
          catch (InterruptedException ignored) {
          }
        }
      }
    });
    processHandler.addProcessListener(new ProcessAdapter() {
      @Override
      public void processTerminated(ProcessEvent event) {
        done.set(true);
      }
    });
  }

  @Nullable
  public static JstdDebugBrowserInfo build(@NotNull JstdRunSettings settings) {
    JstdServerState jstdServerState = JstdServerState.getInstance();
    CapturedBrowsers browsers = jstdServerState.getCaptured();
    if (browsers == null) {
      return null;
    }

    List<JstdDebugBrowserInfo> debugBrowserInfos = new SmartList<JstdDebugBrowserInfo>();
    for (SlaveBrowser slaveBrowser : browsers.getSlaveBrowsers()) {
      Pair<JSDebugEngine, WebBrowser> engine = JSDebugEngine.findByBrowserName(slaveBrowser.getBrowserInfo().getName());
      if (engine != null) {
        debugBrowserInfos.add(new JstdDebugBrowserInfo(engine, slaveBrowser.getCaptureUrl(), slaveBrowser));
      }
    }
    if (debugBrowserInfos.size() == 1) {
      return debugBrowserInfos.get(0);
    }
    if (debugBrowserInfos.size() > 1) {
      WebBrowser preferredBrowser = settings.getPreferredDebugBrowser();
      for (JstdDebugBrowserInfo info : debugBrowserInfos) {
        if (preferredBrowser.equals(info.getBrowser())) {
          return info;
        }
      }
    }
    return null;
  }
}
