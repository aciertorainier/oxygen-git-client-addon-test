package com.oxygenxml.git.service.exceptions;

public class RepoNotInitializedException extends Exception {
  public RepoNotInitializedException() {
    super("The remote repository has not been initialized.");
  }
}
