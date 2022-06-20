package com.oxygenxml.git.view.remotes;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.WindowConstants;
import javax.swing.border.Border;

import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.TextFormatUtil;
import com.oxygenxml.git.view.branches.BranchConfigurations;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;


/**
 * Dialog used to edit the current branch remote.
 * 
 * @author alex_smarandache
 *
 */
public class CurrentBranchRemotesDialog extends OKCancelDialog {
 	
	/**
	 * The translator.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();

	/**
	 * The default dialog width.
	 */
	private static final int DIALOG_WIDTH = 550;

	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(CurrentBranchRemotesDialog.class);

	/**
	 * Combo box with all remotes from current repository.
	 */
	private final JComboBox<RemoteBranchItem> remoteBranchItems = new JComboBox<>();

	/**
	 * The current branch.
	 */
	private final String currentBranch;
	
	/**
	 * The current dialog status.
	 */
	private int currentStatus = STATUS_OK;
	
	/**
	 * Constant for status ok.
	 */
	private static final int STATUS_OK = 0;
	
	/**
	 * Constant for status when the remote doesn't exists.
	 */
	public static final int STATUS_REMOTE_NOT_EXISTS = 1;
	
	/**
	 * Constant for status when branches are not founded.
	 */
	public static final int STATUS_BRANCHES_NOT_EXIST = 2;
	
	/**
	 * Maximum number of characters for a remote branch item.
	 */
	private static final int MAXIMUM_REMOTE_ITEM_NO_OF_CHARACTERS = 60;
	
	

