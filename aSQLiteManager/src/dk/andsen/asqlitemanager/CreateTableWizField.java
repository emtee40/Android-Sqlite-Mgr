package dk.andsen.asqlitemanager;

import dk.andsen.utils.Utils;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class CreateTableWizField extends Activity implements OnClickListener {

	private Context _cont;
	private boolean _logging = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_cont = this;
		Utils.logD("onCreate", _logging);
		_logging  = Prefs.getLogging(this);
		setContentView(R.layout.tablewizfield);
		
	}
		
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}

}
