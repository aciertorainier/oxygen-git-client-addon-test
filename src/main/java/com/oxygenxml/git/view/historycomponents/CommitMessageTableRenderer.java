package com.oxygenxml.git.view.historycomponents;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.service.GitAccess;

import sun.swing.DefaultLookup;

/**
 * Renderer for HistoryTable including tag and branch labels.
 * 
 * @Alexandra_Dinisor
 *
 */
public class CommitMessageTableRenderer extends JPanel implements TableCellRenderer {
	/**
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(CommitMessageTableRenderer.class);

	/**
	 * Git repository.
	 */
	private Repository repository;

	/**
	 * Commits ahead (to push) and behind (to pull).
	 */
  private CommitsAheadAndBehind commitsAheadAndBehind;

	/**
	 * Construct the Table Renderer with accurate alignment.
	 * 
	 * @param repository            The current repository
	 * @param commitsAheadAndBehind Commits ahead (to push) and behind (to pull).
	 */
	public CommitMessageTableRenderer(Repository repository, CommitsAheadAndBehind commitsAheadAndBehind) {
		this.repository = repository;
    this.commitsAheadAndBehind = commitsAheadAndBehind;

		setLayout(new GridBagLayout());
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		removeAll();

		// keep the selection for whole columns of the row when selecting.
		if (isSelected) {
			super.setForeground(table.getSelectionForeground());
			super.setBackground(table.getSelectionBackground());
		} else {
			Color background = table.getBackground();
			if (background == null || background instanceof javax.swing.plaf.UIResource) {
				Color alternateColor = DefaultLookup.getColor(this, ui, "Table.alternateRowColor");
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
		constr.insets = new Insets(0, 3, 0, 3);

		String toRender = "";
		if (value instanceof CommitCharacteristics) {
			CommitCharacteristics commitCharacteristics = (CommitCharacteristics) value;
			toRender = commitCharacteristics.getCommitMessage().replaceAll("\\n+", " ").trim();
			
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
			  JLabel component = new JLabel(arrow);
			  component.setForeground(getForeground());
			  constr.gridx ++;
			  add(component, constr);
			}
			
			// bold the text for uncommitted changes
			if (toRender.equals(GitAccess.UNCOMMITTED_CHANGES)) {
				toRender = "<html><body><b>" + GitAccess.UNCOMMITTED_CHANGES + "</b></body></html>";
			} else if (repository != null) {
				// add labels in historyTable for tags and branch names from corresponding maps
				try {
				  GitAccess gitAccess = GitAccess.getInstance();
				  
					String abbreviatedId = commitCharacteristics.getCommitAbbreviatedId();
					Map<String, List<String>> tagMap = gitAccess.getTagMap(repository);
					List<String> tagList = tagMap.get(abbreviatedId);
					addTagOrBranchLabel(tagList, constr);

					Map<String, List<String>> localBranchMap = gitAccess.getBranchMap(repository, ConfigConstants.CONFIG_KEY_LOCAL);
					List<String> localBranchList = localBranchMap.get(abbreviatedId);
					addTagOrBranchLabel(localBranchList, constr);

					Map<String, List<String>> remoteBranchMap = gitAccess.getBranchMap(repository, ConfigConstants.CONFIG_KEY_REMOTE);
					List<String> remoteBranchList = remoteBranchMap.get(abbreviatedId);
					addTagOrBranchLabel(remoteBranchList, constr);

				} catch (IOException | GitAPIException e) {
					logger.debug(e, e);
				}
			}
		} else {
			toRender = value != null ? value.toString() : "";
		}

		constr.gridx ++;
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.weightx = 1;
		JLabel comp = new JLabel(toRender);
		comp.setForeground(getForeground());
    add(comp, constr);
		return this;
	}

	/**
	 * Add Label to "Commit Message" column: tag or local/remote branch
	 * 
	 * @param nameForLabelList List of tags or branches corresponding the commit.
	 * @param constr           The constraints for tag / branch label when wrapping
	 */
	private void addTagOrBranchLabel(List<String> nameForLabelList, GridBagConstraints constr) {
	  Color foregroundColor = getForeground();
		if (nameForLabelList != null) {
			for (String name : nameForLabelList) {
				JLabel label = new JLabel(name);
				label.setForeground(foregroundColor);
				label.setBorder(BorderFactory.createLineBorder(foregroundColor));
				constr.gridx ++;
				add(label, constr);
			}
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
	    for (RevCommit revCommit : commitsAhead) {
	      if (revCommit.getId().getName().equals(commitID)) {
	        isIt = true;
	        break;
	      }
	    }
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
	    for (RevCommit revCommit : commitsBehind) {
	      if (revCommit.getId().getName().equals(commitID)) {
	        isIt = true;
	        break;
	      }
	    }
	  }
	  return isIt;
 }

	/**
	 * @see javax.swing.table.DefaultTableCellRenderer.getNoFocusBorder()
	 * @return The Border with no focus
	 */
	private Border getNoFocusBorder() {
		Border border = DefaultLookup.getBorder(this, ui, "Table.cellNoFocusBorder");
		if (System.getSecurityManager() != null) {
			if (border != null)
				return border;
			return SAFE_NO_FOCUS_BORDER;
		} else if (border != null) {
			if (noFocusBorder == null || noFocusBorder == DEFAULT_NO_FOCUS_BORDER) { // NOSONAR squid:S1066
				return border;
			}
		}
		return noFocusBorder;
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