	/**
	 * Constructor.
	 */
	public CurrentBranchRemotesDialog() {
		super((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
				TRANSLATOR.getTranslation(Tags.CONFIGURE_REMOTE_FOR_BRANCH), true
				);
		
		setOkButtonText(TRANSLATOR.getTranslation(Tags.TRACK_BRANCH));
		boolean existsRemotes = false;
		final List<RemoteBranchItem> branchesToAdd = new ArrayList<>();
		
		currentBranch = GitAccess.getInstance().getBranchInfo().getBranchName();

		try {
			
			final ListCellRenderer<? super RemoteBranchItem> oldRender = remoteBranchItems.getRenderer();
			remoteBranchItems.setRenderer((list, value, index, isSelected, cellHasFocus) -> {

				final JLabel toReturn = (JLabel) 
				    oldRender.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				
				final Border padding = BorderFactory.createEmptyBorder(
						0, 
						UIConstants.COMPONENT_LEFT_PADDING, 
						0, 
						UIConstants.COMPONENT_RIGHT_PADDING
						);

				toReturn.setBorder(padding);
				toReturn.setText(TextFormatUtil.shortenText(toReturn.getText(), MAXIMUM_REMOTE_ITEM_NO_OF_CHARACTERS, 
				    0, "..."));

				return toReturn;
			});

			existsRemotes = addRemoteBranches(branchesToAdd);

		} catch (NoRepositorySelected | URISyntaxException e) {
			LOGGER.error(e.getMessage(), e);
		}

		if(!existsRemotes) {
			currentStatus = STATUS_REMOTE_NOT_EXISTS;
			this.doCancel();
		} else if(branchesToAdd.isEmpty()) {
			currentStatus = STATUS_BRANCHES_NOT_EXIST;
			this.doCancel();
		} else {
			branchesToAdd.sort((b1, b2) -> {
				int comparasionResult = !b1.isUndefined() && !b2.isUndefined() ? 
						Boolean.compare(b2.branch.endsWith(currentBranch), b1.branch.endsWith(currentBranch)) : 0;
				if(comparasionResult == 0) {
					comparasionResult = b1.toString().compareTo(b2.toString());
				}
				return comparasionResult;
			});
			
			branchesToAdd.forEach(branch -> {
				remoteBranchItems.addItem(branch);
				if(branch.isFirstSelection()) {
					remoteBranchItems.setSelectedIndex(remoteBranchItems.getItemCount() - 1);
				}
			});
			getContentPane().add(createGUIPanel());

			pack();
			repaint();

			JFrame parentFrame = PluginWorkspaceProvider.getPluginWorkspace() != null ? 
					(JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null;
					if (parentFrame != null) {
						setIconImage(parentFrame.getIconImage());
						setLocationRelativeTo(parentFrame);
					}

					setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);


					this.setVisible(true);
					this.setResizable(false);
		}

	}

    
	/**
	 * Add remote branches for current repository. 
	 * 
	 * @param branchesToAdd List to add the branches.
     *
	 * @return <code>true</code> if at least a branch is founded.
	 * 
	 * @throws URISyntaxException
	 * @throws NoRepositorySelected 
	 */
	private boolean addRemoteBranches(final List<RemoteBranchItem> branchesToAdd) throws URISyntaxException, NoRepositorySelected {
		
		final StoredConfig config = GitAccess.getInstance().getRepository().getConfig();
		final BranchConfigurations branchConfig = new BranchConfigurations(config, currentBranch);
		final List<String> remotesNames = new ArrayList<>(GitAccess.getInstance()
				.getRemotesFromConfig().keySet());
		boolean foundedBranchRemoteForCurrentLocalBranch = false;
		boolean existsRemotes = false;
		
		for(String remote : remotesNames) {
			existsRemotes = true;
			final URIish sourceURL = new URIish(config.getString(ConfigConstants.CONFIG_REMOTE_SECTION,
					remote, ConfigConstants.CONFIG_KEY_URL));
			final Collection<Ref> branchesConfig = GitAccess.getInstance().doListRemoteBranchesInternal(
					sourceURL, null);
		
			for(Ref branch: branchesConfig) {
				final String branchName = branch.getName();
				final String remoteC = branchConfig.getRemote();
				final String mergeC = branchConfig.getMerge();
				if(remoteC !=null && remoteC.equals(remote) 
						&& mergeC != null && mergeC.equals(branchName)) {
					final RemoteBranchItem remoteItem = new RemoteBranchItem(remote, branchName);
					foundedBranchRemoteForCurrentLocalBranch = true;
					remoteItem.setFirstSelection(true);
					branchesToAdd.add(remoteItem);
				} else {
					branchesToAdd.add(new RemoteBranchItem(remote, branchName));
				}
			}
		}

		if(!foundedBranchRemoteForCurrentLocalBranch) {
			final RemoteBranchItem remoteItem = new RemoteBranchItem(null, null);
			remoteItem.setFirstSelection(true);
			remoteBranchItems.addItem(remoteItem);	
			remoteBranchItems.setSelectedIndex(remoteBranchItems.getItemCount() - 1);
		}
		
		return existsRemotes;
	}


	@Override
	public Dimension getPreferredSize() {
	  final Dimension prefferedSize = super.getPreferredSize();
		return new Dimension(Math.max(DIALOG_WIDTH, prefferedSize.width), prefferedSize.height);
	}
	
	
	/**
	 * Create the dialog GUI.
	 * 
	 * @return The created panel.
	 */
	private JPanel createGUIPanel() {
		JPanel guiPanel = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();

		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(0, 0, UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_LARGE_PADDING);
		constraints.weightx = 0;
		constraints.weighty = 0;
		constraints.fill = GridBagConstraints.NONE;

		JLabel remoteNameLabel = new JLabel(TRANSLATOR.getTranslation(Tags.LOCAL_BRANCH));
		guiPanel.add(remoteNameLabel, constraints);

		constraints.gridx++;
		guiPanel.add(new JLabel(currentBranch), constraints);

		constraints.gridx = 0;
		constraints.gridy++;
		guiPanel.add(new JLabel(TRANSLATOR.getTranslation(Tags.REMOTE_TRACKING_BRANCH) + ":"), constraints);

		constraints.weightx = 1;
		constraints.gridx++;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = new Insets(0, 0, UIConstants.COMPONENT_BOTTOM_PADDING, 0);
		guiPanel.add(remoteBranchItems, constraints);

		return guiPanel;
	}


	@Override
	protected void doOK() {
		RemoteBranchItem currentSelectedBranch = (RemoteBranchItem) remoteBranchItems.getSelectedItem();
		if(!currentSelectedBranch.isUndefined() && !currentSelectedBranch.isFirstSelection()) {
			try {
				BranchConfigurations branchConfig = new BranchConfigurations(
						GitAccess.getInstance().getRepository().getConfig(), currentBranch);
				branchConfig.setRemote(currentSelectedBranch.remote);
				branchConfig.setMerge(currentSelectedBranch.branch);
				GitAccess.getInstance().updateConfigFile();
			} catch (NoRepositorySelected e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		
		super.doOK();
	}
	
	
	/**
	 * @return The dialog status.
	 */
	public int getStatusResult() {
		return currentStatus;
	}
	

	/**
	 * !!! Used for tests !!! 
	 *
	 * @return The remote branch items.
	 */
	public JComboBox<RemoteBranchItem> getRemoteBranchItems() {
    return remoteBranchItems;
  }
	

  /**
	 * Used to help us to store the remote branch informations.
	 * 
	 * @author alex_smarandache
	 *
	 */
	 public static class RemoteBranchItem {
		
		/**
		 * Constant when no remote or repo are selected.
		 */
		private static final String NONE = "<none>";
		
		/**
		 * The remote from config.
		 */
		final String remote;
		
		/**
		 * A branch from current remote.
		 */
		final String branch;
		
		/**
		 * The branch short name.
		 */
		private final String branchShortName;
		
    /**
     * <code>true</code> if this item represents the first selection.
     */
		private boolean isFirstSelection = false;
		
		
		/**
		 * Constructor.
		 * 
		 * @param remote
		 * @param branch
		 */
		RemoteBranchItem(final String remote, final String branch) {
			this.remote = remote;
			this.branch = branch;
			this.branchShortName = branch != null ? Repository.shortenRefName(branch) : null;
		}
		
		
		/**
		 * @return <code>true</code> if this item represents the first selection.
		 */
		public boolean isFirstSelection() {
			return isFirstSelection;
		}

        /**
         * @param isFirstSelection <code>true</code> if this item represents the first selection.
         */
		public void setFirstSelection(boolean isFirstSelection) {
			this.isFirstSelection = isFirstSelection;
		}
		
		/**
		 * @return <code>true</code> if the remote or branch are undefined.
		 */
		public boolean isUndefined() {
			return remote == null || branch == null;
		}

		@Override
		public String toString() {
			return isUndefined() ? NONE : remote + "/" + branchShortName;
		}
	}
	
}
