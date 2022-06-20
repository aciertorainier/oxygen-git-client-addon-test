package com.oxygenxml.git.view.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.auth.AuthenticationInterceptor;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.TextField;

/**
 * Dialog that appears when a repository has no remote and the user tries to
 * push or pull and asks the user for a remote repository
 * 
 * @author Beniamin Savu
 *
 */
@SuppressWarnings("java:S110")
public class AddRemoteDialog extends OKCancelDialog {
	
	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(AddRemoteDialog.class);

	/**
	 * The translator for the messages that are displayed in this dialog
	 */
	private static Translator translator = Translator.getInstance();

	/**
	 * Remote repository text field.
	 */
	private JTextField remoteRepoTextField;

	/**
	 * Constructor.
	 */
	public AddRemoteDialog() {
		super((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
		    translator.getTranslation(Tags.SET_REMOTE_BRANCH), true);
		setOkButtonText(translator.getTranslation(Tags.SET_REMOTE));
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		addInformationLabel(gbc);
		addRemoteRepoLabel(gbc);
		addRemoteRepoTextField(gbc);

		setMinimumSize(new Dimension(500, 130));
		setResizable(true);
		pack();
		setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
	}
	
	/**
	 * Presents the dialog for the user to link a new remote.
	 * 
	 * @return <code>true</code> if the user gave a new remote.
	 */
	public boolean linkRemote() {
	  setVisible(true);
	  
	  return getResult() == RESULT_OK;
	}

	/**
	 * Some information message displayed above the text filed
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 */
	private void addInformationLabel(GridBagConstraints gbc) {
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 2;
		JLabel label = new JLabel(translator.getTranslation(Tags.ADD_REMOTE_DIALOG_INFO_LABEL));
		getContentPane().add(label, gbc);
	}

	/**
	 * Adds the text field to the dialog
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 */
	private void addRemoteRepoTextField(GridBagConstraints gbc) {
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		remoteRepoTextField = new TextField();
		remoteRepoTextField.setPreferredSize(new Dimension(350, remoteRepoTextField.getPreferredSize().height));
		getContentPane().add(remoteRepoTextField, gbc);
	}

	/**
	 * Adds a label on the left side of the text filed
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 */
	private void addRemoteRepoLabel(GridBagConstraints gbc) {
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		JLabel label = new JLabel(translator.getTranslation(Tags.REMOTE_REPO_URL));
		getContentPane().add(label, gbc);
	}

	/**
	 * Adds the remote as origin to the repository
	 */
	@Override
	protected void doOK() {
		super.doOK();
		StoredConfig config;
		try {
			config = GitAccess.getInstance().getRepository().getConfig();
			RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
			URIish uri = new URIish(remoteRepoTextField.getText());
			remoteConfig.addURI(uri);
			RefSpec spec = new RefSpec("+refs/heads/*:refs/remotes/origin/*");
			remoteConfig.addFetchRefSpec(spec);
			remoteConfig.update(config);
			config.save();
			
			String host = uri.getHost();
			if (!AuthenticationInterceptor.isBound(host)) {
        AuthenticationInterceptor.bind(host);
      }
		} catch (NoRepositorySelected | URISyntaxException | IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
		dispose();
	}
}
