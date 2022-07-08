package com.oxygenxml.git.service;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.awaitility.Awaitility;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.utils.PlatformDetectionUtil;
import com.oxygenxml.git.view.history.CommitCharacteristics;
import com.oxygenxml.git.view.history.HistoryStrategy;
import com.oxygenxml.git.view.history.actions.RevertCommitAction;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.images.ImageUtilities;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.util.ColorTheme;
import ro.sync.exml.workspace.api.util.UtilAccess;

/**
 * Tests the revert action
 *
 * @author Tudosie Razvan
 */
public class RevertCommitTest extends GitTestBase {
  private static final String DATE_REGEX = "(([0-9])|([0-2][0-9])|([3][0-1]))\\ "
      + "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\ \\d{4}";
  private final static String LOCAL_TEST_REPOSITORY = "target/test-resources/GitAccessRevertCommitTest";
  private final static String LOCAL_FILE_NAME = "local.txt";
  private final static String LOCAL_FILE_NAME_2 = "local_2.txt";
  private File firstFile;
  private File secondFile;
  private GitAccess gitAccess;
  private String[] errMsg = new String[1];

  /**
   * Creates the local repository and commits a few files.
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();
    
    StandalonePluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    Mockito.doAnswer(invocation -> {
      Object[] arguments = invocation.getArguments();
      errMsg[0] = arguments != null && arguments.length > 0 ? (String) arguments[0] : "";
      return null;
    }).when(pluginWSMock).showErrorMessage(Mockito.anyString(), Mockito.any());
    errMsg[0] = "";
    
    ColorTheme colorTheme = Mockito.mock(ColorTheme.class);
    Mockito.when(colorTheme.isDarkTheme()).thenReturn(false);
    Mockito.when(pluginWSMock.getColorTheme()).thenReturn(colorTheme);
    
    UtilAccess utilAccessMock = Mockito.mock(UtilAccess.class);
    Mockito.when(pluginWSMock.getUtilAccess()).thenReturn(utilAccessMock);
    Mockito.when(utilAccessMock.locateFile((URL) Mockito.any())).then(new Answer<File>() {
      @Override
      public File answer(InvocationOnMock invocation) throws Throwable {
        URL url = (URL) invocation.getArguments()[0];
        
        String path = url.getPath();
        if (PlatformDetectionUtil.isWin() && path.startsWith("/")) {
          path = path.substring(1, path.length());
        }
        
        return new File(url.getPath());
      }
    });
    
   //    PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess().getFileName()
    Mockito.when(utilAccessMock.getFileName(Mockito.anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        String file = (String) invocation.getArguments()[0];
        file = file.replace('\\', '/');
        int index = file.lastIndexOf("/");
        return index != -1 ? file.substring(index + 1) : file;
      }
    });
    
    Mockito.when(utilAccessMock.uncorrectURL(Mockito.anyString())).then(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return invocation.getArguments()[0].toString().replace("%20", " ");
      }
    });
    
    ImageUtilities imageUtilities = Mockito.mock(ImageUtilities.class);
    Mockito.doReturn(null).when(imageUtilities).loadIcon((URL)Mockito.any());
    Mockito.doReturn(imageUtilities).when(pluginWSMock).getImageUtilities();
    
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);

    // Create the local repository.
    createRepository(LOCAL_TEST_REPOSITORY);
    gitAccess = GitAccess.getInstance();

    // Create first file make the first commit for the local repository.
    firstFile = new File(LOCAL_TEST_REPOSITORY, LOCAL_FILE_NAME);
    firstFile.createNewFile();
    setFileContent(firstFile, "new local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, LOCAL_FILE_NAME));
    gitAccess.commit("First commit.");

    // Modify first file and make another commit.
    firstFile.createNewFile();
    setFileContent(firstFile, "modified content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, LOCAL_FILE_NAME));
    gitAccess.commit("Modified a file");

    gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITORY);
    
    // Create a second file and make a third commit
    secondFile = new File(LOCAL_TEST_REPOSITORY, LOCAL_FILE_NAME_2);
    secondFile.createNewFile();
    setFileContent(secondFile, "second local file");
    gitAccess.add(new FileStatus(GitChangeType.ADD, LOCAL_FILE_NAME_2));
    gitAccess.commit("Added a new file");
  }

  /**
   * <p><b>Description:</b> cannot revert first version.</p>
   * <p><b>Bug ID:</b> EXM-47154</p>
   *
   * @author Tudosie Razvan
   * 
   * @throws Exception
   */
  public void testRevertCommit_cannotRevertFirstVersion() throws Exception {
    // The history at this moment
    List<CommitCharacteristics> commitsCharacteristics = gitAccess.getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, null);
    String expected = "[ Added a new file , DATE , AlexJitianu <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n"
        + "[ Modified a file , DATE , AlexJitianu <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n"
        + "[ First commit. , DATE , AlexJitianu <alex_jitianu@sync.ro> , 3 , AlexJitianu , null ]\n" + "";
    assertEquals(expected, dumpHistory(commitsCharacteristics).replaceAll(DATE_REGEX, "DATE"));

    CommitCharacteristics commitToRevert = commitsCharacteristics.get(2);
    RevertCommitAction revertAction = new RevertCommitAction(commitToRevert);
    SwingUtilities.invokeLater(() -> revertAction.actionPerformed(null));
    flushAWT();

