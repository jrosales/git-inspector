package com.gitinspector.domain.recordable;

import java.util.ArrayList;
import java.util.List;

public class CommitWithProfanity extends BadCommit {

    private List<String> profanityList;
    public CommitWithProfanity(String orgName, String repoFullName, String repoOwner, String committer,
                               String commitSha, List<String> profanityList) {
        super(orgName, repoFullName, repoOwner, committer, commitSha);
        this.profanityList = new ArrayList<>(profanityList);
    }

    public List<String> getProfanityList() {
        return profanityList;
    }

    public void setProfanityList(List<String> profanityList) {
        this.profanityList = profanityList;
    }

    public void addProfainWordToList(String profainWord) {
        profanityList.add(profainWord);
    }

    @Override
    public String toString() {
        return super.toString() +
                " profanityList=" + profanityList;
    }
}
