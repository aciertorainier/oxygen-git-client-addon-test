package com.oxygenxml.git.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * A dialog that is shown when the pull is successful but has conflicts. It
 * shows an infromation message and the files that are in conflict
 * 
 * @author Beniamin Savu
 *
 */
public class PullWithConflictsDialog extends OKCancelDialog {

	public PullWithConflictsDialog(JFrame frame, String title, boolean modal, Set<String> conflictFiles,
			Translator translator) {
		super(frame, title, modal);

		JLabel label = new JLabel(translator.getTraslation(Tags.PULL_SUCCESSFUL_CONFLICTS));
		Border border = label.getBorder();
		Border margin = new EmptyBorder(0, 7, 7, 0);
		label.setBorder(new CompoundBorder(border, margin));

		DefaultListModel<String> model = new DefaultListModel<String>();
		for (String file : conflictFiles) {
			model.addElement(file);
		}
		JList<String> filesInConflictList = new JList<String>(model);
		JScrollPane scollPane = new JScrollPane(filesInConflictList);
		scollPane.setPreferredSize(new Dimension(300, 100));

		getContentPane().add(label, BorderLayout.NORTH);
		getContentPane().add(scollPane, BorderLayout.SOUTH);
		getCancelButton().setVisible(false);
		this.pack();
		this.setLocationRelativeTo(frame);
		this.setResizable(false);
		this.setVisible(true);
		this.setDefaultCloseOperation(OKCancelDialog.DISPOSE_ON_CLOSE);
	}

}
