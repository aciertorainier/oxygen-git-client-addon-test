package com.oxygenxml.git.view.branches;

import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JDialog;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.TestUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.event.GitController;
/**
 * Test cases for the actions that can be done on a branch.
 * 
 * @author gabriel_nedianu
 *
 */
public class BranchMergingTest extends GitTestBase {
  private final static String LOCAL_TEST_REPOSITORY = "target/test-resources/GitAccessMerging/localRepository";
  private final static String REMOTE_TEST_REPOSITORY = "target/test-resources/GitAccessMerging/remoteRepository";
  private final static String LOCAL_BRANCH_NAME1 = "LocalBranch";
  private GitAccess gitAccess;
  private Repository remoteRepository;
  private Repository localRepository;
  
  @Override
  @Before
  public void setUp() throws Exception {
    
    super.setUp();
    gitAccess = GitAccess.getInstance();
    
    //Creates the remote repository.
    createRepository(REMOTE_TEST_REPOSITORY);
    remoteRepository = gitAccess.getRepository();
    
    //Creates the local repository.
    createRepository(LOCAL_TEST_REPOSITORY);
    localRepository = gitAccess.getRepository();
    
    bindLocalToRemote(localRepository , remoteRepository);
  }
  
  /**
   * <p><b>Description:</b>Tests the branch merging.</p>
   * <p><b>Bug ID:</b> EXM-43410</p>
   * 
   * @author gabriel_nedianu
   * 
   * @throws Exception
   */
  @Test
  public void testBranchMerging() throws Exception{
    File file = new File(LOCAL_TEST_REPOSITORY, "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    //Make the first commit for the local repository and create a branch for it.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);
    gitAccess.fetch();
    
    String initialBranchName = gitAccess.getBranchInfo().getBranchName();
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, initialBranchName);
    
    GitControllerBase mock = new GitController(GitAccess.getInstance());
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
    branchManagementPanel.refreshBranches();
    flushAWT();
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
  
    //------------- Checkout the first branch in the tree: LOCAL_BRANCH_NAME1 -------------
    GitTreeNode firstLeaf = (GitTreeNode)root.getFirstLeaf();
    String firstLeafPath = (String)firstLeaf.getUserObject();
    assertTrue(firstLeafPath.contains(Constants.R_HEADS));
    
    String[] branchPath = firstLeafPath.split("/");
    assertEquals(LOCAL_BRANCH_NAME1, branchPath[branchPath.length - 1]);
    
    List<AbstractAction> actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(firstLeaf);
    for (AbstractAction abstractAction : actionsForNode) {
      if(abstractAction.getValue(AbstractAction.NAME).equals(translator.getTranslation(Tags.CHECKOUT))) {
        abstractAction.actionPerformed(null);
        break;
      }
    }
    sleep(500);
    gitAccess.fetch();
    assertEquals(LOCAL_BRANCH_NAME1, gitAccess.getRepository().getBranch());
    
    setFileContent(file, "local content for merging");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("Branch commit");
  
    gitAccess.setBranch(GitAccess.DEFAULT_BRANCH_NAME);
    sleep(500);
    
    List<AbstractAction> actionsForNode2 = branchTreeMenuActionsProvider.getActionsForNode(firstLeaf);
    for (AbstractAction abstractAction : actionsForNode2) {
      if(abstractAction.getValue(AbstractAction.NAME).toString().contains("Merge")) {
        abstractAction.actionPerformed(null);
        break;
      }
    }
    
    sleep(500);
    
    assertEquals("local content for merging", TestUtil.read(file.toURI().toURL()));
    
  }
  
