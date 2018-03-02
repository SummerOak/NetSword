package com.chedifier.netsword.swing;

import java.awt.Color;
import java.nio.channels.SelectionKey;
import java.util.HashMap;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.chedifier.ladder.base.Log;
import com.chedifier.ladder.iface.SProxyIface.STATE;

public class ConnsTableModel extends AbstractTableModel{
	private static final String TAG = "ConnsTableModel";
	private static final long serialVersionUID = 1L;
	
	private static final int[] HEADS = new int[] {COLUMN.ID,COLUMN.CLIENT,COLUMN.DOMAIN,COLUMN.PORT,COLUMN.STATE,COLUMN.SRC_IN,COLUMN.SRC_OUT,COLUMN.DEST_IN,COLUMN.DEST_OUT,COLUMN.ERR};
	private static final HashMap<Integer,Head> sHeadIdx = new HashMap<>();
	static {
		sHeadIdx.put(COLUMN.ID, 		new Head(COLUMN.ID,			"ID",			int.class,		30));
		sHeadIdx.put(COLUMN.CLIENT, 	new Head(COLUMN.CLIENT,		"CLIENT",		String.class,	100));
		sHeadIdx.put(COLUMN.DOMAIN, 	new Head(COLUMN.DOMAIN,		"DOMAIN",		String.class,	200));
		sHeadIdx.put(COLUMN.IP, 		new Head(COLUMN.IP,			"IP",			String.class,	100));
		sHeadIdx.put(COLUMN.PORT, 	new Head(COLUMN.PORT,		"PORT",			int.class,		50));
		sHeadIdx.put(COLUMN.STATE, 	new Head(COLUMN.STATE,		"STATE",			String.class	,	50));
		sHeadIdx.put(COLUMN.SRC_IN, 	new Head(COLUMN.SRC_IN,		"SI",			long.class,		80));
		sHeadIdx.put(COLUMN.SRC_OUT, new Head(COLUMN.SRC_OUT,		"SO",			long.class,		80));
		sHeadIdx.put(COLUMN.DEST_IN, new Head(COLUMN.DEST_IN,		"DI",			long.class,		80));
		sHeadIdx.put(COLUMN.DEST_OUT, new Head(COLUMN.DEST_OUT,	"DO",			long.class,		80));
		sHeadIdx.put(COLUMN.ERR, 	new Head(COLUMN.ERR,			"ERR",			String.class		));
	}
	
	private Object mLock = new Object();
	private HashMap<Integer, ConnItem> mConns = new HashMap<>();
	private HashMap<Integer,Integer> mRowMapping = new HashMap<>();
	private HashMap<Integer,Integer> mItemMapping = new HashMap<>();
	
	private static final int MAX_ROWS = 500;
	
	public ConnsTableModel() {
		
	}
	
	public void initColumnWidth(JTable t) {
		if(t != null) {
			for(int i=0;i<HEADS.length;i++) {
				Head h = sHeadIdx.get(HEADS[i]);
				if(h != null && h.width > 0) {					
					t.getColumnModel().getColumn(i).setPreferredWidth(h.width);
				}
			}
		}
	}

	@Override
	public int getRowCount() {
		synchronized (mLock) {			
			return mConns.size();
		}
	}

	@Override
	public int getColumnCount() {
		return HEADS.length;
	}

	@Override
	public String getColumnName(int columnIndex) {
		if(0 <= columnIndex&&columnIndex<HEADS.length) {			
			Head h = sHeadIdx.get(HEADS[columnIndex]);
			if(h != null) {
				return h.name;
			}
		}
		
		return "";
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if(0 <= columnIndex&&columnIndex<HEADS.length) {			
			Head h = sHeadIdx.get(HEADS[columnIndex]);
			if(h != null) {
				return h.type;
			}
		}
		
		return String.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		ConnItem item = getConnByRow(rowIndex);
		if(item != null) {
			return item.getColumn(columnIndex);
		}
		
		return null;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		//unsupported operation!
	}
	
