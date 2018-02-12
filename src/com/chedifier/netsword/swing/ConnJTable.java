package com.chedifier.netsword.swing;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class ConnJTable extends JTable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ConnsTableModel mModel;
	
	public ConnJTable(ConnsTableModel model) {
		super(model);
		mModel = model;
	}
	
	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component comp = super.prepareRenderer(renderer, row, column);
		
		Color color = mModel.getCellBackgroundColor(row, column);
		if(color != null) {
			comp.setBackground(color);
		}else {
			comp.setBackground(Color.WHITE);
		}
		
		return comp;
	}
	
}
