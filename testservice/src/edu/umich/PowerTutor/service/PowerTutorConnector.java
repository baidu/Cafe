package edu.umich.PowerTutor.service;

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
    private ICounterService          counterService;
    private CounterServiceConnection conn;
    private Intent                   serviceIntent;

    public PowerTutorConnector(Context context) {
        mContext = context;
    }

    private class CounterServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className, IBinder boundService) {
            counterService = ICounterService.Stub.asInterface((IBinder) boundService);
        }

        public void onServiceDisconnected(ComponentName className) {
            counterService = null;
            mContext.unbindService(conn);
            mContext.bindService(serviceIntent, conn, 0);
        }
    }

    public void connectToPowerTutor() {
        serviceIntent.setClassName(mContext, "edu.umich.PowerTutor.service.UMLoggerService");
        mContext.startService(serviceIntent);
        conn = new CounterServiceConnection();
        mContext.bindService(serviceIntent, conn, 0);
        try {
            counterService.hello();
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
        }
    }
}