	public void addConn(int id) {
		addItem(new ConnItem(id));
		int rowNum = getRowCount();
		map(id,rowNum - 1);
		
		fireTableRowsInserted(rowNum-1, rowNum-1);
	}
	
	public void updateField(int id,int fieldIdx,Object value) {
		Log.d(TAG, "updateField " + id + " fieldIdx " + fieldIdx + " value " + value);
		ConnItem item = getConnById(id);
		if(item != null) {
			item.setField(fieldIdx, value);
			
			int r = getRow(id);
			Log.d(TAG, "update row " + r);
			if(r >= 0) {
				if(needUpdateTotalRow(fieldIdx)) {
					fireTableRowsUpdated(r, r);
				}else {					
					fireTableCellUpdated(r, fieldIdx);
				}
			}
		}else {
			Log.d(TAG, "not exist item with id " + id);
		}
	}
	
	private boolean needUpdateTotalRow(int column) {
		switch(column) {
		case COLUMN.STATE:
			return true;
		}
		
		return false;
	}
	
	public void updatePortOps(int id,boolean src,int ops) {
		ConnItem item = getConnById(id);
		if(item != null) {
			if(src) {				
				item.srcOps = ops;
			}else {
				item.destOps = ops;
			}
			
			Log.i(TAG, "updatePortOps: " + id + " src " + item.srcOps + " dest " + item.destOps + " ops" + ops);
			
			int[] columns = null;
			if(src) {
				columns = new int[] {COLUMN.SRC_IN,COLUMN.SRC_OUT};
			}else {
				columns = new int[] {COLUMN.DEST_IN,COLUMN.DEST_OUT};
			}
			updateItemUI(id, columns);
		}
	}
	
	public Color getCellBackgroundColor(int r,int c) {
		ConnItem item = getConnByRow(r);
		if(item == null) {
			return null;
		}
		
		if(item.state == STATE.TERMINATE) {
			return Color.GRAY;
		}
		
		
		if(HEADS[c] == COLUMN.SRC_IN) {
			if(item != null && (item.srcOps&SelectionKey.OP_READ) == 0) {
				return Color.RED;
			}
		}else if(HEADS[c] == COLUMN.SRC_OUT){
			if(item != null && (item.srcOps&SelectionKey.OP_WRITE) == 0) {
				return Color.RED;
			}
		}else if(HEADS[c] == COLUMN.DEST_IN){
			if(item != null && (item.destOps&SelectionKey.OP_READ) == 0) {
				return Color.RED;
			}
		}else if(HEADS[c] == COLUMN.DEST_OUT){
			if(item != null && (item.destOps&SelectionKey.OP_WRITE) == 0) {
				return Color.RED;
			}
		}
		
		return null;
	}
	
	private void updateItemUI(int id,int[] columns) {
		int row = getRow(id);
		if(row >= 0) {
			if(columns != null && columns.length > 0) {
				for(int head:columns) {
					int c = getColumnIndex(head);
					if(c >= 0) {
						fireTableCellUpdated(row, c);
					}
				}
			}else {
				fireTableRowsUpdated(row, row);
			}
		}
	}
	
	private int getColumnIndex(int id) {
		for(int i=0;i<HEADS.length;i++) {
			if(HEADS[i] == id) {
				return i;
			}
		}
		
		return -1;
	}
	
	private void addItem(ConnItem item) {
		if(item != null) {
			synchronized (mLock) {			
				mConns.put(item.id, item);
			}
		}
	}
	
	public void clear() {
		synchronized (mLock) {
			mConns.clear();
			mItemMapping.clear();
			mRowMapping.clear();
		}
		
		fireTableDataChanged();
	}
	
	public int getRow(int id) {
		synchronized (mLock) {
			Integer r = mItemMapping.get(id);
			return r==null?-1:r;
		}
	}
	
	private void map(int id,int row) {
		synchronized (mLock) {
			mItemMapping.put(id, row);
			mRowMapping.put(row, id);
		}
	}
	
