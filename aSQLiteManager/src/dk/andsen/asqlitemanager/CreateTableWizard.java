package dk.andsen.asqlitemanager;

import java.util.ArrayList;
import java.util.List;

import dk.andsen.utils.Utils;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

public class CreateTableWizard extends Activity implements OnClickListener {

	Button newTabNewField;
	Button newTabCancel;
	Button newTabOk;
	EditText newTabTabName;
	private boolean _logging;
	private Context _cont;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_cont = this;
		Utils.logD("onCreate", _logging);
		_logging = Prefs.getLogging(this);
		setContentView(R.layout.tablewizard);
		// fldList contains the list of fields
		final List<String> fldList = new ArrayList<String>();
		final List<String> fkList = new ArrayList<String>();
		final LinearLayout newTabSV;
		newTabNewField = (Button) this.findViewById(R.id.tabWizAddField);
		newTabNewField.setOnClickListener(this);
		newTabCancel = (Button) this.findViewById(R.id.tabWizCancel);
		newTabCancel.setOnClickListener(this);
		newTabOk = (Button) this.findViewById(R.id.tabWizOK);
		newTabOk.setOnClickListener(this);
		newTabSV = (LinearLayout) this.findViewById(R.id.tabWizSV);
		newTabTabName = (EditText) this.findViewById(R.id.tabWizTabName);
		this.setTitle(getText(R.string.CreateTable));

	}
	
	public void onClick(View v) {
		// TODO Auto-generated method stub
		Utils.logD("CreateTableWizard onClick " +v.getId(), _logging);
		if (v.getId() == newTabNewField.getId()) {
			
			Utils.logD("CreateTableWizard newTabNewField", _logging);

			Intent i = new Intent(this, CreateTableWizField.class);
			try {
				startActivityForResult(i, 55);
			} catch (Exception e) {
				Utils.logE("Error in CreateTableWizField", _logging);
				e.printStackTrace();
				Utils.showException("Plase report this error with descriptions of how to generate it", _cont);
			}
		} else if (v.getId() == newTabOk.getId()) {
			Utils.logD("CreateTableWizard OK", _logging);
			
		} else if (v.getId() == newTabCancel.getId()) {
			Utils.logD("CreateTableWizard Cancel", _logging);
			finish();
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (resultCode) {
		case 1:
			
			break;
		default:

			break;
		}
	}

}
