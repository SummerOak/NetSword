package com.chedifier.netsword.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.StringUtils;


public class SwordUI implements KeyListener{
	private static final String TAG = "SwordUI";
	
	private static final int WIDNOW_WIDTH = 800;
	private static final int WINDOW_HEIGHT = 480;
	
	private boolean mIsLcoal;
	private String mTitle = "NetSword";
	private String mProxyHost = null;
	private int mProxyPort = 0;
	private int mLocalPort = 0;
	
	private Dimension mDimension;
	private JFrame mFrame;
	private JPanel mContent;
	private JTextField mBaseInfo = null;
	private JTextField mAliveConnCounter = null;
	private JTextField mMaxAlive = null;
	private JTextField mMemInfo = null;
	private JScrollPane mConnTableCnt = null;
	private JTable mConnTable = null;
	
	private long mMaxAliveNum = 0;
	
	private ConnsTableModel mConnModel;
	
	private ISwordUIEvent mListener;
	
	public static SwordUI build() {
		return new SwordUI();
	}
	
	private SwordUI() {
		mDimension = Toolkit.getDefaultToolkit().getScreenSize();
		mFrame = new JFrame(mTitle);
		mFrame.setLayout(new GridBagLayout());
		mFrame.setSize(WIDNOW_WIDTH,WINDOW_HEIGHT);  
		mFrame.setLocation(mDimension.width/2-mFrame.getSize().width/2, mDimension.height/2-mFrame.getSize().height/2);
		mFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);   
		mFrame.setResizable(false);
		mFrame.setFocusable(true);
		mFrame.addKeyListener(this);
		mFrame.addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent e) {
				if(mListener != null) {
					mListener.onSwordUIEvent(ISwordUIEvent.WINDOW_CLOSE);
				}
			}
		});
		
		mContent = new JPanel();
		mContent.setLayout(new GridBagLayout());
		
		mBaseInfo=new JTextField();  
		mBaseInfo.setEditable(false);
		mBaseInfo.setBackground(new Color(240,240,240));
		GridBagConstraints cnts = new GridBagConstraints();
		cnts.fill = GridBagConstraints.HORIZONTAL;
		cnts.weightx = 0.8f;cnts.weighty = 0f;
		cnts.gridx = 0;cnts.gridy = 0;
		mContent.add(mBaseInfo,cnts);
		
		mAliveConnCounter=new JTextField();  
		mAliveConnCounter.setEditable(false);
		mAliveConnCounter.setText("Alive connections: 0");
		mAliveConnCounter.setBackground(new Color(240,240,240));
		cnts = new GridBagConstraints();
		cnts.fill = GridBagConstraints.HORIZONTAL;
		cnts.weightx = 0.5f;cnts.weighty = 0f;
		cnts.gridx = 1;cnts.gridy = 0;
		cnts.gridwidth = 1;
		
		mContent.add(mAliveConnCounter,cnts);
		
		mMaxAlive=new JTextField();  
		mMaxAlive.setEditable(false);
		mMaxAlive.setText("Max alive: 0");
		mMaxAlive.setBackground(new Color(240,240,240));
		cnts = new GridBagConstraints();
		cnts.fill = GridBagConstraints.HORIZONTAL;
		cnts.weightx = 0.4f;cnts.weighty = 0f;
		cnts.gridx = 2;cnts.gridy = 0;
		cnts.gridwidth = 1;
		
		mContent.add(mMaxAlive,cnts);
		
		mMemInfo=new JTextField();  
		mMemInfo.setEditable(false);
		mMemInfo.setHorizontalAlignment(SwingConstants.RIGHT);
		mMemInfo.setText("Mem: 0 bytes");
		mMemInfo.setBackground(new Color(240,240,240));
		cnts = new GridBagConstraints();
		cnts.fill = GridBagConstraints.HORIZONTAL;
		cnts.weightx = 0.6f;cnts.weighty = 0f;
		cnts.gridx = 3;cnts.gridy = 0;
		mContent.add(mMemInfo,cnts);

		mConnModel = new ConnsTableModel();
		mConnTable = new ConnJTable(mConnModel);
		mConnTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
		mConnModel.initColumnWidth(mConnTable);
		mConnTable.setEnabled(false);
		mConnTableCnt = new JScrollPane(mConnTable);
		cnts = new GridBagConstraints();
		cnts.fill = GridBagConstraints.BOTH;
		cnts.weightx = 1f;cnts.weighty = 1f;
		cnts.gridwidth = 4;
		cnts.gridx = 0;cnts.gridy = 1;
		mContent.add(mConnTableCnt,cnts);
		
		cnts = new GridBagConstraints();
		cnts.gridx = cnts.gridy = 0;
		cnts.fill = GridBagConstraints.BOTH;
		cnts.weightx = cnts.weighty = 1.0f;
		mFrame.add(mContent,cnts);
	}
	
	public void setEventListener(ISwordUIEvent l) {
		this.mListener = l;
	}
	
	public void show() {
		mFrame.setVisible(true);
	}
	
	public void setServer(boolean isLocal) {
		mIsLcoal = isLocal;
		mFrame.setTitle(mIsLcoal?"NetSword-Local":"NetSword-Server");
	}
	
	public void setProxyHost(String host) {
		mProxyHost = host;
		
		updateBaseInfo();
	}
	
	public void setProxyPort(int port) {
		mProxyPort = port;
		updateBaseInfo();
	}
	
	public void setLocalPort(int port) {
		mLocalPort = port;
		updateBaseInfo();
	}
	
	private void updateBaseInfo() {
		
		StringBuilder info = new StringBuilder();
		if(mLocalPort >0 ) {
			info.append("listening " + mLocalPort);
		}
		
		if(mIsLcoal) {
			if(!StringUtils.isEmpty(mProxyHost)) {
				if(info.length() > 0) {
					info.append(",");
				}
				info.append(" server(" + mProxyHost + "/" + mProxyPort + ")");
			}
		}
		
		mBaseInfo.setText(info.toString());
	}
	
	public void addConn(int id) {
		if(mConnModel != null) {
			mConnModel.addConn(id);
		}
	}
	
	public void updateAliveConns(long aliveConnNum) {
		if(mAliveConnCounter != null) {
			mAliveConnCounter.setText("Living connections: " + String.valueOf(aliveConnNum));
		}
		
		if(aliveConnNum > mMaxAliveNum) {
			mMaxAliveNum = aliveConnNum;
			updateMaxAlive(mMaxAliveNum);
		}
	}
	
	public void updateConn(int id,int column,Object value) {
		Log.d(TAG, "updateConn " + id + " column " + column + " value " + value);
		if(mConnModel != null) {			
			mConnModel.updateField(id, column, value);
		}
		
	}
	
	public void updateMem(long inUsing,long total) {
		Log.d(TAG, "update memory, pool " + inUsing + " total " + total);
		if(mMemInfo != null) {
			synchronized (mMemInfo) {				
				mMemInfo.setText("Mem: " + inUsing + "/" + total);
			}
		}
	}
	
	private void updateMaxAlive(long max) {
		Log.d(TAG, "updateMaxAlive, max " + max);
		if(mMaxAlive != null) {
			synchronized (mMaxAlive) {				
				mMaxAlive.setText("Max concurrent: " + max);
			}
		}
	}
	
	public void updatePortOps(int id,boolean src,int ops) {
		if(mConnModel != null) {
			mConnModel.updatePortOps(id, src, ops);
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		Log.d(TAG, "keyTyped " + e);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if ((e.getKeyCode() == KeyEvent.VK_X) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
            Log.d(TAG, "user clear all conns!");
            if(mConnModel != null) {
            		mConnModel.clear();
            }
        }
	}

	@Override
	public void keyReleased(KeyEvent e) {
		Log.d(TAG, "keyReleased " + e);
	}
	
	public static interface ISwordUIEvent{
		public Object onSwordUIEvent(int eventId,Object... params);
		
		/**
		 * window is closing
		 * params: none
		 */
		public static final int WINDOW_CLOSE = 1;
	}
	
}
