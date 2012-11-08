package dk.andsen.asqlitemanager;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import dk.andsen.utils.Utils;

public class FilterBuilder  extends Activity implements OnClickListener  {
	private Context _cont;
	private boolean _logging;
	
	private EditText etValue;
	private Spinner spField;
	private Spinner spHvadHedderDe;
	private Button btnAdd;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		_cont = this;
		_logging = Prefs.getLogging(_cont);
		Utils.logD("onCreate", _logging);
		setContentView(R.layout.filter_wizard);
		setUpUI();
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			// we are editing an existing filter
		}
	}

	public void onClick(View arg0) {

		
	}
	
	private void setUpUI() {
		etValue = (EditText) findViewById(R.id.FilterETValue);
		spField = (Spinner) findViewById(R.id.FilterSPField);
		spHvadHedderDe = (Spinner) findViewById(R.id.FilterSPHvadHedderDe);
		btnAdd = (Button) findViewById(R.id.FilterBTNAdd);
		btnAdd.setOnClickListener(this);
	}

}