	private int getConnId(int row) {
		synchronized (mLock) {			
			Integer id = mRowMapping.get(row);
			return id == null?-1:id;
		}
	}
	
	private ConnItem getConnByRow(int row) {
		return getConnById(getConnId(row));
	}
	
	private ConnItem getConnById(int id) {
		synchronized (mLock) {
			ConnItem item = mConns.get(id);
			return item;
		}
	}
	
	private static class Head{
		public int id;
		public String name;
		public Class<?> type;
		public int width = 0;
		
		public Head(int id,String name,Class<?> type) {
			this(id,name,type,0);
		}
		
		public Head(int id,String name,Class<?> type,int w) {
			this.id = id;
			this.name = name;
			this.type = type;
			this.width = w;
		}
	}
	
	private static class ConnItem{
		public int id = -1;
		public String client = null;
		public String domain = null;
		public String ip = null;
		public int port = -1;
		public int state = -1;
		public long srcIn = 0;
		public long srcOut = 0;
		public long destIn = 0;
		public long destOut = 0;
		public String err = null;
		
		public int srcOps = 0;
		public int destOps = 0;
		
		public long diedTime = 0;
		
		public ConnItem(int id) {
			this.id = id;
			this.state = STATE.INIT;
		}
		
		public void setField(int column,Object value) {
			switch (column) {
			case COLUMN.ID: 			if(value instanceof Integer)	id = (int)value; 			break;
			case COLUMN.CLIENT: 		if(value instanceof String)	client = (String)value; 		break;
			case COLUMN.DOMAIN: 		if(value instanceof String) 	domain = (String)value; 		break;
			case COLUMN.IP: 			if(value instanceof String) 	ip = (String)value;	 		break;
			case COLUMN.PORT: 		if(value instanceof Integer) port = (int)value;			break;
			case COLUMN.STATE:		if(value instanceof Integer) state = (int)value;			break;
			case COLUMN.SRC_IN: 		if(value instanceof Long) srcIn = (long)value;			break;
			case COLUMN.SRC_OUT:		if(value instanceof Long) srcOut = (long)value;			break;
			case COLUMN.DEST_IN:		if(value instanceof Long) destIn = (long)value;			break;
			case COLUMN.DEST_OUT:	if(value instanceof Long) destOut = (long)value;			break;
			case COLUMN.ERR:			if(value instanceof String) err = (String)value;			break;
			}
		}
		
		public Object getColumn(int column) {
			if(0<=column && column<HEADS.length) {
				switch (HEADS[column]) {
				case COLUMN.ID: 			return id;
				case COLUMN.CLIENT: 		return client;
				case COLUMN.DOMAIN: 		return domain;
				case COLUMN.IP: 			return ip;
				case COLUMN.PORT: 		return port>0?port:null;
				case COLUMN.STATE:		return getStateDesc(state);
				case COLUMN.SRC_IN: 		return srcIn;
				case COLUMN.SRC_OUT:		return srcOut;
				case COLUMN.DEST_IN:		return destIn;
				case COLUMN.DEST_OUT:	return destOut;
				case COLUMN.ERR:			return err;

				default:return null;
				}
			}
			
			return null;
		}
		
		private String getStateDesc(int state) {
			switch (state) {
				case STATE.INIT: return "INI";
				case STATE.VERIFY: return "VFY";
				case STATE.CONN: return "CONN";
				case STATE.TRANS: return "TRS";
				case STATE.TERMINATE: return "TMN";
		
				default: return "";
			}
		}
	}
	
	public static final class COLUMN{
		public static final int ID 		= 0;
		public static final int CLIENT 	= 1;
		public static final int DOMAIN 	= 2;
		public static final int IP 		= 3;
		public static final int PORT 	= 4;
		public static final int STATE 	= 5;
		public static final int SRC_IN 	= 6;
		public static final int SRC_OUT 	= 7;
		public static final int DEST_IN 	= 8;
		public static final int DEST_OUT = 9;
		public static final int ERR 		= 10;
	}

}
