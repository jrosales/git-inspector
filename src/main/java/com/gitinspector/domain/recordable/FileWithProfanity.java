package com.gitinspector.domain.recordable;

import java.util.ArrayList;
import java.util.List;

public class FileWithProfanity extends Violation {

    private String fileName;

    private List<String> profanityList;

    public FileWithProfanity(String orgName, String repoFullName, String repoOwner, String fileName,
                             List<String> profanityList) {
        super(orgName, repoFullName, repoOwner);
        this.fileName = fileName;
        this.profanityList = new ArrayList<>(profanityList);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<String> getProfanityList() {
        return profanityList;
    }

    public void setProfanityList(List<String> prfainWordList) {
        this.profanityList = prfainWordList;
    }

    @Override
    public String toString() {
        return super.toString() +
                " fileName='" + fileName +
                " profanityList=" + profanityList;
    }
}
