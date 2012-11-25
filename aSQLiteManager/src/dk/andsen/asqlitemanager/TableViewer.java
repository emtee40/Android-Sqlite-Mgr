/**
 * Part of aSQLiteManager (http://sourceforge.net/projects/asqlitemanager/)
 * a a SQLite Manager by andsen (http://sourceforge.net/users/andsen)
 *
 * Show informations and data for tables and views
 *
 * @author andsen
 *
 */

/*
 * Use SQL like this
 * SELECT rowid as rowid, * FROM programs
 * to get unique id for each record this might be a primary key but only
 * if this is a single field
 */
package dk.andsen.asqlitemanager;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import dk.andsen.RecordEditor.RecordEditorBuilder;
import dk.andsen.RecordEditor.types.TableField;
import dk.andsen.types.AField;
import dk.andsen.types.AField.FieldType;
import dk.andsen.types.Record;
import dk.andsen.types.Types;
import dk.andsen.types.ViewUpdateable;
import dk.andsen.utils.Utils;
/**
 * @author mh
 *
 */
public class TableViewer extends Activity implements OnClickListener {
	private Database _db = null;
	private String _table;
	private Context _cont;
	//private String _type = "Fields";
	private TableLayout _aTable;
	//Page offset
	private int offset = 0;
	private int limit = 15;
	private boolean _updateTable;
	private Button bUp;
	private Button bDwn;
	private int sourceType;
	// Where clause for filter
	protected String _where = "";
	private static final int MENU_DUMP_TABLE = 0;
	private static final int MENU_FIRST_REC = 1;
	private static final int MENU_LAST_REC = 2;
	private static final int MENU_FILETR = 3;
	private static final int MENU_TABLE_DEF = 4;
	
	private boolean _logging;
	private int _fontSize;
	protected String _order = "";
	private boolean _increasing = false;
	private boolean _canInsertInView = false;
	private boolean _canUpdateView = false;
	private boolean _canDeleteView = false;
	
	Record[] _data;
	private boolean _fieldMode = false;
	private boolean _showTip = false;
	private int _maxWidth;
	
