package com.chedifier.netsword.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;

import com.chedifier.netsword.base.Log;
import com.chedifier.netsword.base.StringUtils;


public class SwordUI {
	private static final String TAG = "SwordUI";
	
	private static final int WIDNOW_WIDTH = 800;
	private static final int WINDOW_HEIGHT = 400;
	
	private boolean mIsLcoal;
	private String mTitle = "NetSword";
	private String mProxyHost = null;
	private int mProxyPort = 0;
	private int mLocalPort = 0;
	
	private Dimension mDimension;
	private JFrame mFrame;
	private JPanel mContent;
	private JTextArea mBaseInfo = null;
	private JScrollPane mConnTableCnt = null;
	private JTable mConnTable = null;
	
	private ConnsTableModel mConnModel;
	
	public static SwordUI build() {
		return new SwordUI();
	}
	
	private SwordUI() {
		mDimension = Toolkit.getDefaultToolkit().getScreenSize();
		mFrame = new JFrame(mTitle);
		mFrame.setLayout(new GridBagLayout());
		mFrame.setSize(WIDNOW_WIDTH,WINDOW_HEIGHT);  
		mFrame.setLocation(mDimension.width/2-mFrame.getSize().width/2, mDimension.height/2-mFrame.getSize().height/2);
		mFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);   
		mFrame.setResizable(false);
		
		mContent = new JPanel();
		mContent.setLayout(new GridBagLayout());
		
		mBaseInfo=new JTextArea();  
		mBaseInfo.setLineWrap(true);
		mBaseInfo.setEditable(false);
		mBaseInfo.setBackground(new Color(240,240,240));
		GridBagConstraints cnts = new GridBagConstraints();
		cnts.fill = GridBagConstraints.HORIZONTAL;
		cnts.weightx = 1f;cnts.weighty = 0f;
		cnts.gridx = 0;cnts.gridy = 0;
		mContent.add(mBaseInfo,cnts);

		mConnModel = new ConnsTableModel();
		mConnTable = new ConnJTable(mConnModel);
		mConnTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
		mConnModel.initColumnWidth(mConnTable);
		mConnTable.setEnabled(false);
		mConnTableCnt = new JScrollPane(mConnTable);
		cnts = new GridBagConstraints();
		cnts.fill = GridBagConstraints.BOTH;
		cnts.weightx = 1f;cnts.weighty = 1f;
		cnts.gridx = 0;cnts.gridy = 1;
		mContent.add(mConnTableCnt,cnts);
		
		cnts = new GridBagConstraints();
		cnts.gridx = cnts.gridy = 0;
		cnts.fill = GridBagConstraints.BOTH;
		cnts.weightx = cnts.weighty = 1.0f;
		mFrame.add(mContent,cnts);
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
			info.append("  port=" + mLocalPort);
		}
		
		if(mIsLcoal) {
			if(!StringUtils.isEmpty(mProxyHost)) {
				info.append("  proxy addr=" + mProxyHost);
			}
			
			if(mProxyPort > 0) {
				info.append("  server_port=" + mProxyPort);
			}
		}
		
		mBaseInfo.setText(info.toString());
	}
	
	public void addConn(int id) {
		if(mConnModel != null) {
			mConnModel.addConn(id);
		}
	}
	
	public void updateConn(int id,int column,Object value) {
		Log.d(TAG, "updateConn " + id + " column " + column + " value " + value);
		if(mConnModel != null) {			
			mConnModel.updateField(id, column, value);
		}
		
	}
	
	public void updatePortOps(int id,boolean src,int ops) {
		if(mConnModel != null) {
			mConnModel.updatePortOps(id, src, ops);
		}
	}
	
	
}
