package edu.umich.PowerTutor.service;

import com.baidu.cafe.remote.Log;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2012-9-23
 * @version
 * @todo
 */
public class PowerTutorConnector {
    private Context                  mContext;
    private ICounterService          counterService = null;
    private CounterServiceConnection conn;
    private Intent                   serviceIntent  = new Intent();

    public PowerTutorConnector(Context context) {
        mContext = context;
        conn = new CounterServiceConnection();
    }

    private class CounterServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className, IBinder boundService) {
            counterService = ICounterService.Stub.asInterface(boundService);
        }

        public void onServiceDisconnected(ComponentName className) {
            counterService = null;
            mContext.unbindService(conn);
//                        mContext.bindService(serviceIntent, conn, 0);
        }
    }

    public void connectToPowerTutor() {
        serviceIntent.setClassName("edu.umich.PowerTutor", "edu.umich.PowerTutor.service.UMLoggerService");
        mContext.bindService(serviceIntent, conn, Context.BIND_AUTO_CREATE);
        int count = 0;
        try {
            while (null == counterService) {
                Thread.sleep(200);
                count++;
                if (count == 20) { //timeout = 4 seconds
                    Log.print("timeout 4 seconds!!!");
                    mContext.unbindService(conn);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            counterService.hello();
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
        }
    }
}