	/*
	 * What is needed to allow editing form  table viewer 
	 * 
	 * When displaying records
	 * select rowid, t.* form table as t
	 * 
	 * to include the sqlite rowid
	 * 
	 * But only if a single field primary key does not exists
	 * If there does only
	 * select * from table
	 * 
	 * Then it is possible to update ... where rowid = x
	 * 
	 */

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.logD("TableViewer onCreate", _logging);
		_logging = Prefs.getLogging(this);
		_fontSize = Prefs.getFontSize(this);
		setContentView(R.layout.table_viewer);
		TextView tvDB = (TextView)this.findViewById(R.id.TableToView);
		Button bFields = (Button) this.findViewById(R.id.Fields);
		Button bData = (Button) this.findViewById(R.id.Data);
		Button bNewRec = (Button) this.findViewById(R.id.NewRec);
		bUp = (Button) this.findViewById(R.id.PgUp);
		bDwn = (Button) this.findViewById(R.id.PgDwn);
		bUp.setOnClickListener(this);
		bDwn.setOnClickListener(this);
		bUp.setVisibility(View.GONE);
		bDwn.setVisibility(View.GONE);
		_cont = this;
		limit = Prefs.getPageSize(this);
		bFields.setOnClickListener(this);
		bData.setOnClickListener(this);
		bNewRec.setOnClickListener(this);
		Bundle extras = getIntent().getExtras();
		if(extras !=null)
		{
			_cont = tvDB.getContext();
			sourceType = extras.getInt("type");
			Utils.logD("Opening database", _logging);
			_table = extras.getString("Table");
			_db = aSQLiteManager.database;
			Utils.logD("Database open", _logging);
			if (sourceType == Types.TABLE) {
				tvDB.setText(getString(R.string.DBTable) + " " + _table);
			}
			else if (sourceType == Types.VIEW) {
				tvDB.setText(getString(R.string.DBView) + " " + _table);
			}

			if (savedInstanceState != null) {
				Utils.logD("TableViewer RestoreInstanceState", _logging);
				offset = savedInstanceState.getInt("PageOffset", 0);
				Utils.logD("Offset " + offset, _logging);
				_where = savedInstanceState.getString("WhereClause");
				if (_where == null)
					_where = "";
				Utils.logD("_where " + _where, _logging);

				_order = savedInstanceState.getString("Order");
				Utils.logD("_order " + _order, _logging);
				_increasing = savedInstanceState.getBoolean("Increasing");
				Utils.logD("_increasing " + _increasing, _logging);
				
				if (savedInstanceState.getBoolean("showTip")) {
					Utils.logD("showHint true", _logging);
					showTip(getText(R.string.Tip4), 4);
				}
			} else
				showTip(getText(R.string.Tip4), 4);

			switch(Prefs.getDefaultView(_cont)){
			case 2:
				updateButtons(true);
				onClick(bData);
				break;
			default:
				onClick(bFields);
			}
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		Utils.logD("TableViewer onSaveInstanceState", _logging);
	  // Save UI state changes to the savedInstanceState.
	  // This bundle will be passed to onCreate if the process is
	  // killed and restarted.
	  savedInstanceState.putBoolean("showTip", _showTip);
	  savedInstanceState.putInt("PageOffset", offset);
	  savedInstanceState.putString("WhereClause", _where);
	  savedInstanceState.putString("Order", _order);
	  savedInstanceState.putBoolean("Increasing", _increasing);
	  super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onPause() {
		Utils.logD("TableViewer onPause", _logging);
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		//_db.close();
		Utils.logD("TableViewer onDestroy", _logging);
		super.onDestroy();
	}

	@Override
	protected void onRestart() {
		Utils.logD("TableViewer onRestart", _logging);
		_db = aSQLiteManager.database;
		//_db = new Database(_dbPath, _cont);
		super.onRestart();
	}

	/* (non-Javadoc)
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View v) {
		int key = v.getId();
		_aTable=(TableLayout)findViewById(R.id.datagrid);
		//boolean _isView = false;
		if (key == R.id.Fields) {
			offset = 0;
			_fieldMode = true;
			String[][] data;
			_canInsertInView = false;
			_canUpdateView = false;
			_canDeleteView = false;
			try {
				String[] fieldNames = _db.getTableStructureHeadings(_table);
				setTitles(_aTable, fieldNames, false);
				data = _db.getTableStructure(_table);
				updateButtons(false);
				oldappendRows(_aTable, data, false);
			} catch (Exception e) {
				Utils.showException(e.getLocalizedMessage(), _cont);
				e.printStackTrace();
			}
			updateButtons(false);
		} else if (key == R.id.Data) {
			/*
			 * If not a query include rowid in data if no single field
			 * primary key exists
			 */
			_fieldMode = false;
			//offset = 0;
			updateButtons(true);
			checkForUpdateableView();
			fillDataTableWithArgs();
		} else if (key == R.id.NewRec) {
			addNewRecord();
		} else if (key == R.id.PgDwn) {
			int childs = _aTable.getChildCount();
			Utils.logD("Table childs: " + childs, _logging);
			if (childs >= limit) {  //  No more data on to display - no need to PgDwn
				offset += limit;
				fillDataTableWithArgs();
//				String [] fieldNames = _db.getFieldsNames(_table);
//				setTitles(_aTable, fieldNames, !_isView);
//				Record[] data = _db.getTableDataWithWhere(_table, _where, offset, limit, _isView);
//				appendRows(_aTable, data, !_isView);
			}
			Utils.logD("PgDwn:" + offset, _logging);
		} else if (key == R.id.PgUp) {
			offset -= limit;
			if (offset < 0)
				offset = 0;
			fillDataTableWithArgs();
//			String [] fieldNames = _db.getFieldsNames(_table);
//			setTitles(_aTable, fieldNames, !_isView);
//			Record[] data = _db.getTableDataWithWhere(_table, _where, offset, limit, _isView);
//			appendRows(_aTable, data, !_isView);
//			Utils.logD("PgUp: " + offset, _logging);
		}
	}
	
	private void updateData() {
		checkForUpdateableView();
		try {
			fillDataTableWithArgs();
			updateButtons(true);
		} catch (Exception e) {
			Utils.logE(e.getLocalizedMessage(), _logging);
			e.printStackTrace();
		}
	}

	private void addNewRecord() {
		if ( sourceType != Types.VIEW || _canInsertInView) {
			final RecordEditorBuilder re;
			TableField[] rec = _db.getEmptyRecord(_table);
			final Dialog dial = new Dialog(_cont);
			dial.setCancelable(false);
			dial.setContentView(R.layout.line_editor);
			dial.setTitle(getText(R.string.InsertNewRow));
			LinearLayout ll = (LinearLayout)dial.findViewById(R.id.LineEditor);
			re = new RecordEditorBuilder(rec, false, _cont, _db);
			re.setFieldNameWidth(200);
			re.setTreatEmptyFieldsAsNull(true);
			final ScrollView sv = re.getScrollView();
			final Button btnOK = new Button(_cont);
			btnOK.setText(getText(R.string.OK));
			btnOK.setLayoutParams(new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.FILL_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
			btnOK.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					if (v == btnOK) {
						String msg = re.checkInput(sv); 
						if (msg == null) {
							//Utils.logD("Record edited; " + rowid);
							TableField[] res = re.getEditedData(sv);
							try {
								_db.insertRecord(_table, res, _cont);
							} catch (Exception e) {
								Utils.showException(e.getLocalizedMessage(), sv.getContext());
							}
							dial.dismiss();
							_updateTable = true;
						}
						else
							Utils.showException(msg, sv.getContext());
					} 
				}
			});
			final Button btnCancel = new Button(_cont);
			btnCancel.setText(getText(R.string.Cancel));
			btnCancel.setLayoutParams(new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
			btnCancel.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					if (v == btnCancel) {
						dial.dismiss();
					}
				}
			});
			LinearLayout llButtons = new LinearLayout(_cont);
			llButtons.setOrientation(LinearLayout.HORIZONTAL);
			llButtons.setLayoutParams(new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT));
			llButtons.addView(btnOK);
			llButtons.addView(btnCancel);
			ll.addView(llButtons);
			ll.addView(sv);
			dial.show();
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (_updateTable) {
			onClick((Button) this.findViewById(R.id.Data));
		}
	}

	/**
	 * If paging = true show paging buttons otherwise not
	 * @param paging
	 */
	private void updateButtons(boolean paging) {
		if (paging) {
			bUp.setVisibility(View.VISIBLE);
			bDwn.setVisibility(View.VISIBLE);
		} else {
			bUp.setVisibility(View.GONE);
			bDwn.setVisibility(View.GONE);
		}
	}
	

	/**
	 * @param table The layout to add the rows to
	 * @param aTable true if it is a table and false if it is a view
	 * Append rows to the table grid
	 */
	private void newAppendRows(TableLayout table, final boolean aTable) {
		if (_data == null)
			return;
		int rowSize = _data.length;
		if (_data.length == 0)
			return;
		int colSize = _data[0].getFields().length;
		Utils.logD("Columns: " + colSize, _logging);
		//Go through all the rows of data
		for(int i = 0; i < rowSize; i++){
			final int rowNo = i;
			TableRow row = new TableRow(this);
			//Make every second row dark gray
			if (i%2 == 1)
				row.setBackgroundColor(Color.DKGRAY);
			_maxWidth = Prefs.getMaxWidth(_cont);
			//Go through all the columns
			for(int j = 0; j < colSize; j++){
				// if it is the first cell (and updateable) add a cell with "Edit" 
				if (j==0 && (aTable || (_canInsertInView || _canUpdateView || _canDeleteView))) {
					TextView c = addEditField(rowNo, aTable);
					row.addView(c);
				}
				if (!aTable || j > 0) {
					TextView c = new TextView(this);
					c.setTextSize(_fontSize);
					if (_maxWidth > 0)
						c.setMaxWidth(_maxWidth);
					if (aTable)
						c.setText(_data[i].getFields()[j].getFieldData());
					else
						c.setText(_data[i].getFields()[j].getFieldData());
					c.setBackgroundColor(getBackgroundColor(_data[i].getFields()[j].getFieldType(), (i%2 == 1)));
					c.setPadding(3, 3, 3, 3);
					// Adding a onClickListener to copy cell value to clip board
					c.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							String text = (String)((TextView)v).getText();
							ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
							clipboard.setText(text);
							Utils.toastMsg(_cont, getText(R.string.CopiedToClipboard).toString());
						}
					});
					row.addView(c);
				}
			}
			table.addView(row, new TableLayout.LayoutParams());
		}
	}
	
	private TextView addEditField(final int rowNo, final boolean aTable){
		Utils.logD("addEditField rowNo,  aTable: " + rowNo + ", " + aTable, _logging);
		TextView c = new TextView(this);
		if (_maxWidth > 0)
			c.setMaxWidth(_maxWidth);
		c.setTextSize(_fontSize);
		int id;
		// change to long?
		id = rowNo;
		//If it is a table use rowid as id (always in first position) if now a
		// a view use rowNo
		if (aTable) {
			if (_data[rowNo].getFields()[0].getFieldData() != "") {
				c.setHint(new Long(_data[rowNo].getFields()[0].getFieldData()).toString());
				c.setId(id);
			}
		} else {
			c.setHint(new Long(rowNo).toString());
			c.setId(id);
		}
		c.setPadding(3, 3, 3, 3);
		// TODO More efficient to make one OnClickListener and assign this to all records edit field?
		if(aTable || (_canUpdateView || _canDeleteView)) {  //_canInsertInView || 
			c.setText(getText(R.string.Edit));
			c.setOnClickListener(new OnClickListener() {
				//Edit or delete the selected record
				public void onClick(View v) {
					final RecordEditorBuilder re;
					TextView a = (TextView)v;
					final Long rowid;
					if (a.getHint() != null) {
						if (aTable) {
							 rowid = new Long(a.getHint().toString());
						} else {
							 rowid = new Long(a.getHint().toString());
						}
						//Utils.logD("rowId in hint", _logging);
					} else
						rowid = (long)rowNo - 1;
					Utils.logD("Ready to edit rowid " + rowid + " in table " + _table, _logging);
					TableField[] rec;
					if (aTable)
						rec = _db.getRecord(_table, rowid);
					else {
						int fields = 0;
						rec = _db.getEmptyRecord(_table);
						fields = rec.length;
						Record dat = _data[rowid.intValue()];
						for (int i = 0; i < fields; i++) {
							rec[i].setValue(dat.getFields()[i].getFieldData());
						}
						//TODO OK to UI must be handled as a special case during update
					}
					final Dialog dial = new Dialog(_cont);
					dial.setContentView(R.layout.line_editor);
					dial.setTitle(getText(R.string.EditDeleteRow) + " " + rowid);
					dial.setCancelable(false);
					LinearLayout ll = (LinearLayout)dial.findViewById(R.id.LineEditor);
					re = new RecordEditorBuilder(rec, true, _cont, _db);
					re.setFieldNameWidth(200);
					re.setTreatEmptyFieldsAsNull(true);
					final ScrollView sv = re.getScrollView();
					final Button btnOK = new Button(_cont);
					btnOK.setText(getText(R.string.OK));
					btnOK.setLayoutParams(new LinearLayout.LayoutParams(
							LinearLayout.LayoutParams.MATCH_PARENT,
							LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
					btnOK.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							Utils.logD("Edit record"	, _logging);
							if (v == btnOK) {
								String msg = re.checkInput(sv); 
								if (msg == null) {
									//Utils.logD("Record edited; " + rowid, _logging);
									TableField[] res = re.getEditedData(sv);
									if (_table.equals("sqlite_master")) {
										Utils.showMessage(getString(R.string.Error), getString(R.string.ROSystemTable), _cont);
									} else {
										if (aTable) {
											_db.updateRecord(_table, rowid, res, _cont);
										} else  {
											Record oldData = _data[rowid.intValue()];
											_db.updateViewRecord(_table, oldData, res, _cont);
										}
										_updateTable = true;
									}
									dial.dismiss();
								}
								else
									Utils.showException(msg, sv.getContext());
							} 
						}
					});
					final Button btnCancel = new Button(_cont);
					btnCancel.setText(getText(R.string.Cancel));
					btnCancel.setLayoutParams(new LinearLayout.LayoutParams(
							LinearLayout.LayoutParams.MATCH_PARENT,
							LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
					btnCancel.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							Utils.logD("Cancel edit", _logging);
							if (v == btnCancel) {
								dial.dismiss();
							}
						}
					});
					final Button btnDelete = new Button(_cont);
					btnDelete.setText(getText(R.string.Delete));
					btnDelete.setLayoutParams(new LinearLayout.LayoutParams(
							LinearLayout.LayoutParams.MATCH_PARENT,
							LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
					if (aTable || _canDeleteView)
						btnDelete.setEnabled(true);
					else
						btnDelete.setEnabled(false);
					btnDelete.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							Utils.logD("Delete record", _logging);
							if (v == btnDelete) {
								if (aTable) {
									_db.deleteRecord(_table, rowid, _cont);
								} else {
									Record oldData = _data[rowid.intValue()];
									_db.deleteViewRecord(_table, oldData, _cont);
								}
								_updateTable = true;
								dial.dismiss();
							}
						}
					});
					LinearLayout llButtons = new LinearLayout(_cont);
					llButtons.setOrientation(LinearLayout.HORIZONTAL);
					llButtons.setLayoutParams(new LinearLayout.LayoutParams(
							LinearLayout.LayoutParams.MATCH_PARENT,
							LinearLayout.LayoutParams.WRAP_CONTENT));
					llButtons.addView(btnOK);
					llButtons.addView(btnCancel);
					llButtons.addView(btnDelete);
					ll.addView(llButtons);
					ll.addView(sv);
					dial.show();
				}
			});
		} else {
			c.setText(getText(R.string.Edit));
			c.setPaintFlags(c.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
		}
		return c;
	}
	
	
	private int getBackgroundColor(FieldType fieldType, boolean light) {
		int color = 0;
		if (fieldType == AField.FieldType.NULL) {
			if (light)
				color = Color.parseColor("#760000");
			else
				color = Color.parseColor("#400000");
		} else if (fieldType == AField.FieldType.TEXT) {
			if (light)
				color = Color.parseColor("#000075");
			else
				color = Color.parseColor("#000040");
		} else if (fieldType == AField.FieldType.INTEGER) {
			if (light)
				color = Color.parseColor("#007500");
			else
				color = Color.parseColor("#004000");
		} else if (fieldType == AField.FieldType.REAL) {
			if (light)
				color = Color.parseColor("#507550");
			else
				color = Color.parseColor("#254025");
		} else if (fieldType == AField.FieldType.BLOB) {
			if (light)
				color = Color.DKGRAY; // .parseColor("#509a4a");
			else
				color = Color.BLACK; // parseColor("#458540");
		}
		return color;
	}

	/**
	 * Add a row to the table
	 * @param table
	 * @param data
	 */
	private void oldappendRows(TableLayout table, String[][] data, boolean allowEdit) {
		int rowSize=data.length;
		int colSize=(data.length>0)?data[0].length:0;
		for(int i=0; i<rowSize; i++){
			TableRow row = new TableRow(this);
			row.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					// Edit button was clicked!
					Utils.logD("OnClick: " + v.getId(),_logging);
				}
			});
			if (i%2 == 1)
				row.setBackgroundColor(Color.DKGRAY);
			// TODO change rows to ConvertView
			// Adding all columns as TextView's should be changed to a ConvertView
			// as described here:
			// http://android-er.blogspot.com/2010/06/using-convertview-in-getview-to-make.html
			// or in android41cv dk.andsen.utils.MyArrayAdapter
			for(int j=0; j<colSize; j++){
				if (j==0 && allowEdit) {
					TextView c = new TextView(this);
					c.setText(getText(R.string.Edit));
					c.setTextSize(_fontSize);
					int id;
					id = i;
					c.setHint(new Long(data[i][j]).toString());
					c.setId(id);
					c.setPadding(3, 3, 3, 3);
					// TODO More efficient to make one OnClickListener and assign this to all records edit field?
					c.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							final RecordEditorBuilder re;
							TextView a = (TextView)v;
							final Long rowid = new Long(a.getHint().toString());
							Utils.logD("Ready to edit rowid " +v.getId() + " in table " + _table,_logging);
							TableField[] rec = _db.getRecord(_table, rowid);
							final Dialog dial = new Dialog(_cont);
							dial.setContentView(R.layout.line_editor);
							dial.setTitle(getText(R.string.EditRow) + " " + rowid);
							LinearLayout ll = (LinearLayout)dial.findViewById(R.id.LineEditor);
							re = new RecordEditorBuilder(rec, true, _cont, _db);
							re.setFieldNameWidth(200);
							re.setTreatEmptyFieldsAsNull(true);
							final ScrollView sv = re.getScrollView();
							final Button btnOK = new Button(_cont);
							btnOK.setText(getText(R.string.OK));
							btnOK.setLayoutParams(new LinearLayout.LayoutParams(
									LinearLayout.LayoutParams.FILL_PARENT,
									LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
							btnOK.setOnClickListener(new OnClickListener() {
								public void onClick(View v) {
									if (v == btnOK) {
										String msg = re.checkInput(sv); 
										if (msg == null) {
											//Utils.logD("Record edited; " + rowid);
											TableField[] res = re.getEditedData(sv);
											if (_table.equals("sqlite_master")) {
												Utils.showMessage(getString(R.string.Error), getString(R.string.ROSystemTable), _cont);
											} else {
												_db.updateRecord(_table, rowid, res, _cont);
												_updateTable = true;
											}
											dial.dismiss();
										}
										else
											Utils.showException(msg, sv.getContext());
									} 
								}
							});
							final Button btnCancel = new Button(_cont);
							btnCancel.setText(getText(R.string.Cancel));
							btnCancel.setLayoutParams(new LinearLayout.LayoutParams(
									LinearLayout.LayoutParams.FILL_PARENT,
									LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
							btnCancel.setOnClickListener(new OnClickListener() {
								public void onClick(View v) {
									if (v == btnCancel) {
										dial.dismiss();
									}
								}
							});
							LinearLayout llButtons = new LinearLayout(_cont);
							llButtons.setOrientation(LinearLayout.HORIZONTAL);
							llButtons.setLayoutParams(new LinearLayout.LayoutParams(
									LinearLayout.LayoutParams.FILL_PARENT,
									LinearLayout.LayoutParams.WRAP_CONTENT));
							llButtons.addView(btnOK);
							llButtons.addView(btnCancel);
							ll.addView(llButtons);
							ll.addView(sv);
							dial.show();
						}
					});
					row.addView(c);
				} else {
					TextView c = new TextView(this);
					c.setText(data[i][j]);
					c.setTextSize(_fontSize);
					c.setPadding(3, 3, 3, 3);
					c.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							String text = (String)((TextView)v).getText();
							ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
							clipboard.setText(text);
							Utils.toastMsg(_cont, "Text copied to clip board");
						}
					});
					row.addView(c);
				}

			}
			table.addView(row, new TableLayout.LayoutParams());
		}
	}

	/**
	 * Add titles to the columns
	 * @param table
	 * @param titles
	 */
	private void setTitles(TableLayout table, String[] titles, boolean allowEdit) {
		int rowSize=titles.length;
		table.removeAllViews();
		TableRow row = new TableRow(this);
		row.setBackgroundColor(Color.BLUE);
		if (allowEdit || (_canInsertInView || _canUpdateView || _canDeleteView)) {
			TextView c = new TextView(this);
			c.setTextSize(_fontSize);
			c.setTextAppearance(this, Typeface.BOLD);
			c.setPadding(3, 3, 3, 3);
			if (allowEdit || _canInsertInView) {
				c.setText(getText(R.string.New));
				c.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						final RecordEditorBuilder re;
						TableField[] rec = _db.getEmptyRecord(_table);
						final Dialog dial = new Dialog(_cont);
						dial.setContentView(R.layout.line_editor);
						dial.setTitle(getText(R.string.InsertNewRow));
						LinearLayout ll = (LinearLayout)dial.findViewById(R.id.LineEditor);
						re = new RecordEditorBuilder(rec, false, _cont, _db);
						re.setFieldNameWidth(200);
						re.setTreatEmptyFieldsAsNull(true);
						final ScrollView sv = re.getScrollView();
						final Button btnOK = new Button(_cont);
						btnOK.setText(getText(R.string.OK));
						btnOK.setLayoutParams(new LinearLayout.LayoutParams(
								LinearLayout.LayoutParams.FILL_PARENT,
								LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
						btnOK.setOnClickListener(new OnClickListener() {
							public void onClick(View v) {
								if (v == btnOK) {
									String msg = re.checkInput(sv); 
									if (msg == null) {
										//Utils.logD("Record edited; " + rowid);
										TableField[] res = re.getEditedData(sv);
										_db.insertRecord(_table, res, _cont);
										dial.dismiss();
										_updateTable = true;
									}
									else
										Utils.showException(msg, sv.getContext());
								} 
							}
						});
						final Button btnCancel = new Button(_cont);
						btnCancel.setText(getText(R.string.Cancel));
						btnCancel.setLayoutParams(new LinearLayout.LayoutParams(
								LinearLayout.LayoutParams.FILL_PARENT,
								LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
						btnCancel.setOnClickListener(new OnClickListener() {
							public void onClick(View v) {
								if (v == btnCancel) {
									dial.dismiss();
								}
							}
						});
						LinearLayout llButtons = new LinearLayout(_cont);
						llButtons.setOrientation(LinearLayout.HORIZONTAL);
						llButtons.setLayoutParams(new LinearLayout.LayoutParams(
								LinearLayout.LayoutParams.FILL_PARENT,
								LinearLayout.LayoutParams.WRAP_CONTENT));
						llButtons.addView(btnOK);
						llButtons.addView(btnCancel);
						ll.addView(llButtons);
						ll.addView(sv);
						dial.show();
					}
				});
			}
			row.addView(c);
		}
		for(int i=0; i<rowSize; i++){
			TextView c = new TextView(this);
			c.setTextSize(_fontSize);
			c.setTextAppearance(this, Typeface.BOLD);
			String title = titles[i];
			if (title.equals(_order))
				if (_increasing)
					title += " ↓";
				else
					title += " ↑";
			c.setText(title);
			c.setPadding(3, 3, 3, 3);
			if (!_fieldMode)
			c.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					String newOrder = ((TextView) v).getText().toString();
					if (newOrder.endsWith("↑") || newOrder.endsWith("↓"))
						newOrder = newOrder.substring(0, newOrder.length() - 2);
					Utils.logD("newOrder: " + newOrder, _logging);
					// if same field clicked twice reverse sorting
					if (newOrder.equals(_order)) {
						_increasing = !_increasing; 
					} else {
						_order  = newOrder;
						_increasing = true; 
					}
					fillDataTableWithArgs();
					updateData();
					Utils.logD("Sort by " + _order + " increasing = " + _increasing, _logging);
				}
			});
			row.addView(c);
		}
		table.addView(row, new TableLayout.LayoutParams());
	}
	
	/**
	 * Show a tip if not disabled
	 * @param tip
	 *          a CharSequence with the tip
	 * @param tipNo
	 *          a int with the tip number
	 */
	private void showTip(CharSequence tip, final int tipNo) {
		Utils.logD("Show Tip	" + tipNo, _logging);
		final boolean logging = Prefs.getLogging(_cont);
		Utils.logD("TipNo " + tipNo, logging);
		SharedPreferences prefs = _cont.getSharedPreferences(
				"dk.andsen.asqlitemanager_tips", Context.MODE_PRIVATE);
		boolean showTip = prefs.getBoolean("TipNo" + tipNo, true);
		if (showTip) {
			final Dialog dial = new Dialog(_cont);
			dial.setContentView(R.layout.tip);
			dial.setTitle(R.string.Tip);
			Button _btOK = (Button) dial.findViewById(R.id.OK);
			TextView tvTip = (TextView) dial.findViewById(R.id.TextViewTip);
			tvTip.setText(tip);
			_btOK.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					CheckBox _remember = (CheckBox) dial.findViewById(R.id.ShowTipAgain);
					_remember.setText(R.string.ShowTipAgain);
					SharedPreferences prefs = _cont.getSharedPreferences(
							"dk.andsen.asqlitemanager_tips", Context.MODE_PRIVATE);
					Editor edt = prefs.edit();
					Utils.logD("Show again " + _remember.isChecked(), logging);
					edt.putBoolean("TipNo" + tipNo, _remember.isChecked());
					edt.commit();
					_showTip = false;
					dial.dismiss();
				}
			});
			_showTip = true;
			dial.show();
		}
	}

	/*
	 *  Creates the menu items
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_FIRST_REC, 0, R.string.First);
		menu.add(0, MENU_LAST_REC, 1, R.string.Last);
		menu.add(0, MENU_FILETR, 2, R.string.Filter);
		menu.add(0, MENU_DUMP_TABLE, 3, R.string.DumpTable);
		menu.add(0, MENU_TABLE_DEF, 4, R.string.TableDef);
		return true;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
    case MENU_DUMP_TABLE:
    	// Dump table to .sql file
    	if (_db.exportTable(_table)) {
    		Utils.toastMsg(this, String.format(this.getString(R.string.TableDumpep) , _table));
      	return true;
    	}
    	else
    	  Utils.toastMsg(this, this.getString(R.string.DumpFailed));
    	return true;
    case MENU_FIRST_REC:
    	offset = 0;
    	fillDataTableWithArgs();
    	return true;
    case MENU_LAST_REC:
			int childs = _db.getNoOfRecords(_table, _where); 
			Utils.logD("Records = " + childs, _logging);
			offset = childs - limit;
			fillDataTableWithArgs();
    	return true;
    case MENU_FILETR:
    	buildFilerMenu(_table);
    	return true;
    case MENU_TABLE_DEF:
    	getTableDefinition();
    	return true;
		}
		return false;
	}
	
	private void getTableDefinition() {
		offset = 0;
		if (_db == null) {
			Utils.showMessage(getString(R.string.Error), 
					getString(R.string.NoDatabaseOpen), _cont);
		} else {
			String [] fieldNames = {"SQL"};
			setTitles(_aTable, fieldNames, false);
			String [][] data = _db.getSQL(_table); 
			updateButtons(false);
			oldappendRows(_aTable, data, false);
		}
	}

	private void buildFilerMenu(String _table2) {
		final Dialog dial = new Dialog(_cont);
		dial.setTitle(R.string.Filter);
		ScrollView sv = new ScrollView(_cont);
		sv.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
		sv.setPadding(5, 5, 5, 5);
		LinearLayout lmain = new LinearLayout(_cont);
		lmain.setOrientation(LinearLayout.VERTICAL);
		lmain.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		TextView tv = new TextView(_cont);
		tv.setText(R.string.EnterWhere);
		tv.setPadding(5, 5, 5, 5);
		lmain.addView(tv);
		final EditText et = new EditText(_cont);
		et.setText(_where);
		lmain.addView(et);
		Button btn = new Button(_cont);
		btn.setText(R.string.OK);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String where = et.getText().toString();
				dial.dismiss();
				offset = 0;
				if (where.trim().equals("")) {
					_where = "";
				} else {
					_where = where;
				}
				fillDataTableWithArgs();
			}});
		lmain.addView(btn);
		dial.setContentView(lmain);
		//.setPositiveButton(getText(R.string.OK), new DialogButtonClickHandler())
		dial.show();
	}

	/**
	 * 
	 */
	private void fillDataTableWithArgs() {
		boolean isUnUpdateableView = false;
		if (sourceType == Types.VIEW) {
				isUnUpdateableView = true;
		} else if (sourceType == Types.TABLE) {
			isUnUpdateableView = false;
		}
		String order = "";
		if (!_order.equals("")) {
			order = " order by [" + _order;
			if (_increasing)
				order += "] ASC";
			else
				order += "] DESC ";
		}
		//Record[] data = _db.getTableDataWithWhere(_table, _where, order, offset, limit, isUnUpdateableView);
		_data = _db.getTableDataWithWhere(_table, _where, order, offset, limit, isUnUpdateableView);
		setTitles(_aTable, _db.getFieldsNames(_table), !isUnUpdateableView);
		newAppendRows(_aTable, !isUnUpdateableView);
		Utils.logD("where = " + _where, _logging);
	}

	public class DialogButtonClickHandler implements DialogInterface.OnClickListener {
		public void onClick( DialogInterface dialog, int clicked )
		{
			Utils.logD("Dialog: " + dialog.getClass().getName(), _logging);
			switch(clicked)
			{
			case DialogInterface.BUTTON_POSITIVE:

				Utils.showMessage("Debug", "Filter", _cont);
				break;
			}
		}
	}

	/**
	 * Set the two boolean variables canInsertInView and canUpdateView
	 * If the view has instead of insert or instead of update triggers 
	 */
	private void checkForUpdateableView() {
		Utils.logD("_table = " + _table, _logging);
		ViewUpdateable upd = _db.isViewUpdatable(_table);
		_canInsertInView = upd.isInsertable();
		Utils.logD("Insertable: " + _canInsertInView, _logging);
		_canUpdateView = upd.isUpdateable();
		Utils.logD("Updateable: " + _canUpdateView, _logging);
		_canDeleteView = upd.isDeleteable();
		Utils.logD("Deleteable: " + _canDeleteView, _logging);
	}

}