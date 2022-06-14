package com.oxygenxml.git.view.history;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.RoundedLineBorder;
import com.oxygenxml.git.view.components.ApplicationLabel;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Renderer for HistoryTable including tag and branch labels.
 * 
 * @Alexandra_Dinisor
 *
 */
public class CommitMessageTableRenderer extends JPanel implements TableCellRenderer {
  /**
   * The size of the border shown for a tag or branch label.
   */
  private static final int LABEL_BORDER_CORNER_SIZE = 6;

  /**
   * Font size for the arrow characters that show the incoming and outgoing changes.
   */
  private static final int ARROWS_FONT_SIZE = 12;

  /**
   * Default horizontal insets between components.
   */
  private static final int HORIZONTAL_INSET = 3;

  /**
   * The maximum length for a branch or tag name.
   */
  private static final int MAX_BRANCH_OR_TAG_NAME_LENGTH = 18;

  /**
   * Logger for logging.
   */
  @SuppressWarnings("unused")
  private static final Logger LOGGER =  LoggerFactory.getLogger(CommitMessageTableRenderer.class);

  /**
   * Git repository.
   */
  private final Repository repository;

  /**
   * Commits ahead (to push) and behind (to pull).
   */
  private final CommitsAheadAndBehind commitsAheadAndBehind;
  /**
   * The current branch name in the git repository.
   */
  private final String currentBranchName;
  /**
   * Commit ID to a list of tags.
   */
  private final Map<String, List<String>> tagMap;
  /**
   * Commit ID to a list of branch labels.
   */
  private final Map<String, List<String>> localBranchMap;
  /**
   * Commit ID to a list of branch labels.
   */
  private final Map<String, List<String>> remoteBranchMap;

  /**
   * Table for this render.
   */
  private JTable table;

  /**
   * The current row.
   */
  private int row;

  /**
   * The current column.
   */
  private int column;


  /**
   * Construct the Table Renderer with accurate alignment.
   * 
   * @param repository            The current repository
   * @param commitsAheadAndBehind Commits ahead (to push) and behind (to pull).
   * @param branchName            Branch name.
   * @param tagMap                Tags map.
   * @param localBranchMap        Local branches map.
   * @param remoteBranchMap       Remote branches map.
   */
  public CommitMessageTableRenderer(
      Repository repository, 
      CommitsAheadAndBehind commitsAheadAndBehind,
      String branchName,
      Map<String, List<String>> tagMap,
      Map<String, List<String>> localBranchMap,
      Map<String, List<String>> remoteBranchMap) {
    this.repository = repository;
    this.commitsAheadAndBehind = commitsAheadAndBehind;
    this.currentBranchName = branchName;
    this.tagMap = tagMap;
    this.localBranchMap = localBranchMap;
    this.remoteBranchMap = remoteBranchMap;

    setLayout(new GridBagLayout());
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
      int row, int column) {
    this.table = table;
    this.row = row;
    this.column = column;

    removeAll();

    // keep the selection for whole columns of the row when selecting.
    if (isSelected) {
      super.setForeground(table.getSelectionForeground());
      super.setBackground(table.getSelectionBackground());
    } else {
      Color background = table.getBackground();
      if (background == null || background instanceof javax.swing.plaf.UIResource) {
        Color alternateColor = UIManager.getColor("Table.alternateRowColor");
        if (alternateColor != null && row % 2 != 0) {
          background = alternateColor;
        }
      }
      super.setForeground(table.getForeground());
      super.setBackground(background);
    }

    setFont(table.getFont());
    setBorder(getNoFocusBorder());

    // adding constraints for commit message label when wrapping
    GridBagConstraints constr = new GridBagConstraints();
    constr.fill = GridBagConstraints.NONE;
    constr.anchor = GridBagConstraints.WEST;
    constr.gridy = 0;
    constr.gridx = -1;
    constr.insets = new Insets(0, HORIZONTAL_INSET, 0, HORIZONTAL_INSET);

    String toRender = "";
    if (value instanceof CommitCharacteristics) {
      toRender = getRenderingStringForCommit(value, constr, table);
    } else {
      toRender = value != null ? value.toString() : "";
    }

    constr.gridx ++;
    constr.fill = GridBagConstraints.HORIZONTAL;
    constr.weightx = 1;
    final JLabel comp = new JLabel(toRender);
    comp.setForeground(getForeground());
    if(!toRender.isEmpty()) {
      comp.setToolTipText(toRender);
    }
    add(comp, constr);

    return this;
  }

