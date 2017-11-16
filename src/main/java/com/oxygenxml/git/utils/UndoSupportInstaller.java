package com.oxygenxml.git.utils;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

import ro.sync.util.PlatformDetector;

/**
 * Installs the UNDO/REDO support on a text component. 
 * @author alex_jitianu
 */
public class UndoSupportInstaller {

  private static class MyCompoundEdit extends CompoundEdit {
  	boolean isUnDone = false;
  
  	public int getLength() {
  		return edits.size();
  	}
  
  	public void undo() throws CannotUndoException {
  		super.undo();
  		isUnDone = true;
  	}
  
  	public void redo() throws CannotUndoException {
  		super.redo();
  		isUnDone = false;
  	}
  
  	public boolean canUndo() {
  		return edits.size() > 0 && !isUnDone;
  	}
  
  	public boolean canRedo() {
  		return edits.size() > 0 && isUnDone;
  	}
  
  }

  private static class UndoManager extends AbstractUndoableEdit implements UndoableEditListener {
  	String lastEditName = null;
  	int lastOffset = -1;
  	ArrayList<MyCompoundEdit> edits = new ArrayList<MyCompoundEdit>();
  	MyCompoundEdit current;
  	int pointer = -1;
  
  	public void undoableEditHappened(UndoableEditEvent e) {
  		UndoableEdit edit = e.getEdit();
  		if (edit instanceof AbstractDocument.DefaultDocumentEvent) {
  			try {
  				AbstractDocument.DefaultDocumentEvent event = (AbstractDocument.DefaultDocumentEvent) edit;
  				int start = event.getOffset();
  				int len = event.getLength();
  				String text = "";
  				if ("addition".equals(edit.getPresentationName())) {
  					text = event.getDocument().getText(start, len);
  				}
  				boolean isNeedStart = false;
  				if (current == null) {
  					isNeedStart = true;
  				} else if (text.contains("\n") && !"deletion".equals(edit.getPresentationName())) {
  					isNeedStart = true;
  				} else if (lastEditName == null || !lastEditName.equals(edit.getPresentationName())) {
  					isNeedStart = true;
  				} else if (Math.abs(lastOffset - start) > 1) {
  					isNeedStart = true;
  				}
  
  				while (pointer < edits.size() - 1) {
  					edits.remove(edits.size() - 1);
  					isNeedStart = true;
  				}
  				if (isNeedStart) {
  					createCompoundEdit();
  				}
  
  				current.addEdit(edit);
  				lastEditName = edit.getPresentationName();
  				lastOffset = start;
  
  			} catch (BadLocationException e1) {
  				e1.printStackTrace();
  			}
  		}
  	}
  
  	public void createCompoundEdit() {
  		if (current == null) {
  			current = new MyCompoundEdit();
  		} else if (current.getLength() > 0) {
  			current = new MyCompoundEdit();
  		}
  
  		edits.add(current);
  		pointer++;
  	}
  
  	public void undo() throws CannotUndoException {
  		if (!canUndo()) {
  			throw new CannotUndoException();
  		}
  
  		MyCompoundEdit u = edits.get(pointer);
  		u.undo();
  		pointer--;
  
  	}
  
  	public void redo() throws CannotUndoException {
  		if (!canRedo()) {
  			throw new CannotUndoException();
  		}
  
  		pointer++;
  		MyCompoundEdit u = edits.get(pointer);
  		u.redo();
  
  	}
  
  	public boolean canUndo() {
  		return pointer >= 0;
  	}
  
  	public boolean canRedo() {
  		return edits.size() > 0 && pointer < edits.size() - 1;
  	}
  
  }

  public static void installUndoManager(JTextComponent commitMessage) {
    final UndoManager undoManager = new UndoManager();
  	Document doc = commitMessage.getDocument();
  	// Listen for undo and redo events
  	doc.addUndoableEditListener(undoManager);
  
  	// Create an undo action and add it to the text component
  	commitMessage.getActionMap().put("Undo", new AbstractAction("Undo") {
  		public void actionPerformed(ActionEvent evt) {
  			if (undoManager.canUndo()) {
  				undoManager.undo();
  			}
  		}
  	});
  
  	// Bind the undo action to ctl-Z
  	int modifier = PlatformDetector.isMacOS() ? KeyEvent.META_DOWN_MASK:KeyEvent.CTRL_DOWN_MASK;
  	
  	commitMessage.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, modifier), "Undo");
  
  	// Create a redo action and add it to the text component
  	commitMessage.getActionMap().put("Redo", new AbstractAction("Redo") {
  		public void actionPerformed(ActionEvent evt) {
  			if (undoManager.canRedo()) {
  				undoManager.redo();
  			}
  		}
  	});
  
  	// Bind the redo action to ctl-Y
  	commitMessage.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, modifier), "Redo");
  }

}