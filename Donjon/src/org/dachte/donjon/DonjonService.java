package org.dachte.donjon;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class DonjonService extends Service {

	// Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        DonjonService getService() {
            // Return this instance of LocalService so clients can call public methods
            return DonjonService.this;
        }
    }

    public String docmd(String input)
	    {
	    DonjonParser engine = new DonjonParser(this); // XXX Or should we keep our existing one statically?
	    String ret = engine.parser(input);
	    return ret;
	    }
    
    public String getprompt()
	    {
	    DonjonParser engine = new DonjonParser(this);
	    Integer state = engine.get_gamestate();
	    
	    if(state < 2)
		    {return ">";}
	    else if(state==2)
		    {return "(Name Entry)\n>";}
	    else if(state==3)
		    {
		    String roomname = engine.get_roomname();
		    return roomname + "\n>";
		    }
	    else
		    {return null;} // Should never happen, so let's let java get angry if it does
	    }
	
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
