package com.gitinspector.domain;

/**
 * A user of git. Keeps track of their username and email address.
 */
public class GitUser {
    private String username;
    private String emailaddress;

    public GitUser(String username, String emailaddress) {
        this.username = username;
        this.emailaddress = emailaddress;
    }

    public String getUsername() {
        return username;
    }

    public String getEmailaddress() {
        return emailaddress;
    }

    @Override
    public String toString() {
        return "GitUser{" +
               "username='" + username + '\'' +
               ", emailaddress='" + emailaddress + '\'' +
               '}';
    }
}