  /**
   * <p><b>Description:</b>Tests the branch merging. Conflict happens.</p>
   * <p><b>Bug ID:</b> EXM-43410</p>
   * 
   * @author gabriel_nedianu
   * 
   * @throws Exception
   */
  @Test
  public void testBranchMergingWithConflict() throws Exception {

    File file1 = new File(LOCAL_TEST_REPOSITORY, "local1.txt");
    File file2 = new File(LOCAL_TEST_REPOSITORY, "local2.txt");
    file1.createNewFile();
    file2.createNewFile();

    setFileContent(file1, "local file 1 content");
    setFileContent(file2, "local file 2 content");

    // Make the first commit for the local repository and create a new branch
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local1.txt"));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local2.txt"));
    gitAccess.commit("First local commit on main.");
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);

    GitControllerBase mock = new GitController(GitAccess.getInstance());
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
    branchManagementPanel.refreshBranches();
    flushAWT();
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
    GitTreeNode root = (GitTreeNode) (branchManagementPanel.getTree().getModel().getRoot());
    GitTreeNode secondaryBranchNode = (GitTreeNode) root.getFirstLeaf();
    String secondaryBranchPath = (String) secondaryBranchNode.getUserObject();
    assertTrue(secondaryBranchPath.contains(Constants.R_HEADS));

    // ------------- Checkout the first branch in the tree: LOCAL_BRANCH_NAME1  -------------
    
    gitAccess.setBranch(LOCAL_BRANCH_NAME1);
    // Commit on this branch
    setFileContent(file1, "local file 1 on new branch");
    setFileContent(file2, "local file 2 on new branch");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local1.txt"));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local2.txt"));
    gitAccess.commit("Commit on secondary branch");

    // ------------- Move to the main branch and commit something there
    // -------------

    gitAccess.setBranch(GitAccess.DEFAULT_BRANCH_NAME);
    setFileContent(file1, "local file 1 modifications");
    setFileContent(file2, "local file 2 modifications");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local1.txt"));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local2.txt"));
    gitAccess.commit("2nd commit on main branch");

    // merge action
    List<AbstractAction> actionsForNode2 = branchTreeMenuActionsProvider.getActionsForNode(secondaryBranchNode);
    for (AbstractAction abstractAction : actionsForNode2) {
      if (abstractAction.getValue(AbstractAction.NAME).toString().contains("Merge")) {
        abstractAction.actionPerformed(null);
        break;
      }
    }
    sleep(500);

    JDialog conflictMergeDialog = findDialog(translator.getTranslation(Tags.MERGE_CONFLICTS_TITLE));
    assertNotNull(conflictMergeDialog);
    sleep(200);

    conflictMergeDialog.dispose();
    assertTrue(TestUtil.read(file1.toURI().toURL()).
        contains("<<<<<<< HEAD\n" 
            + "local file 1 modifications\n" 
            + "=======\n" 
            + "local file 1 on new branch\n" 
            + ">>>>>>>"));

    assertTrue(TestUtil.read(file2.toURI().toURL()).
        contains("<<<<<<< HEAD\n" 
            + "local file 2 modifications\n" 
            + "=======\n" 
            + "local file 2 on new branch\n" 
            + ">>>>>>>"));
  }
  
  /**
   *<p><b>Description:</b>Tests the failing merging.</p>
   * <p><b>Bug ID:</b> EXM-43410</p>
   * 
   * @author gabriel_nedianu
   * 
   * @throws Exception
   */
  @Test
  public void testFailingBranchMerging() throws Exception {
  
    File file1 = new File(LOCAL_TEST_REPOSITORY, "local1.txt");
    File file2 = new File(LOCAL_TEST_REPOSITORY, "local2.txt");
    file1.createNewFile();
    file2.createNewFile();

    setFileContent(file1, "local file 1 content");
    setFileContent(file2, "local file 2 content");

    // Make the first commit for the local repository, create a new branch and move on it
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local1.txt"));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local2.txt"));
    gitAccess.commit("First local commit on main.");
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);
    
    GitControllerBase mock = new GitController(GitAccess.getInstance());
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
    branchManagementPanel.refreshBranches();
    flushAWT();
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
    GitTreeNode root = (GitTreeNode) (branchManagementPanel.getTree().getModel().getRoot());
    GitTreeNode secondaryBranchNode = (GitTreeNode) root.getFirstLeaf();
    
    gitAccess.setBranch(LOCAL_BRANCH_NAME1);
    
    // Commit on this branch
    setFileContent(file1, "branch file1 modification ");
    setFileContent(file2, "branch file2 modification ");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local1.txt"));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local2.txt"));
    gitAccess.commit("Commit on secondary branch");
    
    // Move on main branch, make some uncommitted modifications and then try to merge
    
    gitAccess.setBranch(GitAccess.DEFAULT_BRANCH_NAME);
    setFileContent(file1, "file1 something xx...xx...");
    setFileContent(file2, "file2 something xx...xx...");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local1.txt"));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local2.txt"));
    // merge action
    
    List<AbstractAction> actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(secondaryBranchNode);
    for (AbstractAction abstractAction : actionsForNode) {
      if (abstractAction.getValue(AbstractAction.NAME).toString().contains("Merge")) {
        abstractAction.actionPerformed(null);
        break;
      }
    }
    
    JDialog checkoutMergeFailDialog = findDialog(translator.getTranslation(Tags.MERGE_FAILED_UNCOMMITTED_CHANGES_TITLE));
    assertNotNull(checkoutMergeFailDialog);
    sleep(200);
    
    checkoutMergeFailDialog.dispose();
    
    
    //Commit the changes on the main branch then make other uncommitted changes and try to merge again
    gitAccess.commit("Commit on main branch");
    
    setFileContent(file1, "file1 something modif");
    setFileContent(file2, "file2 something modif");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local1.txt"));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local2.txt"));
    
    List<AbstractAction> actionsForNode2 = branchTreeMenuActionsProvider.getActionsForNode(secondaryBranchNode);
    for (AbstractAction abstractAction : actionsForNode2) {
      if (abstractAction.getValue(AbstractAction.NAME).toString().contains("Merge")) {
        abstractAction.actionPerformed(null);
        break;
      }
    }
    
    JDialog mergeFailDialog = findDialog(translator.getTranslation(Tags.MERGE_FAILED_UNCOMMITTED_CHANGES_TITLE));
    assertNotNull(mergeFailDialog);
    sleep(200);
    
    mergeFailDialog.dispose();
  }
}
