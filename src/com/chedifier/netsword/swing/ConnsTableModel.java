package com.chedifier.netsword.swing;

import java.awt.Color;
import java.nio.channels.SelectionKey;
import java.util.HashMap;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.iface.SProxyIface.STATE;

public class ConnsTableModel extends AbstractTableModel{
	private static final String TAG = "ConnsTableModel";
	private static final long serialVersionUID = 1L;
	
	private static final String HEAD_ID 		= "id";
	private static final String HEAD_CLIENT 	= "client";
	private static final String HEAD_DOMAIN 	= "domain";
	private static final String HEAD_IP 		= "ip";
	private static final String HEAD_PORT 	= "port";
	private static final String HEAD_STATE 	= "state";
	private static final String HEAD_SI 		= "si";
	private static final String HEAD_SO 		= "so";
	private static final String HEAD_DI 		= "di";
	private static final String HEAD_DO 		= "do";
	private static final String HEAD_ERR 	= "err";
	
	private static final String[] HEADS = new String[] {HEAD_ID,HEAD_CLIENT,HEAD_DOMAIN,HEAD_IP,HEAD_PORT,HEAD_STATE,HEAD_SI,HEAD_SO,HEAD_DI,HEAD_DO,HEAD_ERR};
	private static final HashMap<String,Head> sHeadIdx = new HashMap<>();
	static {
		sHeadIdx.put(HEAD_ID, 		new Head(HEAD_ID,		int.class,		30));
		sHeadIdx.put(HEAD_CLIENT, 	new Head(HEAD_CLIENT,	String.class,	100));
		sHeadIdx.put(HEAD_DOMAIN, 	new Head(HEAD_DOMAIN,	String.class,	200));
		sHeadIdx.put(HEAD_IP, 		new Head(HEAD_IP,		String.class,	100));
		sHeadIdx.put(HEAD_PORT, 		new Head(HEAD_PORT,		int.class,		50));
		sHeadIdx.put(HEAD_STATE, 	new Head(HEAD_STATE,		String.class		));
		sHeadIdx.put(HEAD_SI, 		new Head(HEAD_SI,		long.class,		80));
		sHeadIdx.put(HEAD_SO, 		new Head(HEAD_SO,		long.class,		80));
		sHeadIdx.put(HEAD_DI, 		new Head(HEAD_DI,		long.class,		80));
		sHeadIdx.put(HEAD_DO, 		new Head(HEAD_DO,		long.class,		80));
		sHeadIdx.put(HEAD_ERR, 		new Head(HEAD_ERR,		String.class		));
	}
	
	private HashMap<Integer, ConnItem> mConns = new HashMap<>();
	private HashMap<Integer,Integer> mRowMapping = new HashMap<>();
	private HashMap<Integer,Integer> mItemMapping = new HashMap<>();
	
	private static final int MAX_ROWS = 50;
	
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
		synchronized (mConns) {			
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
			
			Log.t(TAG, "updatePortOps: " + id + " src " + item.srcOps + " dest " + item.destOps + " ops" + ops);
			
			String[] columns = null;
			if(src) {
				columns = new String[] {HEAD_SI,HEAD_SO};
			}else {
				columns = new String[] {HEAD_DI,HEAD_DO};
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
		
		String head = getColumnName(c);
		if(HEAD_SI.equals(head)) {
			
			if(item != null && (item.srcOps&SelectionKey.OP_READ) == 0) {
				return Color.RED;
			}
		}else if(HEAD_SO.equals(head)){
			if(item != null && (item.srcOps&SelectionKey.OP_WRITE) == 0) {
				return Color.RED;
			}
		}else if(HEAD_DI.equals(head)){
			if(item != null && (item.destOps&SelectionKey.OP_READ) == 0) {
				return Color.RED;
			}
		}else if(HEAD_DO.equals(head)){
			if(item != null && (item.destOps&SelectionKey.OP_WRITE) == 0) {
				return Color.RED;
			}
		}
		
		return null;
	}
	
	private void updateItemUI(int id,String[] columns) {
		int row = getRow(id);
		if(row >= 0) {
			if(columns != null && columns.length > 0) {
				for(String head:columns) {
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
	
	private int getColumnIndex(String head) {
		for(int i=0;i<HEADS.length;i++) {
			if(HEADS[i].equals(head)) {
				return i;
			}
		}
		
		return -1;
	}
	
	private void addItem(ConnItem item) {
		if(item != null) {
			synchronized (mConns) {			
				mConns.put(item.id, item);
			}
		}
	}
	
	public int getRow(int id) {
		synchronized (mItemMapping) {
			Integer r = mItemMapping.get(id);
			return r==null?-1:r;
		}
	}
	
	private void map(int id,int row) {
		synchronized (mItemMapping) {
			mItemMapping.put(id, row);
			mRowMapping.put(row, id);
		}
	}
	
	private int getConnId(int row) {
		synchronized (mRowMapping) {			
			Integer id = mRowMapping.get(row);
			return id == null?-1:id;
		}
	}
	
	private ConnItem getConnByRow(int row) {
		return getConnById(getConnId(row));
	}
	
	private ConnItem getConnById(int id) {
		synchronized (mConns) {
			ConnItem item = mConns.get(id);
			return item;
		}
	}
	
	private static class Head{
		public String name;
		public Class<?> type;
		public int width = 0;
		
		public Head(String name,Class<?> type) {
			this(name,type,0);
		}
		
		public Head(String name,Class<?> type,int w) {
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
			switch (column) {
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
		
		private String getStateDesc(int state) {
			switch (state) {
				case STATE.INIT: return "init";
				case STATE.VERIFY: return "verify";
				case STATE.CONN: return "conn";
				case STATE.TRANS: return "trans";
				case STATE.TERMINATE: return "terminate";
		
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
