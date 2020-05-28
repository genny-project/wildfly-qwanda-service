package life.genny.qwanda.util;

import life.genny.bootxport.bootx.RealmUnit;
import life.genny.qwandautils.GennySettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ProjectSheetUtil {
    private static final Logger log = LoggerFactory.getLogger(ProjectSheetUtil.class);

    // Get github user, password, repo url array, branch etc
    static class InvalidGithubUrlException extends Exception {
        public InvalidGithubUrlException(String message) {
            super(message);
        }
    }

    public static Map<String, List<String>> getBranchToProjectUrlsMapping(String repoUrls) throws InvalidGithubUrlException {
        if (repoUrls == null || repoUrls.equals("")) {
            String info = repoUrls == null ? "null" : "empty string";
            log.error(String.format("GitHub repoUrls from sheet is %s, use gitProjectUrls in GennySettings!", info));
            repoUrls = GennySettings.gitProjectUrls;
        }

        HashMap<String, List<String>> branchToProjectUrlsMapping = new HashMap<>();
        String[] repoUrlArray = repoUrls.replace("\n", "").split(";");
        for (String item : repoUrlArray) {
            // url:branch
            int index = item.lastIndexOf(':');
            if (index != -1) {
                String repoUrl = item.substring(0, index);
                String repoBranch = item.substring(index + 1);
                if (branchToProjectUrlsMapping.containsKey(repoBranch)) {
                    branchToProjectUrlsMapping.get(repoBranch).add(repoUrl);
                } else {
                    List<String> urlList = new LinkedList<>();
                    urlList.add(repoUrl);
                    branchToProjectUrlsMapping.put(repoBranch, urlList);
                }
            } else {
                throw new InvalidGithubUrlException("Check repo urls format:" + repoUrls + ", should be \"url1:banch;url2:branch2:url3:branch3\"");
            }
        }
        return branchToProjectUrlsMapping;
    }

    public static String getGitUserName(RealmUnit realmUnit) {
        String gitUserName = realmUnit.getGithubUserName();
        if (gitUserName == null || gitUserName.equals("")) {
            String info = gitUserName == null ? "null" : "empty string";
            log.error(String.format("GitHub user name from sheet is %s, use gitUsername in GennySettings!", info));
            gitUserName = GennySettings.gitUsername;
        }
        return gitUserName;
    }

    public static String getGitPassword(RealmUnit realmUnit) {
        String gitPassword = realmUnit.getGithubPassword();
        if (gitPassword == null || gitPassword.equals("")) {
            String info = gitPassword == null ? "null" : "empty string";
            log.error(String.format("GitHub user password from sheet is %s, use gitPassword in GennySettings!", info));
            gitPassword = GennySettings.gitPassword;
        }
        return gitPassword;
    }

    public static void main(String[] args) {
        // ["url1:branch1","url2:branch2"]
        // e.g "v3.1.0":[url1, url2]
        String repoUrls = "https://github.com/genny-project/prj_genny.git:v3.1.0-AC-rules;https://github.com/OutcomeLife/prj_internmatch.git:v3.1.0;\n" +
                "https://github.com/OutcomeLife/prj_stt.git:v3.1.0";
        repoUrls = null;
        repoUrls = "";
        try {
            getBranchToProjectUrlsMapping(repoUrls);
            Map<String, List<String>> branchToProjectUrlsMapping = ProjectSheetUtil.getBranchToProjectUrlsMapping(repoUrls);
            branchToProjectUrlsMapping.keySet().forEach(gitBranch -> {
                List<String> projectUrlList = branchToProjectUrlsMapping.get(gitBranch);
                log.info(String.format("URL number:%d, branch name:%s", projectUrlList.size(), gitBranch));
                for (String gitProjectUrl : projectUrlList) {
                    log.info(String.format("Git repo url:%s, branch name:%s", gitProjectUrl, gitBranch));
                }
            });
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