  /**
   * Get the text to render for a commit.
   * 
   * @param value  The commit characteristics.
   * @param constr Grid bag constraints.
   * @param table  The table.
   * 
   * @return the text to render.
   */
  private String getRenderingStringForCommit(Object value, GridBagConstraints constr, JTable table) {
    CommitCharacteristics commitCharacteristics = (CommitCharacteristics) value;
    String toRender = commitCharacteristics.getCommitMessage().replaceAll("\\n+", " ").trim();

    // Show outgoing and incoming commits using arrows
    String arrow = "";
    if (isAheadCommit(commitCharacteristics.getCommitId())) {
      // Up arrow
      arrow = "\u2191";
    } else if (isBehindCommit(commitCharacteristics.getCommitId())) {
      // Down arrow
      arrow = "\u2193";
    }
    if (!arrow.isEmpty()) {
      JLabel arrowLabel = new JLabel(arrow);
      arrowLabel.setFont(new Font("Dialog", Font.PLAIN, ARROWS_FONT_SIZE));
      arrowLabel.setForeground(getForeground());
      constr.gridx ++;
      add(arrowLabel, constr);
    }

    // bold the text for uncommitted changes
    final String uncommittedChangesMessage = Translator.getInstance().getTranslation(Tags.UNCOMMITTED_CHANGES);
    if (toRender.equals(uncommittedChangesMessage)) {
      toRender = "<html><body><b>" + uncommittedChangesMessage + "</b></body></html>";
    } else if (repository != null) {
      String abbreviatedId = commitCharacteristics.getCommitAbbreviatedId();
      boolean isDarkTheme = PluginWorkspaceProvider.getPluginWorkspace().getColorTheme().isDarkTheme();

      List<String> tagList = tagMap.get(abbreviatedId);
      Color tagBackgroundColor = isDarkTheme ? UIUtil.TAG_GRAPHITE_BACKGROUND 
          : UIUtil.TAG_LIGHT_BACKGROUND;
      addTagOrBranchLabel(tagList, constr, tagBackgroundColor, table.getForeground());

      List<String> localBranchList = localBranchMap.get(abbreviatedId);
      addTagOrBranchLabel(localBranchList, constr, table.getBackground(), table.getForeground());

      List<String> remoteBranchList = remoteBranchMap.get(abbreviatedId);
      Color remoteBackgroundColor = isDarkTheme ? UIUtil.REMOTE_BRANCH_GRAPHITE_BACKGROUND 
          : UIUtil.REMOTE_BRANCH_LIGHT_BACKGROUND;

      addTagOrBranchLabel(remoteBranchList, constr, remoteBackgroundColor, table.getForeground());
    }

    return toRender;
  }

  /**
   * Add Label to "Commit Message" column: tag or local/remote branch
   * 
   * @param nameForLabelList List of tags or branches corresponding the commit.
   * @param constr           The constraints for tag / branch label when wrapping
   * @param backgroundColor  The background color.
   * @param foregroundColor  The foreground color.
   */
  private void addTagOrBranchLabel(
      List<String> nameForLabelList,
      GridBagConstraints constr,
      Color backgroundColor,
      Color foregroundColor) {
    if (nameForLabelList != null && !nameForLabelList.isEmpty()) {
      Insets oldInsets = constr.insets;
      // No insets. We will impose space from the borders.
      constr.insets = new Insets(0, 0, 0, 0);
      int lineSize = 1;

      for (String name : nameForLabelList) {
        final RoundedLineBorder border = new RoundedLineBorder(null, lineSize, LABEL_BORDER_CORNER_SIZE, true);
        final String shortenBranchName = name.length() > MAX_BRANCH_OR_TAG_NAME_LENGTH ? 
            (name.substring(0, MAX_BRANCH_OR_TAG_NAME_LENGTH - "...".length()) + "...") 
            : name;
        final JLabel label = new ApplicationLabel(shortenBranchName) {

          @Override
          protected void paintComponent(Graphics g) {
            border.fillBorder(this, g, 0, 0, getWidth(), getHeight());
            super.paintComponent(g);
          }
        };
        if (name.equals(currentBranchName)) {
          label.setFont(label.getFont().deriveFont(Font.BOLD));
        }
        label.setBackground(backgroundColor);
        label.setForeground(foregroundColor);
        label.setBorder(border);
        label.setToolTipText(name);
        constr.gridx ++;
        add(label, constr);
      }

      // We added a label. Update the top insets of the initial insets.
      constr.insets = oldInsets;
    }
  }

  /**
   * Check if this is a commit to push.
   * 
   * @param commitID Commit ID.
   *  
   * @return true if this is a commit to push.
   */
  private boolean isAheadCommit(String commitID) {
    boolean isIt = false;
    if (commitsAheadAndBehind != null) {
      List<RevCommit> commitsAhead = commitsAheadAndBehind.getCommitsAhead();
      isIt = commitsAhead.stream().anyMatch(commit -> commit.getId().getName().equals(commitID));
    }
    return isIt;
  }

  /**
   * Check if this is a commit to pull.
   * 
   * @param commitID Commit ID.
   *  
   * @return true if this is a commit to pull.
   */
  private boolean isBehindCommit(String commitID) {
    boolean isIt = false;
    if (commitsAheadAndBehind != null) {
      List<RevCommit> commitsBehind = commitsAheadAndBehind.getCommitsBehind();
      isIt = commitsBehind.stream().anyMatch(commit -> commit.getId().getName().equals(commitID));
    }
    return isIt;
  }

  /**
   * @see javax.swing.table.DefaultTableCellRenderer.getNoFocusBorder()
   * @return The Border with no focus
   */
  private Border getNoFocusBorder() {
    Border toReturn = noFocusBorder;

    Border border = UIManager.getBorder("Table.cellNoFocusBorder");
    if (System.getSecurityManager() != null) {
      toReturn = border != null ? border : SAFE_NO_FOCUS_BORDER;
    } else if (border != null) {
      if (noFocusBorder == null || noFocusBorder == DEFAULT_NO_FOCUS_BORDER) { // NOSONAR squid:S1066
        toReturn = border;
      }
    }

    return toReturn;
  }

  @Override
  public String getToolTipText(MouseEvent event) {

    String tooltipText = null;

    Rectangle rectangle = table.getCellRect(row, column, true);
    setBounds(rectangle);
    doLayout();

    final Component component = getComponentAt(event.getPoint());

    if (component instanceof JLabel) {
      final JLabel label = (JLabel)component;
      tooltipText = label.getToolTipText();
    }

    return tooltipText;

  }

  /**
   * An empty <code>Border</code>. This field might not be used. To change the
   * <code>Border</code> used by this renderer override the
   * <code>getTableCellRendererComponent</code> method and set the border of the
   * returned component directly.
   */
  private static final Border SAFE_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);
  private static final Border DEFAULT_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);
  protected static Border noFocusBorder = DEFAULT_NO_FOCUS_BORDER;

}
