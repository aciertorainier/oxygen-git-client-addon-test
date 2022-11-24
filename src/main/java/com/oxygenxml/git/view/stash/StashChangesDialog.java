package com.oxygenxml.git.view.stash;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.TextFormatUtil;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.TextField;

/**
 * Dialog shown when stashing changes.
 * 
 * @author Alex Smarandache
 */
public class StashChangesDialog extends OKCancelDialog {

  /**
   * Translator.
   */
  private static final Translator TRANSLATOR = Translator.getInstance();
  
  /**
   * A text field for the stash message/description.
   */
  private final JTextField stashDescriptionField = new TextField();
  
  /**
   * The date format.
   */
  private final SimpleDateFormat dateFormat = new SimpleDateFormat(UIUtil.DATE_FORMAT_WITH_COMMA_PATTERN);
  
  /**
   * When selected, the stash will include the untracked files.
   */
  private final JCheckBox includeUntrackedCheckBox = new JCheckBox(TRANSLATOR.getTranslation(Tags.INCLUDE_UNTRACKED));

  /**
   * The dialog width.
   */
  private static final int DIALOG_WIDTH = 200;


  /**
   * Public constructor.
   */
  public StashChangesDialog() {
    super(PluginWorkspaceProvider.getPluginWorkspace() != null
        ? (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame()
        : null,
        TRANSLATOR.getTranslation(Tags.STASH_CHANGES),
        true);
    
    setOkButtonText(TRANSLATOR.getTranslation(Tags.STASH));

    // Create GUI
    JPanel panel = new JPanel(new GridBagLayout());
    createGUI(panel);
    add(panel);
    setResizable(true);
    pack();
    
    stashDescriptionField.requestFocus();

    if (PluginWorkspaceProvider.getPluginWorkspace() != null) {
      setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
    }
    setMinimumSize(getPreferredSize());
    Dimension maximumSize = getMaximumSize();
    maximumSize.height = getPreferredSize().height;
    setMaximumSize(maximumSize);
  }

  
  /**
   * Adds the elements to the user interface/
   * 
   * @param panel         The panel in which the components are added.
   */
  private void createGUI(JPanel panel) {
    
    // Informative label message.
    JLabel informativeLabel = new JLabel(TextFormatUtil.toHTML(TRANSLATOR.getTranslation(Tags.STASH_INFORMATIVE_MESSAGE)));
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.weightx = 0;
    constraints.weighty = 0;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(0, 0, UIConstants.COMPONENT_BOTTOM_PADDING, 0);
    panel.add(informativeLabel, constraints);
    
    JLabel addDescriptionMessage = new JLabel(TextFormatUtil.toHTML(TRANSLATOR.getTranslation(Tags.STASH_ADD_DESCRIPTION)
        + ":"));
    constraints.gridy++;
    panel.add(addDescriptionMessage, constraints);

    // Stash description field.
    String description = "WIP on " 
        + GitAccess.getInstance().getBranchInfo().getBranchName() 
        + " [" 
        + dateFormat.format(new Date())
        + "]";
    stashDescriptionField.setText(description);
    stashDescriptionField.selectAll();
    stashDescriptionField.setPreferredSize(new Dimension(DIALOG_WIDTH, stashDescriptionField.getPreferredSize().height));
    constraints.gridy ++;
    constraints.weightx = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, 0, 0, 0);
    panel.add(stashDescriptionField, constraints);

    addDescriptionMessage.setLabelFor(stashDescriptionField);

    constraints.gridy++;
    constraints.weightx = 0;
    constraints.fill = GridBagConstraints.NONE;
    
    includeUntrackedCheckBox.setSelected(OptionsManager.getInstance().getStashIncludeUntracked());
    includeUntrackedCheckBox.addItemListener(
        e -> OptionsManager.getInstance().setStashIncludeUntracked(includeUntrackedCheckBox.isSelected()));

    panel.add(includeUntrackedCheckBox, constraints);
    
    constraints.insets = new Insets(0, 0, 0, 0);
    constraints.fill = GridBagConstraints.BOTH;
    constraints.weighty = 1;
    constraints.gridy ++;
    panel.add(new JPanel(), constraints);

  }

  
  /**
   * Gets the stash message to be set for the new stashed changes.
   * 
   * @return The name for the tag.
   */
  public String getStashMessage() {
    return stashDescriptionField.getText();
  }


  /**
   * @return <code>true</code> if the stash should include the untracked files.
   */
  public boolean shouldIncludeUntracked() {
    return includeUntrackedCheckBox.isSelected();
  }

}


