package org.dachte.donjon;

import org.dachte.donjon.DonjonService.LocalBinder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class Donjon extends Activity
{
    DonjonService DonjonServer; 
    boolean DSBound = false;
	
    @Override
    protected void onStart()
	    {
	    super.onStart();
	    Log.i("Donjon", "Attempting to bind service");
	    // Bind to LocalService
	    Intent intent = new Intent(this, DonjonService.class);
	    Boolean ok = bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	    if(! ok)
		    { Log.e("Donjon", "bindService() failed!");}
	    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection()
	    {
	    @Override
	    public void onServiceConnected(ComponentName className, IBinder service)
		    {
		    Log.i("Donjon", "Connected to bound service");
		    // We've bound to LocalService, cast the IBinder and get LocalService instance
		    LocalBinder binder = (LocalBinder) service;
		    DonjonServer = binder.getService();
		    DSBound = true;
		    }
	    @Override
	    public void onServiceDisconnected(ComponentName arg0)
		    {
		    DSBound = false;
		    }
	    };

    @Override
    protected void onStop()
	    {
	    super.onStop();
	    // Unbind from the service
	    if (DSBound)
		    {
		    unbindService(mConnection);
		    DSBound = false;
		    }
	    }

	
    @Override
    protected void onCreate(Bundle savedInstanceState)
	    {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_donjon);
	    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
	    {
	    // Inflate the menu; this adds items to the action bar if it is present.
	    getMenuInflater().inflate(R.menu.activity_donjon, menu);
	    return true;
	    }
    
    public void cmdSubmit(View view)
	{
    	// This sends the command out to the game engine, gets back the response,
    	// and draws it into the std_doubt field.

    	EditText thiscmdobj = (EditText) findViewById(R.id.std_inn);
    	String thiscmd = thiscmdobj.getText().toString();
    	Log.i("Donjon", "CMD[" + thiscmd + "]");
    	thiscmdobj.setText("");
	TextView console = (TextView) findViewById(R.id.std_doubt);
	console.append(thiscmd + "\n");
    	
    	if(DSBound)
    		{
    		String response;
    		response = DonjonServer.docmd(thiscmd);
  //  		response = "Faked response\n";
    		// Do something with it...
    		console.append(response); // Let the server figure out newlines.
    		// TODO: Add direction-dependent buttons and query them after every
    		// command, maybe location too? Or have that all passed over this channel?
    		response = DonjonServer.getprompt();
    		console.append(response);
    		}
    	else	{
    		console.append("Not connected\n");
    		}
    	}
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
	    {
	    super.onRestoreInstanceState(savedInstanceState);
	    TextView doubtHolder = (TextView) findViewById(R.id.std_doubt);
	    doubtHolder.setText(savedInstanceState.getString("ConsoleText"));
	    // Read values from the "savedInstanceState"-object and put them in your textview
	    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
	    {
    	    super.onSaveInstanceState(outState);
    	    TextView doubtHolder = (TextView) findViewById(R.id.std_doubt);
    	    outState.putString("ConsoleText", doubtHolder.getText().toString() );
	    }
}