    JDialog revertConfirmationDlg = findDialog(Tags.REVERT_COMMIT);
    JTextArea confirmationTextArea = findFirstTextArea(revertConfirmationDlg);
    assertEquals(Tags.REVERT_COMMIT_CONFIRMATION, confirmationTextArea.getText().toString());
    JButton revertOkButton = findFirstButton(revertConfirmationDlg, Tags.YES);
    assertEquals("", errMsg[0]);
    SwingUtilities.invokeLater(() -> revertOkButton.doClick());
    flushAWT();
    Awaitility.await().atMost(500, TimeUnit.MILLISECONDS).until(() -> 
      errMsg[0].contains("Cannot revert"));
    
    // The history stays the same
    commitsCharacteristics = gitAccess.getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, null);
    expected = "[ Added a new file , DATE , AlexJitianu <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n"
        + "[ Modified a file , DATE , AlexJitianu <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n"
        + "[ First commit. , DATE , AlexJitianu <alex_jitianu@sync.ro> , 3 , AlexJitianu , null ]\n" + "";
    assertEquals(expected, dumpHistory(commitsCharacteristics).replaceAll(DATE_REGEX, "DATE"));

  }
  
  /**
   * <p><b>Description:</b> revert commit.</p>
   * <p><b>Bug ID:</b> EXM-47154</p>
   *
   * @author sorin_carbunaru
   * 
   * @throws Exception
   */
  public void testRevertCommit() throws Exception {
    // The history at this moment
    List<CommitCharacteristics> commitsCharacteristics = gitAccess.getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, null);
    String expected = "[ Added a new file , DATE , AlexJitianu <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n"
        + "[ Modified a file , DATE , AlexJitianu <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n"
        + "[ First commit. , DATE , AlexJitianu <alex_jitianu@sync.ro> , 3 , AlexJitianu , null ]\n" + "";
    assertEquals(expected, dumpHistory(commitsCharacteristics).replaceAll(DATE_REGEX, "DATE"));

    CommitCharacteristics commitToRevert = commitsCharacteristics.get(1);
    RevertCommitAction revertAction = new RevertCommitAction(commitToRevert);
    SwingUtilities.invokeLater(() -> revertAction.actionPerformed(null));
    flushAWT();
    JDialog revertConfirmationDlg = findDialog(Tags.REVERT_COMMIT);
    JTextArea confirmationTextArea = findFirstTextArea(revertConfirmationDlg);
    assertEquals(Tags.REVERT_COMMIT_CONFIRMATION, confirmationTextArea.getText().toString());
    JButton revertOkButton = findFirstButton(revertConfirmationDlg, Tags.YES);
    SwingUtilities.invokeLater(() -> revertOkButton.doClick());
    flushAWT();
    sleep(300);
    
    // New commit added (for the reverted changes)
    commitsCharacteristics = gitAccess.getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, null);
    assertEquals(4, commitsCharacteristics.size());
    assertTrue(commitsCharacteristics.get(0).getCommitMessage().startsWith("Revert \"Modified a file\""));

  }
  
  /**
   * <p><b>Description:</b> show warning when conflict is generated.</p>
   * <p><b>Bug ID:</b> EXM-48678</p>
   *
   * @author sorin_carbunaru
   * 
   * @throws Exception
   */
  public void testRevertCommit_warningWhenConflictGenerated() throws Exception {
    setFileContent(firstFile, "<modified content 2>");
    gitAccess.add(new FileStatus(GitChangeType.ADD, LOCAL_FILE_NAME));
    gitAccess.commit("Modified a file again");
    
    // The history at this moment
    List<CommitCharacteristics> commitsCharacteristics = gitAccess.getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, null);
    String expected = "[ Modified a file again , DATE , AlexJitianu <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
        "[ Added a new file , DATE , AlexJitianu <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n" + 
        "[ Modified a file , DATE , AlexJitianu <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]\n" + 
        "[ First commit. , DATE , AlexJitianu <alex_jitianu@sync.ro> , 4 , AlexJitianu , null ]\n" + 
        "";
    assertEquals(expected, dumpHistory(commitsCharacteristics).replaceAll(DATE_REGEX, "DATE"));

    //Revert commit that will trigger conflict
    CommitCharacteristics commitToRevert = commitsCharacteristics.get(2);
    RevertCommitAction revertAction = new RevertCommitAction(commitToRevert);
    SwingUtilities.invokeLater(() -> revertAction.actionPerformed(null));
    flushAWT();

    JDialog revertConfirmationDlg = findDialog(Tags.REVERT_COMMIT);
    JTextArea confirmationTextArea = findFirstTextArea(revertConfirmationDlg);
    assertEquals(Tags.REVERT_COMMIT_CONFIRMATION, confirmationTextArea.getText().toString());
    JButton revertOkButton = findFirstButton(revertConfirmationDlg, Tags.YES);
    SwingUtilities.invokeLater(() -> revertOkButton.doClick());
    flushAWT();
    
    JDialog dlg = findDialog(Tags.REVERT_COMMIT);
    assertNotNull(dlg);
    
    JTextArea textArea = findFirstTextArea(dlg);
    assertEquals(Tags.REVERT_COMMIT_RESULTED_IN_CONFLICTS, textArea.getText());
    dlg.dispose();
  }
  
}

