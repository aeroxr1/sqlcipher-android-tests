package net.zetetic.tests;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.WindowManager;

import net.sqlcipher.CursorWindow;
import net.sqlcipher.CursorWindowAllocation;
import net.sqlcipher.database.SQLiteDatabase;
import net.zetetic.ZeteticApplication;

import java.util.ArrayList;
import java.util.List;

public class TestSuiteRunner extends AsyncTask<ResultNotifier, TestResult, Void> {

  String TAG = getClass().getSimpleName();
  private ResultNotifier notifier;
  private Activity activity;

  public TestSuiteRunner(Activity activity) {
    this.activity = activity;
    if (this.activity != null) {
      this.activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
  }

  @Override
  protected Void doInBackground(ResultNotifier... resultNotifiers) {
    this.notifier = resultNotifiers[0];
    Log.i(ZeteticApplication.TAG, String.format("Running test suite on %s platform", Build.CPU_ABI));
    runSuite();
    return null;
  }

  @Override
  protected void onProgressUpdate(TestResult... values) {
    notifier.send(values[0]);
  }

  @Override
  protected void onPostExecute(Void aVoid) {
    notifier.complete();
    if (this.activity != null) {
      this.activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
  }

  private void runSuite() {

    SQLiteDatabase.loadLibs(ZeteticApplication.getInstance());
    CursorWindowAllocation defaultAllocation = CursorWindow.getCursorWindowAllocation();
    for (SQLCipherTest test : getTestsToRun()) {
      try {
        CursorWindow.setCursorWindowAllocation(defaultAllocation);
        Log.i(ZeteticApplication.TAG, "Running test:" + test.getName());
        TestResult result = test.run();
        publishProgress(result);

      } catch (Throwable e) {
        Log.i(ZeteticApplication.TAG, e.toString());
        publishProgress(new TestResult(test.getName(), false, e.toString()));
      }
      finally {
        CursorWindow.setCursorWindowAllocation(defaultAllocation);
      }
    }
  }

  private List<SQLCipherTest> getTestsToRun() {
    List<SQLCipherTest> tests = new ArrayList<>();

    tests.add(new TestQueryCrash());

    return tests;
  }
}
